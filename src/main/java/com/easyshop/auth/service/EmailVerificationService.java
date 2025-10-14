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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final Duration DEFAULT_EXPIRATION = Duration.ofHours(24);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final String verificationBaseUrl;
    private final Duration tokenTtl;
    private final Duration resendCooldown;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                    UserRepository userRepository,
                                    @Value("${easyshop.auth.verification-base-url:http://localhost:9001/api/auth/verify}") String verificationBaseUrl,
                                    @Value("${easyshop.auth.verification-ttl-hours:24}") long tokenTtlHours,
                                    @Value("${easyshop.auth.verification-resend-cooldown-seconds:60}") String resendCooldownSecondsValue) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.verificationBaseUrl = verificationBaseUrl;
        this.tokenTtl = tokenTtlHours > 0 ? Duration.ofHours(tokenTtlHours) : DEFAULT_EXPIRATION;
        long resolvedCooldownSeconds = parseLongOrDefault(resendCooldownSecondsValue, 60L);
        this.resendCooldown = resolvedCooldownSeconds > 0 ? Duration.ofSeconds(resolvedCooldownSeconds) : Duration.ofSeconds(60);
    }

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

    @Transactional
    public void createAndSendToken(User user, Locale locale) {
        tokenRepository.deleteByUser(user);
        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString();
        EmailVerificationToken tokenEntity = new EmailVerificationToken(token, user, now.plus(tokenTtl));
        tokenRepository.save(tokenEntity);

        log.info("Email verification token generated for {} (locale {}): {}?token={}",
                user.getEmail(), locale != null ? locale : Locale.ENGLISH, verificationBaseUrl, token);
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
}
