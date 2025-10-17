package com.easyshop.auth.service;

import com.easyshop.auth.context.UserContext;
import com.easyshop.auth.entity.EmailVerificationCode;
import com.easyshop.auth.entity.User;
import com.easyshop.auth.model.EmailVerificationStatus;
import com.easyshop.auth.model.RegistrationResult;
import com.easyshop.auth.model.ResendVerificationResult;
import com.easyshop.auth.model.VerificationPurpose;
import com.easyshop.auth.repository.UserRepository;
import com.easyshop.auth.security.VerificationCodeGenerator;
import com.easyshop.auth.security.VerificationCodeHasher;
import com.easyshop.auth.web.dto.AuthDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import static com.easyshop.auth.utils.AuthUtils.isValidPassword;
import static com.easyshop.auth.utils.AuthUtils.normalizeEmail;

@Slf4j
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;
    private final UserContext userContext;
    private final EmailService emailService;
    private final VerificationCodeHasher codeHasher;
    private final int maxVerificationAttempts;

    public AuthService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       MessageSource messageSource,
                       UserContext userContext,
                       EmailService emailService,
                       VerificationCodeGenerator codeGenerator,
                       VerificationCodeHasher codeHasher,
                       @Value("${easyshop.auth.verification-max-attempts}") int maxVerificationAttempts) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.messageSource = messageSource;
        this.userContext = userContext;
        this.emailService = emailService;
        this.codeHasher = codeHasher;
        this.maxVerificationAttempts = maxVerificationAttempts;
    }

    @Transactional
    public void register(AuthDto dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(User.Role.USER);
        user.setEnabled(false);
        users.save(user);

        // TODO do it via Kafka messages
        emailService.createAndSendVerificationCode(user, resolveLocale());
    }

    @Transactional
    public ResendVerificationResult sendVerificationCode(String email, VerificationPurpose purpose) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            return ResendVerificationResult.error(getResendGenericErrorMessage());
        }

        Locale locale = resolveLocale();
        Optional<User> maybeUser = users.findByEmail(normalizedEmail);

        if (maybeUser.isEmpty()) {
            // For password reset, don't reveal if user exists (email enumeration protection)
            if (purpose == VerificationPurpose.PASSWORD_RESET) {
                return ResendVerificationResult.success("If an account exists, you will receive a code.");
            }
            return ResendVerificationResult.notFound(getResendGenericErrorMessage());
        }

        User user = maybeUser.get();

        // Check user status based on purpose
        if (purpose == VerificationPurpose.REGISTRATION) {
            if (Boolean.TRUE.equals(user.getEnabled())) {
                return ResendVerificationResult.alreadyVerified(getResendAlreadyVerifiedMessage());
            }
        } else if (purpose == VerificationPurpose.PASSWORD_RESET) {
            if (!Boolean.TRUE.equals(user.getEnabled())) {
                // User not verified yet, can't reset password
                // Don't reveal this for security (email enumeration protection)
                log.warn("Password reset requested for non-verified user: {}", normalizedEmail);
                return ResendVerificationResult.success("If an account exists, you will receive a code.");
            }
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


        if (purpose == VerificationPurpose.PASSWORD_RESET) {
            emailService.createAndSendPasswordResetCode(user, locale);
            log.info("Password reset code sent to user: {}", user.getEmail());
        } else {
            emailService.createAndSendVerificationCode(user, locale);
        }

        return ResendVerificationResult.success(getResendSuccessMessage());
    }

    @Transactional
    public ResendVerificationResult resendVerification(String email) {
        return sendVerificationCode(email, VerificationPurpose.REGISTRATION);
    }

    @Transactional
    public EmailVerificationStatus verifyCode(String email, String code, boolean enableUserOnSuccess) {
        String normalized = normalizeEmail(email);
        if (normalized.isBlank() || code == null || code.trim().isEmpty()) {
            return EmailVerificationStatus.INVALID;
        }

        Optional<User> maybeUser = users.findByEmail(normalized);
        if (maybeUser.isEmpty()) {
            return EmailVerificationStatus.NOT_FOUND;
        }

        User user = maybeUser.get();

        // For registration verification, check if already verified
        if (enableUserOnSuccess && Boolean.TRUE.equals(user.getEnabled())) {
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
            // Success: optionally activate user and delete codes
            if (enableUserOnSuccess) {
                user.setEnabled(true);
                users.save(user);
                emailService.deleteVerificationCodesForUser(user);
                log.info("User verified successfully: {}", user.getEmail());
            } else {
                // For password reset, don't delete code yet - needed for password update
                log.info("Password reset code verified for user: {}", user.getEmail());
            }
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



    private Locale resolveLocale() {
        if (userContext != null && userContext.hasLocale()) {
            return userContext.getLocale();
        }
        Locale locale = LocaleContextHolder.getLocale();
        return locale != null ? locale : Locale.ENGLISH;
    }



    // =============================================================================
    // Password Reset Methods (reusing email verification infrastructure)
    // =============================================================================

    /**
     * Completes password reset after successful code verification.
     */
    @Transactional
    public boolean completePasswordReset(String email, String newPassword) {
        String normalized = normalizeEmail(email);
        Optional<User> userOpt = users.findByEmail(normalized);

        if (userOpt.isEmpty()) {
            return false;
        }

        if (!isValidPassword(newPassword)) {
            return false;
        }

        User user = userOpt.get();

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        users.save(user);

        // Delete verification code
        emailService.deleteVerificationCodesForUser(user);

        log.info("Password reset completed for user: {}", user.getEmail());
        return true;
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
