package com.easyshop.auth.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private final LocaleResolver localeResolver;
    private final UserContext userContext;

    public UserContextInterceptor(LocaleResolver localeResolver, UserContext userContext) {
        this.localeResolver = localeResolver;
        this.userContext = userContext;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        Locale locale = localeResolver.resolveLocale(request);
        userContext.setLocale(locale);
        userContext.setClientIp(resolveClientIp(request));
        return true;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
