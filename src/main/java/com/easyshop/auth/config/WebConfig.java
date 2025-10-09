package com.easyshop.auth.config;

import com.easyshop.auth.context.UserContextInterceptor;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ObjectProvider<UserContextInterceptor> userContextInterceptorProvider;

    public WebConfig(ObjectProvider<UserContextInterceptor> userContextInterceptorProvider) {
        this.userContextInterceptorProvider = userContextInterceptorProvider;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        UserContextInterceptor interceptor = userContextInterceptorProvider.getIfAvailable();
        if (interceptor != null) {
            registry.addInterceptor(interceptor);
        }
    }

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setCookieName("easyshop-lang");
        resolver.setCookiePath("/");
        resolver.setCookieMaxAge((int) Duration.ofDays(365).getSeconds());
        resolver.setDefaultLocale(null);
        return resolver;
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
