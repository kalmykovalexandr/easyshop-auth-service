package com.easyshop.auth.service;

import com.easyshop.auth.context.UserContext;
import com.easyshop.auth.entity.User;
import com.easyshop.auth.model.EmailVerificationStatus;
import com.easyshop.auth.model.RegistrationResult;
import com.easyshop.auth.repository.UserRepository;
import com.easyshop.auth.web.dto.AuthDto;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;
    private final UserContext userContext;
    private final EmailVerificationService emailVerificationService;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    public AuthService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       MessageSource messageSource,
                       UserContext userContext,
                       EmailVerificationService emailVerificationService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.messageSource = messageSource;
        this.userContext = userContext;
        this.emailVerificationService = emailVerificationService;
    }

    public RegistrationResult register(AuthDto dto) {
        String normalizedEmail = normalizeEmail(dto.email());
        boolean emailInUse = users.existsByEmail(normalizedEmail);
        boolean weakPassword = !isValidPassword(dto.password());

        if (emailInUse || weakPassword) {
            return RegistrationResult.failure(emailInUse, weakPassword);
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedEmail);
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(User.Role.USER);
        user.setEnabled(false);
        users.save(user);

        emailVerificationService.createAndSendToken(user, resolveLocale());
        return RegistrationResult.successful();
    }

    public EmailVerificationStatus verifyEmail(String token) {
        return emailVerificationService.verify(token);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    private Locale resolveLocale() {
        if (userContext != null && userContext.hasLocale()) {
            return userContext.getLocale();
        }
        Locale locale = LocaleContextHolder.getLocale();
        return locale != null ? locale : Locale.ENGLISH;
    }

    public String getEmailInUseMessage() {
        return messageSource.getMessage("auth.register.error.emailUsed", null, resolveLocale());
    }

    public String getPasswordValidationMessage() {
        return messageSource.getMessage("auth.password.requirements", null, resolveLocale());
    }

    public String getRegistrationSuccessMessage() {
        return messageSource.getMessage("auth.register.success", null, resolveLocale());
    }
    public String getVerificationMessage(EmailVerificationStatus status) {
        String key = switch (status) {
            case VERIFIED -> "auth.verify.success";
            case ALREADY_VERIFIED -> "auth.verify.already";
            case EXPIRED -> "auth.verify.expired";
            case NOT_FOUND, INVALID -> "auth.verify.invalid";
        };
        return messageSource.getMessage(key, null, resolveLocale());
    }

}