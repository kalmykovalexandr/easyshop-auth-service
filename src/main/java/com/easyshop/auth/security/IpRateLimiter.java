package com.easyshop.auth.security;

import com.easyshop.auth.exception.RateLimitExceededException;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class IpRateLimiter {

    private final RedisTemplate<String, String> redis;
    private final Duration window;
    private final int maxRequests;
    private final Set<String> limitedPaths;

    public IpRateLimiter(RedisTemplate<String, String> redis,
                         @Value("${easyshop.auth.rate-limit.window-seconds}") long windowSeconds,
                         @Value("${easyshop.auth.rate-limit.max-requests}") int maxRequests,
                         @Value("${easyshop.auth.rate-limit.paths}") String pathList) {
        this.redis = redis;
        this.window = Duration.ofSeconds(Math.max(windowSeconds, 1));
        this.maxRequests = Math.max(maxRequests, 1);
        this.limitedPaths = parsePaths(pathList);
    }

    public void check(String path, String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        if (!isLimitedPath(path)) {
            return;
        }

        String key = ipKey(ipAddress);
        Long requests = redis.opsForValue().increment(key);

        if (requests != null && requests == 1L) {
            redis.expire(key, window);
        }

        if (requests != null && requests > maxRequests) {
            Long remaining = redis.getExpire(key, TimeUnit.SECONDS);
            int retryAfter = remaining != null && remaining > 0
                    ? remaining.intValue()
                    : (int) window.getSeconds();
            throw new RateLimitExceededException(retryAfter, null);
        }
    }

    private boolean isLimitedPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        if (limitedPaths.contains(path)) {
            return true;
        }
        // allow for trailing slash variants
        if (path.endsWith("/") && limitedPaths.contains(path.substring(0, path.length() - 1))) {
            return true;
        }
        return !path.endsWith("/") && limitedPaths.contains(path + "/");
    }

    private String ipKey(String ip) {
        return "otp:ip:%s".formatted(ip);
    }

    private Set<String> parsePaths(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }
        return Stream.of(raw.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
