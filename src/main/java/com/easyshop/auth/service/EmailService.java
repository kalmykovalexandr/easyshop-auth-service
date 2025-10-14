package com.easyshop.auth.service;

import com.easyshop.auth.entity.EmailVerificationToken;
import com.easyshop.auth.entity.User;
import com.easyshop.auth.model.EmailVerificationStatus;
import com.easyshop.auth.repository.EmailVerificationTokenRepository;
import com.easyshop.auth.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class EmailService {
    
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final Duration DEFAULT_EXPIRATION = Duration.ofHours(24);
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MessageSource messageSource;
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final String fromEmail;
    private final String fromName;
    private final String verificationBaseUrl;
    private final Duration tokenTtl;
    private final Duration resendCooldown;
    
    public EmailService(JavaMailSender mailSender,
                       TemplateEngine templateEngine,
                       MessageSource messageSource,
                       EmailVerificationTokenRepository tokenRepository,
                       UserRepository userRepository,
                       @Value("${easyshop.mail.from-email:}") String fromEmail,
                       @Value("${easyshop.mail.from-name:EasyShop}") String fromName,
                       @Value("${easyshop.auth.verification-base-url:http://localhost:9001/api/auth/verify}") String verificationBaseUrl,
                       @Value("${easyshop.auth.verification-ttl-hours:24}") long tokenTtlHours,
                       @Value("${easyshop.auth.verification-resend-cooldown-seconds:60}") String resendCooldownSecondsValue) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.verificationBaseUrl = verificationBaseUrl;
        this.tokenTtl = tokenTtlHours > 0 ? Duration.ofHours(tokenTtlHours) : DEFAULT_EXPIRATION;
        long resolvedCooldownSeconds = parseLongOrDefault(resendCooldownSecondsValue, 60L);
        this.resendCooldown = resolvedCooldownSeconds > 0 ? Duration.ofSeconds(resolvedCooldownSeconds) : Duration.ofSeconds(60);
    }
    
    /**
     * Sends email verification message to user
     * @param user User to send verification to
     * @param token Verification token
     * @param locale User's locale
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendVerificationEmail(User user, String token, Locale locale) {
        if (!isEmailConfigured()) {
            log.warn("Email not configured, skipping verification email for user: {}", user.getEmail());
            return false;
        }
        
        try {
            String verificationUrl = verificationBaseUrl + "?token=" + token;
            String subject = messageSource.getMessage("email.verification.subject", null, locale);
            String htmlContent = buildVerificationEmailHtml(user, verificationUrl, locale);
            
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
     * Sends resend verification message to user
     * @param user User to send verification to
     * @param token Verification token
     * @param locale User's locale
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendResendVerificationEmail(User user, String token, Locale locale) {
        if (!isEmailConfigured()) {
            log.warn("Email not configured, skipping resend verification email for user: {}", user.getEmail());
            return false;
        }
        
        try {
            String verificationUrl = verificationBaseUrl + "?token=" + token;
            String subject = messageSource.getMessage("email.resend.subject", null, locale);
            String htmlContent = buildResendVerificationEmailHtml(user, verificationUrl, locale);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            log.info("Resend verification email sent successfully to: {}", user.getEmail());
            return true;
            
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send resend verification email to: {}", user.getEmail(), e);
            return false;
        }
    }
    
    // =============================================================================
    // Token Management Methods (from EmailVerificationService)
    // =============================================================================
    
    @Transactional
    public void createAndSendToken(User user, Locale locale) {
        tokenRepository.deleteByUser(user);
        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString();
        EmailVerificationToken tokenEntity = new EmailVerificationToken(token, user, now.plus(tokenTtl));
        tokenRepository.save(tokenEntity);

        // Send verification email
        boolean emailSent = sendVerificationEmail(user, token, locale);
        
        if (emailSent) {
            log.info("Email verification sent successfully to: {} (locale: {})", 
                    user.getEmail(), locale != null ? locale : Locale.ENGLISH);
        } else {
            log.warn("Failed to send verification email to: {} (locale: {}). Token: {}?token={}", 
                    user.getEmail(), locale != null ? locale : Locale.ENGLISH, verificationBaseUrl, token);
        }
    }
    
    @Transactional
    public void createAndSendResendToken(User user, Locale locale) {
        tokenRepository.deleteByUser(user);
        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString();
        EmailVerificationToken tokenEntity = new EmailVerificationToken(token, user, now.plus(tokenTtl));
        tokenRepository.save(tokenEntity);

        // Send resend verification email
        boolean emailSent = sendResendVerificationEmail(user, token, locale);
        
        if (emailSent) {
            log.info("Resend verification email sent successfully to: {} (locale: {})", 
                    user.getEmail(), locale != null ? locale : Locale.ENGLISH);
        } else {
            log.warn("Failed to send resend verification email to: {} (locale: {}). Token: {}?token={}", 
                    user.getEmail(), locale != null ? locale : Locale.ENGLISH, verificationBaseUrl, token);
        }
    }
    
    @Transactional(readOnly = true)
    public Optional<EmailVerificationToken> findLatestToken(User user) {
        return tokenRepository.findFirstByUserOrderByCreatedAtDesc(user);
    }
    
    public Duration getResendCooldown() {
        return resendCooldown;
    }
    
    @Transactional
    public EmailVerificationStatus verify(String token) {
        if (token == null || token.isBlank()) {
            return EmailVerificationStatus.INVALID;
        }

        return tokenRepository.findByToken(token)
                .map(this::applyVerification)
                .orElse(EmailVerificationStatus.NOT_FOUND);
    }
    
    private EmailVerificationStatus applyVerification(EmailVerificationToken token) {
        LocalDateTime now = LocalDateTime.now();
        if (token.isUsed()) {
            return EmailVerificationStatus.ALREADY_VERIFIED;
        }
        if (token.isExpired(now)) {
            tokenRepository.delete(token);
            return EmailVerificationStatus.EXPIRED;
        }

        User user = token.getUser();
        if (Boolean.TRUE.equals(user.getEnabled())) {
            token.setUsedAt(now);
            tokenRepository.save(token);
            return EmailVerificationStatus.ALREADY_VERIFIED;
        }

        user.setEnabled(true);
        userRepository.save(user);
        token.setUsedAt(now);
        tokenRepository.save(token);
        tokenRepository.deleteByUser(user);
        return EmailVerificationStatus.VERIFIED;
    }
    
    @Transactional
    public void purgeExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
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
            log.warn("Invalid resend cooldown value '{}'. Falling back to {} seconds.", value, defaultValue);
            return defaultValue;
        }
    }
    
    private boolean isEmailConfigured() {
        return StringUtils.hasText(fromEmail);
    }
    
    private String buildVerificationEmailHtml(User user, String verificationUrl, Locale locale) {
        Context context = new Context(locale);
        context.setVariable("userName", user.getEmail());
        context.setVariable("verificationUrl", verificationUrl);
        
        return templateEngine.process("email/verification", context);
    }
    
    private String buildResendVerificationEmailHtml(User user, String verificationUrl, Locale locale) {
        Context context = new Context(locale);
        context.setVariable("userName", user.getEmail());
        context.setVariable("verificationUrl", verificationUrl);
        
        return templateEngine.process("email/resend-verification", context);
    }
}
