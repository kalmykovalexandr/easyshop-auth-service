package com.easyshop.auth.service.impl;

import com.easyshop.auth.exception.BusinessException;
import com.easyshop.auth.exception.ErrorCode;
import com.easyshop.auth.model.dto.AuthDto;
import com.easyshop.auth.model.dto.PasswordResetDto;
import com.easyshop.auth.model.entity.User;
import com.easyshop.auth.repository.UserRepository;
import com.easyshop.auth.service.AuthServiceInt;
import com.easyshop.auth.service.OtpServiceInt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService implements AuthServiceInt {

    private final OtpServiceInt otpService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(OtpServiceInt otpService,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void register(AuthDto dto) {
        String email = dto.getEmail();

        // Check existing user by email
        User existing = userRepository.findByEmail(email).orElse(null);
        String encodedPwd = passwordEncoder.encode(dto.getPassword());

        if (existing != null) {
            // If already verified, keep previous behavior (conflict)
            if (Boolean.TRUE.equals(existing.getEnabled())) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_USED);
            }

            // Continue registration for not yet verified user:
            // - update password to the latest provided
            // - resend OTP (previous code becomes obsolete)
            existing.setPassword(encodedPwd);
            userRepository.save(existing);
            otpService.generateOtp(email);
            log.info("Registration resumed for {}. OTP re-sent.", email);
            return;
        }

        // First-time registration path
        User user = userRepository.save(User.from(dto, encodedPwd, false));
        otpService.generateOtp(email);
        log.info("Registration started for {}. Awaiting email verification.", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetDto request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORDS_DO_NOT_MATCH);
        }

        otpService.validateResetToken(request.getEmail(), request.getResetToken());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        log.info("Password reset completed for {}", request.getEmail());
    }
}
