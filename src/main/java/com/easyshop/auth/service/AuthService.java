package com.easyshop.auth.service;

import com.easyshop.auth.context.UserContext;
import com.easyshop.auth.entity.EmailVerificationCode;
import com.easyshop.auth.entity.User;
import com.easyshop.auth.model.EmailVerificationStatus;
import com.easyshop.auth.model.RegistrationResult;
import com.easyshop.auth.model.ResendVerificationResult;
import com.easyshop.auth.repository.UserRepository;
import com.easyshop.auth.security.VerificationCodeGenerator;
import com.easyshop.auth.security.VerificationCodeHasher;
import com.easyshop.auth.web.dto.AuthDto;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling user authentication, registration, and email verification.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;
    private final UserContext userContext;
    private final EmailService emailService;
    private final VerificationCodeGenerator codeGenerator;
    private final VerificationCodeHasher codeHasher;
    private final int maxVerificationAttempts;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    public AuthService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       MessageSource messageSource,
                       UserContext userContext,
                       EmailService emailService,
                       VerificationCodeGenerator codeGenerator,
                       VerificationCodeHasher codeHasher,
                       @Value("${easyshop.auth.verification-max-attempts:5}") int maxVerificationAttempts) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.messageSource = messageSource;
        this.userContext = userContext;
        this.emailService = emailService;
        this.codeGenerator = codeGenerator;
        this.codeHasher = codeHasher;
        this.maxVerificationAttempts = maxVerificationAttempts;

        log.info("AuthService initialized: maxVerificationAttempts={}", maxVerificationAttempts);
    }

    /**
     * Registers a new user and sends verification code via email.
     *
     * @param dto registration data
     * @return registration result
     */
    @Transactional
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

        // Generate code and send email
        String plainCode = codeGenerator.generateCode();
        emailService.createAndSendVerificationCode(user, resolveLocale(), plainCode);

        return RegistrationResult.successful();
    }

    /**
     * Resends verification code to user's email with cooldown protection.
     *
     * @param email user's email
     * @return resend result
     */
    @Transactional
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

        // Check cooldown
        Optional<Long> cooldownRemaining = emailService.findLatestVerificationCode(user)
                .flatMap(code -> {
                    LocalDateTime now = LocalDateTime.now();
                    Duration elapsed = Duration.between(code.getCreatedAt(), now);
                    Duration cooldown = calculateCooldown(code);
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

        // Generate and send new code
        String plainCode = codeGenerator.generateCode();
        emailService.createAndSendVerificationCode(user, locale, plainCode);

        return ResendVerificationResult.success(getResendSuccessMessage());
    }

    /**
     * Verifies a user's email with the provided code.
     * Implements "soft" rate limiting strategy: after max attempts, code is invalidated
     * and user must request a new one.
     *
     * @param email user's email
     * @param code verification code
     * @return verification status
     */
    @Transactional
    public EmailVerificationStatus verifyCode(String email, String code) {
        String normalized = normalizeEmail(email);
        if (normalized.isBlank() || code == null || code.trim().isEmpty()) {
            return EmailVerificationStatus.INVALID;
        }

        Optional<User> maybeUser = users.findByEmail(normalized);
        if (maybeUser.isEmpty()) {
            return EmailVerificationStatus.NOT_FOUND;
        }

        User user = maybeUser.get();

        // Check if already verified
        if (Boolean.TRUE.equals(user.getEnabled())) {
            return EmailVerificationStatus.ALREADY_VERIFIED;
        }

        // Find latest code
        Optional<EmailVerificationCode> maybeCode = emailService.findLatestVerificationCode(user);
        if (maybeCode.isEmpty()) {
            return EmailVerificationStatus.NOT_FOUND;
        }

        EmailVerificationCode codeEntity = maybeCode.get();

        // Check if expired
        if (codeEntity.isExpired(LocalDateTime.now())) {
            emailService.deleteVerificationCodesForUser(user);
            return EmailVerificationStatus.EXPIRED;
        }

        // Check if max attempts exceeded
        if (codeEntity.hasExceededAttempts(maxVerificationAttempts)) {
            log.warn("Max verification attempts exceeded for user: {}", user.getEmail());
            emailService.deleteVerificationCodesForUser(user);
            return EmailVerificationStatus.TOO_MANY_ATTEMPTS;
        }

        // Verify the code using constant-time comparison via BCrypt
        boolean codeMatches = codeHasher.verifyCode(code.trim(), codeEntity.getCodeHash());

        if (codeMatches) {
            // Success: activate user and delete all codes
            user.setEnabled(true);
            users.save(user);
            emailService.deleteVerificationCodesForUser(user);
            log.info("User verified successfully: {}", user.getEmail());
            return EmailVerificationStatus.VERIFIED;
        } else {
            // Failed attempt: increment counter
            codeEntity.incrementAttempts();
            users.save(user); // save to persist the incremented attempts

            log.warn("Invalid verification code for user: {} (attempt {}/{})",
                    user.getEmail(), codeEntity.getAttempts(), maxVerificationAttempts);

            // Check if this was the last attempt
            if (codeEntity.hasExceededAttempts(maxVerificationAttempts)) {
                log.warn("Max verification attempts reached for user: {}", user.getEmail());
                emailService.deleteVerificationCodesForUser(user);
                return EmailVerificationStatus.TOO_MANY_ATTEMPTS;
            }

            return EmailVerificationStatus.INVALID;
        }
    }

    // =============================================================================
    // Helper Methods
    // =============================================================================

    /**
     * Calculates cooldown duration based on previous attempts.
     * Increases cooldown if there were failed verification attempts (soft strategy).
     *
     * @param code the verification code entity
     * @return cooldown duration
     */
    private Duration calculateCooldown(EmailVerificationCode code) {
        Duration baseCooldown = emailService.getResendCooldown();

        // If there were failed attempts, increase cooldown
        if (code.getAttempts() >= 3) {
            // After 3+ failed attempts, increase cooldown to 5 minutes
            return Duration.ofMinutes(5);
        } else if (code.getAttempts() > 0) {
            // After 1-2 failed attempts, double the cooldown
            return baseCooldown.multipliedBy(2);
        }

        return baseCooldown;
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

    // =============================================================================
    // Message Methods
    // =============================================================================

    public String getEmailInUseMessage() {
        return messageSource.getMessage("auth.register.error.emailUsed", null, resolveLocale());
    }

    public String getPasswordValidationMessage() {
        return messageSource.getMessage("auth.password.requirements", null, resolveLocale());
    }

    public String getRegistrationSuccessMessage() {
        return messageSource.getMessage("auth.register.success", null, resolveLocale());
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
