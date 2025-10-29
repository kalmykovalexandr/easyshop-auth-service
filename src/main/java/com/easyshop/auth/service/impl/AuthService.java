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
    public long register(AuthDto dto) {
        String email = dto.getEmail();

        User user = userRepository.findByEmail(email).orElse(null);
        String encodedPwd = passwordEncoder.encode(dto.getPassword());

        // Continue registration for not yet verified user
        if (user != null) {
            // If user exists and already verified -> conflict
            if (Boolean.TRUE.equals(user.getEnabled())) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_USED);
            }

            user.setPassword(encodedPwd);
            userRepository.save(user);
            long cooldown = otpService.generateOtp(email, true);
            log.info("Registration resumed for {}.", email);
            return cooldown;
        }

        // First-time registration
        userRepository.save(User.from(dto, encodedPwd, false));
        long cooldown = otpService.generateOtp(email, true);
        log.info("Registration started for {}.", email);
        return cooldown;
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetDto request) {
        // TODO move this check into advice layer
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
