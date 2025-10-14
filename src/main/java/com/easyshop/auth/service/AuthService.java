package com.easyshop.auth.service;

import com.easyshop.auth.context.UserContext;
import com.easyshop.auth.entity.User;
import com.easyshop.auth.model.EmailVerificationStatus;
import com.easyshop.auth.model.RegistrationResult;
import com.easyshop.auth.model.ResendVerificationResult;
import com.easyshop.auth.repository.UserRepository;
import com.easyshop.auth.web.dto.AuthDto;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
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
    private final EmailService emailService;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    public AuthService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       MessageSource messageSource,
                       UserContext userContext,
                       EmailService emailService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.messageSource = messageSource;
        this.userContext = userContext;
        this.emailService = emailService;
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

        emailService.createAndSendToken(user, resolveLocale());
        return RegistrationResult.successful();
    }

    public EmailVerificationStatus verifyEmail(String token) {
        return emailService.verify(token);
    }

    public ResendVerificationResult resendVerification(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            return ResendVerificationResult.error(getResendGenericErrorMessage());
        }

        Locale locale = resolveLocale();
        Optional<User> maybeUser = users.findByEmail(normalizedEmail);
        if (maybeUser.isEmpty()) {
            return ResendVerificationResult.notFound(getResendGenericErrorMessage());
        }

        User user = maybeUser.get();
        if (Boolean.TRUE.equals(user.getEnabled())) {
            return ResendVerificationResult.alreadyVerified(getResendAlreadyVerifiedMessage());
        }

        Optional<Long> cooldownRemaining = emailService.findLatestToken(user)
                .flatMap(token -> {
                    LocalDateTime now = LocalDateTime.now();
                    Duration elapsed = Duration.between(token.getCreatedAt(), now);
                    Duration cooldown = emailService.getResendCooldown();
                    Duration remaining;
                    if (elapsed.isNegative()) {
                        remaining = cooldown;
                    } else if (elapsed.compareTo(cooldown) < 0) {
                        remaining = cooldown.minus(elapsed);
                    } else {
                        return Optional.empty();
                    }
                    long seconds = Math.max(1, remaining.getSeconds());
                    return Optional.of(seconds);
                });

        if (cooldownRemaining.isPresent()) {
            long seconds = cooldownRemaining.get();
            return ResendVerificationResult.rateLimited(getResendCooldownMessage(seconds), seconds);
        }

        emailService.createAndSendResendToken(user, locale);
        return ResendVerificationResult.success(getResendSuccessMessage());
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

    public String getResendSuccessMessage() {
        return messageSource.getMessage("login.register.modal.resendSuccess", null, resolveLocale());
    }

    public String getResendGenericErrorMessage() {
        return messageSource.getMessage("login.register.modal.resendError", null, resolveLocale());
    }

    public String getResendCooldownMessage(long seconds) {
        return messageSource.getMessage("auth.resend.cooldown", new Object[]{seconds}, resolveLocale());
    }

    public String getResendAlreadyVerifiedMessage() {
        return messageSource.getMessage("auth.resend.alreadyVerified", null, resolveLocale());
    }
}
