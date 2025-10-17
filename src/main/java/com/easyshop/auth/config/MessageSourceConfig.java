package com.easyshop.auth.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Configuration for internationalization (i18n) message sources.
 * Used for UI messages like success notifications, form labels, etc.
 *
 * Error messages are NOT localized on the server side.
 * They are returned as English error codes and translated client-side.
 */
@Configuration
public class MessageSourceConfig {

    /**
     * Message source for UI messages.
     * Uses messages_*.properties files (e.g., messages_en.properties, messages_ru.properties).
     *
     * @return configured MessageSource for UI messages
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setCacheSeconds(3600); // Cache for 1 hour
        return messageSource;
    }
}
