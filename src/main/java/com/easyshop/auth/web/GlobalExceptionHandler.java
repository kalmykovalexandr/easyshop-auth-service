package com.easyshop.auth.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Global exception handler for validation errors.
 * Catches @Valid annotation failures and formats them consistently with the existing API.
 * Uses MessageSource for internationalized error messages.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String field = error.getField();
            String code = error.getCode(); // Email, NotBlank, Pattern, UniqueEmail, etc.
            String message;

            // Resolve i18n messages based on field and constraint type
            if ("email".equals(field)) {
                if ("UniqueEmail".equals(code)) {
                    message = messageSource.getMessage("auth.register.error.emailUsed", null, locale);
                } else if ("Email".equals(code)) {
                    message = messageSource.getMessage("login.register.error.email", null, locale);
                } else {
                    message = error.getDefaultMessage();
                }
            } else if ("password".equals(field)) {
                if ("Pattern".equals(code)) {
                    message = messageSource.getMessage("login.register.error.password", null, locale);
                } else {
                    message = error.getDefaultMessage();
                }
            } else {
                message = error.getDefaultMessage();
            }

            fieldErrors.put(field, message);
        }

        String detail = String.join(" ", fieldErrors.values());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("detail", detail.trim());
        body.put("errors", fieldErrors);

        log.debug("Validation error for locale {}: {}", locale, fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
