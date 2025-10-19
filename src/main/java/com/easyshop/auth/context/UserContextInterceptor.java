package com.easyshop.auth.context;

import com.easyshop.auth.security.IpRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private final IpRateLimiter ipRateLimiter;
    private final Set<String> trustedProxies;

    public UserContextInterceptor(IpRateLimiter ipRateLimiter,
                                  @Value("${easyshop.auth.trusted-proxies}") String trustedProxyList) {
        this.ipRateLimiter = ipRateLimiter;
        this.trustedProxies = parseTrustedProxies(trustedProxyList);
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String clientIp = resolveClientIp(request);
        ipRateLimiter.check(request.getRequestURI(), clientIp);
        return true;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank() && !StringUtils.containsWhitespace(forwarded)) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    private Set<String> parseTrustedProxies(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Stream.of(raw.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        return trustedProxies.contains(remoteAddr);
    }
}
