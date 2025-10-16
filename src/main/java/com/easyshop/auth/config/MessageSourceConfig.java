package com.easyshop.auth.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Configuration for internationalization (i18n) message sources.
 *
 * Provides two separate message sources:
 * 1. Primary MessageSource - for UI messages (messages_*.properties)
 * 2. Error MessageSource - for error messages (errors_*.properties)
 *
 * This separation allows:
 * - Clear distinction between UI text and error messages
 * - Independent management of error messages
 * - Better organization and maintainability
 */
@Configuration
public class MessageSourceConfig {

    /**
     * Primary message source for general UI messages.
     * Uses messages_*.properties files (e.g., messages_en.properties, messages_ru.properties).
     *
     * @return configured MessageSource for UI messages
     */
    @Bean
    @Primary
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setCacheSeconds(3600); // Cache for 1 hour
        return messageSource;
    }

    /**
     * Dedicated message source for error messages.
     * Uses errors_*.properties files (e.g., errors_en.properties, errors_ru.properties).
     *
     * This bean is specifically for exception handling and error responses.
     *
     * @return configured MessageSource for error messages
     */
    @Bean("errorMessageSource")
    public MessageSource errorMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("errors");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setCacheSeconds(3600); // Cache for 1 hour
        return messageSource;
    }
}
