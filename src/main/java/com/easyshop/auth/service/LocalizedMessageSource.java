package com.easyshop.auth.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Wrapper around MessageSource that automatically resolves the user's locale.
 * Uses dedicated error message source (errors_*.properties) for all error messages.
 *
 * This eliminates the need to manually pass Locale to getMessage() calls.
 *
 * Usage:
 * <pre>
 * // Instead of:
 * messageSource.getMessage("error.key", null, LocaleContextHolder.getLocale())
 *
 * // Simply use:
 * localizedMessageSource.getMessage("error.key")
 * </pre>
 */
@Component
public class LocalizedMessageSource {

    private final MessageSource errorMessageSource;

    public LocalizedMessageSource(@Qualifier("errorMessageSource") MessageSource errorMessageSource) {
        this.errorMessageSource = errorMessageSource;
    }

    /**
     * Gets a localized error message for the current user's locale.
     * Locale is automatically resolved from LocaleContextHolder.
     *
     * @param code the message code to look up (from errors_*.properties)
     * @return the localized error message
     */
    public String getMessage(String code) {
        return errorMessageSource.getMessage(code, null, resolveLocale());
    }

    /**
     * Gets a localized error message with arguments for the current user's locale.
     *
     * @param code the message code to look up (from errors_*.properties)
     * @param args arguments to fill in placeholders in the message
     * @return the localized error message with arguments applied
     */
    public String getMessage(String code, Object[] args) {
        return errorMessageSource.getMessage(code, args, resolveLocale());
    }

    /**
     * Gets a localized error message with a default fallback.
     *
     * @param code the message code to look up (from errors_*.properties)
     * @param defaultMessage fallback message if code is not found
     * @return the localized error message or default if not found
     */
    public String getMessage(String code, String defaultMessage) {
        return errorMessageSource.getMessage(code, null, defaultMessage, resolveLocale());
    }

    /**
     * Gets a localized error message with arguments and a default fallback.
     *
     * @param code the message code to look up (from errors_*.properties)
     * @param args arguments to fill in placeholders
     * @param defaultMessage fallback message if code is not found
     * @return the localized error message or default if not found
     */
    public String getMessage(String code, Object[] args, String defaultMessage) {
        return errorMessageSource.getMessage(code, args, defaultMessage, resolveLocale());
    }

    /**
     * Resolves the current user's locale from Spring's LocaleContextHolder.
     * This is automatically set by Spring based on Accept-Language header.
     *
     * @return the current user's locale
     */
    private Locale resolveLocale() {
        return LocaleContextHolder.getLocale();
    }
}
