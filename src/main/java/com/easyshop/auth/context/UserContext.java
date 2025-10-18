package com.easyshop.auth.context;

import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class UserContext {

    private Locale locale;
    private String clientIp;

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public boolean hasLocale() {
        return locale != null;
    }

    public String getLanguage() {
        return locale != null ? locale.getLanguage() : null;
    }

    public boolean matchesLanguage(String language) {
        if (locale == null || language == null) {
            return false;
        }
        return Objects.equals(locale.getLanguage(), language);
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        if (clientIp == null) {
            this.clientIp = null;
            return;
        }
        String trimmed = clientIp.trim();
        this.clientIp = trimmed.isEmpty() ? null : trimmed;
    }

    public boolean hasClientIp() {
        return clientIp != null && !clientIp.isBlank();
    }
}
