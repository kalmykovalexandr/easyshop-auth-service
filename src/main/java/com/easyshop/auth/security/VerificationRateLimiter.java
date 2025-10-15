package com.easyshop.auth.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP-based rate limiter for verification code attempts.
 * Prevents distributed brute-force attacks by limiting attempts per IP address.
 */
@Component
public class VerificationRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(VerificationRateLimiter.class);

    private final Cache<String, AtomicInteger> ipAttemptCache;
    private final int maxAttempts;
    private final boolean enabled;

    public VerificationRateLimiter(
            @Value("${easyshop.auth.verification-ip-rate-limit-enabled:true}") boolean enabled,
            @Value("${easyshop.auth.verification-ip-max-attempts:20}") int maxAttempts,
            @Value("${easyshop.auth.verification-ip-window-minutes:60}") int windowMinutes) {
        this.enabled = enabled;
        this.maxAttempts = maxAttempts;

        this.ipAttemptCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(windowMinutes))
                .maximumSize(10_000)
                .build();

        log.info("VerificationRateLimiter initialized: enabled={}, maxAttempts={}, windowMinutes={}",
                enabled, maxAttempts, windowMinutes);
    }

    /**
     * Records a verification attempt from an IP address.
     *
     * @param ipAddress the IP address
     * @return true if attempt is allowed, false if rate limit exceeded
     */
    public boolean allowAttempt(String ipAddress) {
        if (!enabled) {
            return true;
        }

        if (ipAddress == null || ipAddress.isBlank()) {
            // If we can't determine IP, allow the attempt (fail open)
            log.warn("IP address is null or blank, allowing attempt");
            return true;
        }

        AtomicInteger attempts = ipAttemptCache.get(ipAddress, k -> new AtomicInteger(0));
        int currentAttempts = attempts.incrementAndGet();

        if (currentAttempts > maxAttempts) {
            log.warn("Rate limit exceeded for IP: {} (attempts: {})", ipAddress, currentAttempts);
            return false;
        }

        if (currentAttempts > maxAttempts * 0.8) {
            log.info("IP {} approaching rate limit: {}/{}", ipAddress, currentAttempts, maxAttempts);
        }

        return true;
    }

    /**
     * Gets the current attempt count for an IP address.
     *
     * @param ipAddress the IP address
     * @return current attempt count
     */
    public int getAttemptCount(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return 0;
        }
        AtomicInteger attempts = ipAttemptCache.getIfPresent(ipAddress);
        return attempts != null ? attempts.get() : 0;
    }

    /**
     * Resets attempts for an IP address (useful for testing or manual intervention).
     *
     * @param ipAddress the IP address
     */
    public void resetAttempts(String ipAddress) {
        if (ipAddress != null && !ipAddress.isBlank()) {
            ipAttemptCache.invalidate(ipAddress);
            log.info("Reset attempts for IP: {}", ipAddress);
        }
    }

    /**
     * Checks if rate limiting is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}
