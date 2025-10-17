package com.easyshop.auth.service.impl;

import com.easyshop.auth.model.entity.User;
import com.easyshop.auth.repository.UserRepository;
import com.easyshop.auth.service.OtpServiceInt;
import com.easyshop.auth.model.dto.AuthDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final OtpServiceInt otpServiceInt;

    public AuthService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       EmailService authEmailService,
                       OtpServiceInt otpServiceInt) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.otpServiceInt = otpServiceInt;
    }

    @Transactional
    public void register(AuthDto dto) {
        String encodedPwd = passwordEncoder.encode(dto.getPassword());
        users.save(User.from(dto, encodedPwd, false));
        otpServiceInt.generateOtp(dto.getEmail());
    }

    @Transactional
    public void updatePassword(String email, String newPassword, String confirmPassword) {
        Optional<User> userOpt = users.findByEmail(email);
        if (userOpt.isPresent() && newPassword.equals(confirmPassword)) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            users.save(user);
            log.info("Password reset completed for user");
        }
    }
}
