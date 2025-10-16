package com.easyshop.auth.web;

import com.easyshop.auth.exception.ApplicationException;
import com.easyshop.auth.exception.ErrorCode;
import com.easyshop.auth.exception.RateLimitExceededException;
import com.easyshop.auth.service.LocalizedMessageSource;
import com.easyshop.auth.web.dto.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handler for all application errors.
 * Provides consistent error response format and internationalization support.
 *
 * Handles:
 * - Validation errors (@Valid, @Validated)
 * - Custom business exceptions (ApplicationException)
 * - Spring framework exceptions
 * - Database exceptions
 * - Generic unexpected errors
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private final LocalizedMessageSource messageSource;
    private final Environment environment;

    public GlobalExceptionHandler(LocalizedMessageSource messageSource, Environment environment) {
        this.messageSource = messageSource;
        this.environment = environment;
    }

    /**
     * Handles validation errors from @Valid/@Validated annotations.
     * Returns field-specific error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String field = error.getField();
            String code = error.getCode();
            String message;

            // Resolve i18n messages based on field and constraint type
            if ("email".equals(field)) {
                if ("UniqueEmail".equals(code)) {
                    message = messageSource.getMessage("auth.register.error.emailUsed");
                } else if ("Email".equals(code)) {
                    message = messageSource.getMessage("login.register.error.email");
                } else {
                    message = error.getDefaultMessage();
                }
            } else if ("password".equals(field)) {
                if ("Pattern".equals(code)) {
                    message = messageSource.getMessage("login.register.error.password");
                } else {
                    message = error.getDefaultMessage();
                }
            } else {
                message = error.getDefaultMessage();
            }

            fieldErrors.put(field, message);
        }

        String detail = String.join(" ", fieldErrors.values());

        ErrorResponse response = ErrorResponse.builder()
                .detail(detail.trim())
                .errors(fieldErrors)
                .errorCode("VALIDATION_ERROR")
                .status(HttpStatus.BAD_REQUEST.name())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();

        log.debug("Validation errors at {}: {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles all custom business logic exceptions.
     * Automatically resolves i18n messages and HTTP status codes.
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(
            ApplicationException ex,
            HttpServletRequest request) {

        String message = messageSource.getMessage(ex.getErrorCode().getMessageKey(), ex.getMessageArgs());

        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .detail(message)
                .errorCode(ex.getErrorCode().name())
                .status(ex.getHttpStatus().name())
                .statusCode(ex.getHttpStatus().value())
                .path(request.getRequestURI());

        // Add retry-after for rate limit exceptions
        if (ex instanceof RateLimitExceededException rateLimitEx) {
            builder.retryAfterSeconds(rateLimitEx.getRetryAfterSeconds());
        }

        ErrorResponse response = builder.build();

        // Log based on severity
        if (ex.getHttpStatus().is5xxServerError()) {
            log.error("Application error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        } else {
            log.warn("Application error at {}: {} - {}", request.getRequestURI(), ex.getErrorCode(), message);
        }

        // Add Retry-After header for rate limiting
        if (ex instanceof RateLimitExceededException rateLimitEx) {
            return ResponseEntity
                    .status(ex.getHttpStatus())
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(rateLimitEx.getRetryAfterSeconds()))
                    .body(response);
        }

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * Handles Spring Security access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        String message = messageSource.getMessage(ErrorCode.ACCESS_DENIED.getMessageKey());

        ErrorResponse response = ErrorResponse.builder()
                .detail(message)
                .errorCode(ErrorCode.ACCESS_DENIED.name())
                .status(HttpStatus.FORBIDDEN.name())
                .statusCode(HttpStatus.FORBIDDEN.value())
                .path(request.getRequestURI())
                .build();

        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handles database errors.
     */
    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ResponseEntity<ErrorResponse> handleDatabaseError(
            Exception ex,
            HttpServletRequest request) {

        String message = messageSource.getMessage(ErrorCode.DATABASE_ERROR.getMessageKey());

        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .detail(message)
                .errorCode(ErrorCode.DATABASE_ERROR.name())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getRequestURI());

        if (isDevelopmentMode()) {
            builder.debugMessage(ex.getMessage());
        }

        ErrorResponse response = builder.build();

        log.error("Database error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handles method argument type mismatch (e.g., passing string for integer parameter).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String message = messageSource.getMessage(
                ErrorCode.INVALID_PARAMETER.getMessageKey(),
                new Object[]{ex.getName(), ex.getValue()});

        ErrorResponse response = ErrorResponse.builder()
                .detail(message)
                .errorCode(ErrorCode.INVALID_PARAMETER.name())
                .status(HttpStatus.BAD_REQUEST.name())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();

        log.debug("Type mismatch at {}: parameter '{}' with value '{}'",
                request.getRequestURI(), ex.getName(), ex.getValue());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles missing required request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String message = messageSource.getMessage(
                ErrorCode.MISSING_REQUIRED_FIELD.getMessageKey(),
                new Object[]{ex.getParameterName()});

        ErrorResponse response = ErrorResponse.builder()
                .detail(message)
                .errorCode(ErrorCode.MISSING_REQUIRED_FIELD.name())
                .status(HttpStatus.BAD_REQUEST.name())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();

        log.debug("Missing parameter at {}: '{}'", request.getRequestURI(), ex.getParameterName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles unsupported media type errors (e.g., sending XML when only JSON is accepted).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        String message = messageSource.getMessage(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessageKey());

        ErrorResponse response = ErrorResponse.builder()
                .detail(message)
                .errorCode(ErrorCode.UNSUPPORTED_MEDIA_TYPE.name())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.name())
                .statusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .path(request.getRequestURI())
                .build();

        log.debug("Unsupported media type at {}: {}", request.getRequestURI(), ex.getContentType());

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    /**
     * Handles HTTP method not allowed (e.g., POST to GET-only endpoint).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        String message = messageSource.getMessage(
                ErrorCode.METHOD_NOT_ALLOWED.getMessageKey(),
                new Object[]{ex.getMethod()});

        ErrorResponse response = ErrorResponse.builder()
                .detail(message)
                .errorCode(ErrorCode.METHOD_NOT_ALLOWED.name())
                .status(HttpStatus.METHOD_NOT_ALLOWED.name())
                .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value())
                .path(request.getRequestURI())
                .build();

        log.debug("Method not allowed at {}: {}", request.getRequestURI(), ex.getMethod());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Fallback handler for all unhandled exceptions.
     * Logs full stack trace and returns generic error message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        String message = messageSource.getMessage(ErrorCode.INTERNAL_SERVER_ERROR.getMessageKey());

        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .detail(message)
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.name())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getRequestURI());

        // Add debug info in development mode
        if (isDevelopmentMode()) {
            builder.debugMessage(ex.getMessage());
        }

        ErrorResponse response = builder.build();

        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Checks if application is running in development mode.
     */
    private boolean isDevelopmentMode() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
}
