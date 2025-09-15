package com.easyshop.auth.service;

import com.easyshop.auth.entity.User;
import com.easyshop.auth.repository.UserRepository;

import com.easyshop.auth.web.dto.AuthDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder enc;

    // Password validation patterns
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    public AuthService(UserRepository users, PasswordEncoder enc) {
        this.users = users;
        this.enc = enc;
    }

    public boolean register(AuthDto d) {
        if (users.existsByEmail(d.email())) {
            return false;
        }
        
        // Validate password strength
        if (!isValidPassword(d.password())) {
            return false;
        }
        
        User user = new User();
        user.setEmail(d.email().toLowerCase().trim());
        user.setPassword(enc.encode(d.password()));
        user.setRole(User.Role.USER);
        user.setUsername(d.email().toLowerCase().trim()); // Use email as username
        users.save(user);
        return true;
    }

    public boolean login(AuthDto d) {
        var u = users.findByEmail(d.email().toLowerCase().trim()).orElse(null);
        return u != null && enc.matches(d.password(), u.getPassword());
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public String getPasswordValidationMessage() {
        return "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character (@$!%*?&)";
    }
}
