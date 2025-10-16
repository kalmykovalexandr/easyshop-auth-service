package com.easyshop.auth.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class VerificationRateLimiter {

    private final Cache<String, AtomicInteger> ipAttemptCache;
    private final int maxAttempts;
    private final boolean enabled;

    public VerificationRateLimiter(
            @Value("${easyshop.auth.verification-ip-rate-limit-enabled}") boolean enabled,
            @Value("${easyshop.auth.verification-ip-max-attempts}") int maxAttempts,
            @Value("${easyshop.auth.verification-ip-window-minutes}") int windowMinutes) {
        this.enabled = enabled;
        this.maxAttempts = maxAttempts;
        this.ipAttemptCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(windowMinutes))
                .maximumSize(10_000)
                .build();
    }

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
}
