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
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_USED);
        }

        String encodedPwd = passwordEncoder.encode(dto.getPassword());
        User user = userRepository.save(User.from(dto, encodedPwd, false));

        otpService.generateOtp(dto.getEmail());
        log.info("Registration started for {}. Awaiting email verification.", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORDS_DO_NOT_MATCH);
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        log.info("Password reset completed for {}", request.getEmail());
    }
}
