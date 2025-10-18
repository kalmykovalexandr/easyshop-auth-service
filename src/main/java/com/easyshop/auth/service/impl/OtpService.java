package com.easyshop.auth.service.impl;

import com.easyshop.auth.context.UserContext;
import com.easyshop.auth.exception.BusinessException;
import com.easyshop.auth.exception.ErrorCode;
import com.easyshop.auth.exception.RateLimitExceededException;
import com.easyshop.auth.model.dto.VerifyCodeDto;
import com.easyshop.auth.model.entity.User;
import com.easyshop.auth.repository.UserRepository;
import com.easyshop.auth.service.EmailServiceInt;
import com.easyshop.auth.service.OtpServiceInt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OtpService implements OtpServiceInt {

    private final EmailServiceInt emailService;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redis;
    private final UserContext userContext;

    private final Duration ttl = Duration.ofMinutes(10);
    private final Duration resendCooldown = Duration.ofSeconds(60);
    private final Duration ipWindow = Duration.ofMinutes(10);
    private final int maxAttempts = 5;
    private final int maxIpRequests = 10;

    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(EmailServiceInt emailService,
                      UserRepository userRepository,
                      RedisTemplate<String, String> redis,
                      UserContext userContext) {
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.redis = redis;
        this.userContext = userContext;
    }

    @Override
    public void generateOtp(String email) {
        enforceCooldown(email);
        enforceIpRateLimit(userContext.getClientIp());

        String code = generateCode();
        String otpKey = otpKey(email);

        redis.opsForValue().set(otpKey, code, ttl);
        redis.opsForValue().set(cooldownKey(email), "1", resendCooldown);
        redis.delete(attemptsKey(email));

        try {
            emailService.sendVerificationEmail(email, code);
            log.info("OTP generated for {} (cooldown={}s)", email, resendCooldown.getSeconds());
        } catch (RuntimeException ex) {
            redis.delete(otpKey);
            redis.delete(cooldownKey(email));
            log.warn("Failed to send OTP e-mail. Cleaned up Redis entries for {}", email);
            throw ex;
        }
    }

    @Override
    public void verifyOtp(VerifyCodeDto dto) {
        String email = dto.getEmail();
        String code = dto.getCode();
        Boolean activateUser = dto.getActivateUser();

        String otpKey = otpKey(email);
        String expected = redis.opsForValue().get(otpKey);

        if (expected == null) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_NOT_FOUND);
        }

        Long tries = redis.opsForValue().increment(attemptsKey(email));
        if (tries != null && tries == 1L) {
            redis.expire(attemptsKey(email), ttl);
        }

        if (tries != null && tries > maxAttempts) {
            redis.delete(otpKey);
            redis.delete(attemptsKey(email));
            throw new BusinessException(ErrorCode.TOO_MANY_VERIFICATION_ATTEMPTS);
        }

        if (!Objects.equals(expected, code)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_INVALID);
        }

        redis.delete(otpKey);
        redis.delete(attemptsKey(email));

        if (Boolean.TRUE.equals(activateUser)) {
            enableUser(email);
        }
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

        log.info("User {} e-mail verified and account enabled.", email);
    }

    private void enforceCooldown(String email) {
        String cooldownKey = cooldownKey(email);
        Boolean hasCooldown = redis.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(hasCooldown)) {
            Long remaining = redis.getExpire(cooldownKey, TimeUnit.SECONDS);
            int retryAfter = remaining != null && remaining > 0
                    ? remaining.intValue()
                    : (int) resendCooldown.getSeconds();
            throw new RateLimitExceededException(retryAfter);
        }
    }

    private void enforceIpRateLimit(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        String key = ipRateKey(ipAddress.trim());
        Long requests = redis.opsForValue().increment(key);
        if (requests != null && requests == 1L) {
            redis.expire(key, ipWindow);
        }
        if (requests != null && requests > maxIpRequests) {
            Long remaining = redis.getExpire(key, TimeUnit.SECONDS);
            int retryAfter = remaining != null && remaining > 0
                    ? remaining.intValue()
                    : (int) ipWindow.getSeconds();
            throw new RateLimitExceededException(retryAfter);
        }
    }

    private String generateCode() {
        int value = secureRandom.nextInt(90_000_000) + 10_000_000;
        return Integer.toString(value);
    }

    private String otpKey(String email) {
        return "otp:%s".formatted(email);
    }

    private String attemptsKey(String email) {
        return "otp:attempts:%s".formatted(email);
    }

    private String cooldownKey(String email) {
        return "otp:cooldown:%s".formatted(email);
    }

    private String ipRateKey(String ip) {
        return "otp:ip:%s".formatted(ip);
    }
}
