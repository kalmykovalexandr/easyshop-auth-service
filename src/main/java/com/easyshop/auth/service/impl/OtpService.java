package com.easyshop.auth.service.impl;

import com.easyshop.auth.exception.BusinessException;
import com.easyshop.auth.exception.ErrorCode;
import com.easyshop.auth.exception.RateLimitExceededException;
import com.easyshop.auth.model.OtpState;
import com.easyshop.auth.model.dto.VerifyCodeDto;
import com.easyshop.auth.model.dto.VerifyCodeResponseDto;
import com.easyshop.auth.model.entity.User;
import com.easyshop.auth.repository.OtpStateRepository;
import com.easyshop.auth.repository.UserRepository;
import com.easyshop.auth.service.EmailServiceInt;
import com.easyshop.auth.service.OtpServiceInt;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.easyshop.auth.exception.ErrorCode.PASSWORD_RESET_CODE_NOT_FOUND;

@Slf4j
@Service
public class OtpService implements OtpServiceInt {

    private final EmailServiceInt emailService;
    private final UserRepository userRepository;
    private final OtpStateRepository otpStateRepository;

    private final Duration otpTtl;
    private final Duration resendCooldown;
    private final Duration resetTokenTtl;
    private final int maxAttempts;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(EmailServiceInt emailService,
                      UserRepository userRepository,
                      OtpStateRepository otpStateRepository,
                      @Value("${easyshop.auth.verification-ttl-minutes}") long otpTtlMinutes,
                      @Value("${easyshop.auth.verification-resend-cooldown-seconds}") long resendCooldownSeconds,
                      @Value("${easyshop.auth.reset-token-ttl-minutes}") long resetTokenTtlMinutes,
                      @Value("${easyshop.auth.verification-max-attempts}") int maxAttempts) {
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.otpStateRepository = otpStateRepository;
        this.otpTtl = Duration.ofMinutes(Math.max(otpTtlMinutes, 1));
        this.resendCooldown = Duration.ofSeconds(Math.max(resendCooldownSeconds, 1));
        this.resetTokenTtl = Duration.ofMinutes(Math.max(resetTokenTtlMinutes, 1));
        this.maxAttempts = Math.max(maxAttempts, 1);
    }

    @Override
    public void generateOtp(String email) {
        // If user does not exist, do not send anything (avoid enumeration/spam)
        if (userRepository.findByEmail(email).isEmpty()) {
            return;
        }

        OtpState otp = otpStateRepository.load(email).orElseGet(OtpState::empty);

        Instant now = Instant.now();
        if (otp.hasOtp() && !otp.isOtpExpired(now)) {
            log.info("OTP already active for {} — no-op", email);
            return;
        }

        String code = generateCode();
        OtpState updated = otp.startOtp(code, now, otpTtl, resendCooldown);
        otpStateRepository.save(email, updated, now);

        try {
            emailService.sendVerificationEmail(email, code);
            log.info("OTP generated for {}", email);
        } catch (RuntimeException ex) {
            otpStateRepository.delete(email);
            log.warn("Failed to send OTP to email {}", email, ex);
            throw ex;
        }
    }

    @Override
    public VerifyCodeResponseDto verifyOtp(VerifyCodeDto dto) {
        Instant now = Instant.now();
        String email = dto.getEmail();
        OtpState state = otpStateRepository.findOrThrow(email, ErrorCode.VERIFICATION_CODE_NOT_FOUND);

        if (!state.hasOtp()) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_NOT_FOUND);
        }
        if (state.isOtpExpired(now)) {
            otpStateRepository.delete(email);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        OtpState attemptsState = state.incrementAttempts();
        if (attemptsState.getAttempts() > maxAttempts) {
            otpStateRepository.delete(email);
            throw new BusinessException(ErrorCode.TOO_MANY_VERIFICATION_ATTEMPTS);
        }

        if (!Objects.equals(attemptsState.getCode(), dto.getCode())) {
            otpStateRepository.save(email, attemptsState, now);
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }

        OtpState cleared = attemptsState.clearOtp();

        // registration flow
        if (Boolean.TRUE.equals(dto.getActivateUser())) {
            otpStateRepository.delete(email);
            enableUser(email);
            return null;
        }

        // reset password flow
        String resetToken = generateResetToken();
        OtpState withToken = cleared.issueResetToken(resetToken, now.plus(resetTokenTtl));
        otpStateRepository.save(email, withToken, now);
        log.info("OTP verified for {}. Reset token issued.", email);
        return new VerifyCodeResponseDto(resetToken);
    }

    @Override
    public void validateResetToken(String email, String resetToken) {
        OtpState state = otpStateRepository.findOrThrow(email, PASSWORD_RESET_CODE_NOT_FOUND);

        if (!state.hasResetToken()) {
            otpStateRepository.delete(email);
            throw new BusinessException(PASSWORD_RESET_CODE_NOT_FOUND);
        }
        if (state.isResetTokenExpired(Instant.now())) {
            otpStateRepository.delete(email);
            throw new BusinessException(ErrorCode.PASSWORD_RESET_CODE_EXPIRED);
        }
        if (!Objects.equals(state.getResetToken(), resetToken)) {
            otpStateRepository.delete(email);
            throw new BusinessException(ErrorCode.PASSWORD_RESET_CODE_INVALID);
        }

        otpStateRepository.delete(email);
    }

    private void enableUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ErrorCode.ALREADY_VERIFIED);
        }

        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setAccountNonExpired(true);
        user.setCredentialsNonExpired(true);
        userRepository.save(user);

        log.info("User {} email verified and account enabled.", email);
    }

    private void enforceCooldown(OtpState state, Instant now) {
        if (!state.inCooldown(now)) {
            return;
        }
        long retryAfter = state.getCooldownUntil().getEpochSecond() - now.getEpochSecond();
        throw new RateLimitExceededException((int) Math.max(1, retryAfter));
    }

    private String generateResetToken() {
        return UUID.randomUUID().toString();
    }

    private String generateCode() {
        int value = secureRandom.nextInt(90_000_000) + 10_000_000;
        return Integer.toString(value);
    }
}
