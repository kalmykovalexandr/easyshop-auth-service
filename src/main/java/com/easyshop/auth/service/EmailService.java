package com.easyshop.auth.service;

import com.easyshop.auth.entity.EmailVerificationCode;
import com.easyshop.auth.entity.User;
import com.easyshop.auth.repository.EmailVerificationCodeRepository;
import com.easyshop.auth.repository.UserRepository;
import com.easyshop.auth.security.VerificationCodeGenerator;
import com.easyshop.auth.security.VerificationCodeHasher;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

/**
 * Service for managing email verification codes during user registration.
 * Handles code generation, hashing, storage, and email delivery.
 */
@Slf4j
@Service
public class EmailService {
    private static final Duration DEFAULT_CODE_TTL = Duration.ofMinutes(15);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MessageSource messageSource;
    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final VerificationCodeGenerator codeGenerator;
    private final VerificationCodeHasher codeHasher;
    private final String fromEmail;
    private final String fromName;
    private final Duration codeTtl;
    private final Duration resendCooldown;

    public EmailService(JavaMailSender mailSender,
                       TemplateEngine templateEngine,
                       MessageSource messageSource,
                       EmailVerificationCodeRepository verificationCodeRepository,
                       UserRepository userRepository,
                       VerificationCodeGenerator codeGenerator,
                       VerificationCodeHasher codeHasher,
                       @Value("${easyshop.mail.from-email:}") String fromEmail,
                       @Value("${easyshop.mail.from-name:EasyShop}") String fromName,
                       @Value("${easyshop.auth.verification-ttl-minutes:15}") long codeTtlMinutes,
                       @Value("${easyshop.auth.verification-resend-cooldown-seconds:60}") String resendCooldownSecondsValue) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
        this.verificationCodeRepository = verificationCodeRepository;
        this.userRepository = userRepository;
        this.codeGenerator = codeGenerator;
        this.codeHasher = codeHasher;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.codeTtl = codeTtlMinutes > 0 ? Duration.ofMinutes(codeTtlMinutes) : DEFAULT_CODE_TTL;
        long resolvedCooldownSeconds = parseLongOrDefault(resendCooldownSecondsValue, 60L);
        this.resendCooldown = resolvedCooldownSeconds > 0 ? Duration.ofSeconds(resolvedCooldownSeconds) : Duration.ofSeconds(60);

        log.info("EmailService initialized: codeTtl={} min, resendCooldown={} sec",
                codeTtl.toMinutes(), resendCooldown.getSeconds());
    }

    // =============================================================================
    // Public API Methods
    // =============================================================================

    /**
     * Creates a verification code and sends it via email to the user.
     * Deletes any existing codes for the user before creating a new one.
     *
     * @param user User to send verification code to
     * @param locale User's locale for email content
     */
    @Transactional
    public void createAndSendVerificationCode(User user, Locale locale) {

        // Delete existing codes for this user
        verificationCodeRepository.deleteByUser(user);

        // Generate code and send email
        String plainCode = codeGenerator.generateCode();

        // Hash the code before storing
        String codeHash = codeHasher.hashCode(plainCode);

        // Create and save new verification code
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(codeTtl);
        EmailVerificationCode codeEntity = new EmailVerificationCode(user, codeHash, expiresAt);
        verificationCodeRepository.save(codeEntity);

        // Send verification email with plain code
        boolean emailSent = sendVerificationEmail(user, plainCode, locale);

        if (emailSent) {
            log.info("Email verification code sent successfully to: {} (locale: {})",
                    user.getEmail(), locale != null ? locale : Locale.ENGLISH);
        } else {
            log.warn("Failed to send verification email to: {} (locale: {})",
                    user.getEmail(), locale != null ? locale : Locale.ENGLISH);
        }
    }

    /**
     * Finds the latest verification code for a user.
     *
     * @param user the user
     * @return Optional containing the verification code if found
     */
    @Transactional(readOnly = true)
    public Optional<EmailVerificationCode> findLatestVerificationCode(User user) {
        return verificationCodeRepository.findFirstByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Gets the configured resend cooldown duration.
     *
     * @return resend cooldown
     */
    public Duration getResendCooldown() {
        return resendCooldown;
    }

    /**
     * Deletes all verification codes for a user.
     *
     * @param user the user
     */
    @Transactional
    public void deleteVerificationCodesForUser(User user) {
        verificationCodeRepository.deleteByUser(user);
    }

    /**
     * Deletes all expired verification codes from the database.
     * Should be called periodically by a scheduled task.
     */
    @Transactional
    public void purgeExpiredVerificationCodes() {
        verificationCodeRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    // =============================================================================
    // Email Sending Methods
    // =============================================================================

    /**
     * Sends email verification message to user with the plain code.
     *
     * @param user User to send verification to
     * @param plainCode Plain text verification code to include in email
     * @param locale User's locale
     * @return true if email was sent successfully, false otherwise
     */
    private boolean sendVerificationEmail(User user, String plainCode, Locale locale) {
        if (!isEmailConfigured()) {
            log.warn("Email not configured, skipping verification email for user: {}", user.getEmail());
            return false;
        }

        try {
            String subject = messageSource.getMessage("email.verification.subject", null, locale);
            String htmlContent = buildVerificationEmailHtml(plainCode, locale);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Verification email sent successfully to: {}", user.getEmail());
            return true;

        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
            return false;
        }
    }

    /**
     * Creates and sends password reset OTP code to user.
     * Reuses email verification code infrastructure and template.
     */
    @Transactional
    public void createAndSendPasswordResetCode(User user, Locale locale) {
        // Delete existing codes for this user
        verificationCodeRepository.deleteByUser(user);

        // Generate and send new code
        String plainCode = codeGenerator.generateCode();
        // Hash the code before storing
        String codeHash = codeHasher.hashCode(plainCode);

        // Create and save new verification code (reusing same table)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(codeTtl);
        EmailVerificationCode codeEntity = new EmailVerificationCode(user, codeHash, expiresAt);
        verificationCodeRepository.save(codeEntity);

        // Send password reset email (using same OTP template but different subject)
        sendPasswordResetEmail(user, plainCode, locale);
    }

    /**
     * Sends password reset email with OTP code.
     * Uses the same email template as verification but with different subject.
     */
    private void sendPasswordResetEmail(User user, String plainCode, Locale locale) {
        if (!isEmailConfigured()) {
            log.warn("Email not configured, skipping password reset email for user: {}", user.getEmail());
            return;
        }

        try {
            String subject = messageSource.getMessage("email.password.reset.subject", null, locale);
            // Reuse the same OTP template as email verification
            String htmlContent = buildVerificationEmailHtml(plainCode, locale);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Password reset OTP email sent successfully to: {}", user.getEmail());

        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send password reset OTP email to: {}", user.getEmail(), e);
        }
    }

    /**
     * Builds the HTML content for verification email using Thymeleaf template.
     *
     * @param plainCode the plain text code to display
     * @param locale user's locale
     * @return HTML email content
     */
    private String buildVerificationEmailHtml(String plainCode, Locale locale) {
        Context context = new Context(locale);
        context.setVariable("verificationCode", plainCode);
        return templateEngine.process("email/otp-verification", context);
    }

    // =============================================================================
    // Helper Methods
    // =============================================================================

    private long parseLongOrDefault(String value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ex) {
            log.warn("Invalid cooldown value '{}'. Falling back to {} seconds.", value, defaultValue);
            return defaultValue;
        }
    }

    private boolean isEmailConfigured() {
        return StringUtils.hasText(fromEmail);
    }
}
