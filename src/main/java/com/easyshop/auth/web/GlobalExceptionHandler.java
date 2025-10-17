package com.easyshop.auth.web;

import com.easyshop.auth.exception.BusinessException;
import com.easyshop.auth.exception.ErrorCode;
import com.easyshop.auth.exception.RateLimitExceededException;
import com.easyshop.auth.web.dto.error.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
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
 * Returns consistent error response format with English messages and error codes.
 * Client is responsible for translating error codes to user's language.
 *
 * Handles:
 * - Validation errors (@Valid, @Validated)
 * - Custom business exceptions (BusinessException)
 * - JPA exceptions (EntityNotFoundException)
 * - Spring framework exceptions
 * - Database exceptions
 * - Generic unexpected errors
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private final Environment environment;

    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    /**
     * Handles validation errors from @Valid/@Validated annotations.
     * Returns field-specific error codes for client-side translation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String field = error.getField();
            String errorCode = mapValidationErrorToCode(error);
            fieldErrors.put(field, errorCode);
        }

        ErrorResponse response = ErrorResponse.builder()
                .detail(ErrorCode.VALIDATION_ERROR.getMessage())
                .errors(fieldErrors)
                .errorCode(ErrorCode.VALIDATION_ERROR.name())
                .status(HttpStatus.BAD_REQUEST.name())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();

        log.debug("Validation errors at {}: {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Maps Bean Validation constraint violations to error codes.
     */
    private String mapValidationErrorToCode(FieldError error) {
        String field = error.getField();
        String constraintCode = error.getCode();

        return switch (field) {
            case "email" -> switch (constraintCode) {
                case "UniqueEmail" -> ErrorCode.EMAIL_ALREADY_USED.name();
                case "Email" -> ErrorCode.EMAIL_INVALID.name();
                case "NotBlank" -> ErrorCode.EMAIL_REQUIRED.name();
                default -> ErrorCode.FIELD_INVALID.name();
            };
            case "password" -> switch (constraintCode) {
                case "Pattern" -> ErrorCode.PASSWORD_WEAK.name();
                case "NotBlank" -> ErrorCode.PASSWORD_REQUIRED.name();
                default -> ErrorCode.PASSWORD_INVALID.name();
            };
            default -> ErrorCode.FIELD_INVALID.name();
        };
    }

    /**
     * Handles all custom business logic exceptions.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus httpStatus = errorCode.getHttpStatus();

        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .detail(ex.getMessage())
                .errorCode(errorCode.name())
                .status(httpStatus.name())
                .statusCode(httpStatus.value())
                .path(request.getRequestURI());

        // Add retry-after for rate limit exceptions
        if (ex instanceof RateLimitExceededException rateLimitEx) {
            builder.retryAfterSeconds(rateLimitEx.getRetryAfterSeconds());
        }

        ErrorResponse response = builder.build();

        // Log based on severity
        if (httpStatus.is5xxServerError()) {
            log.error("Business exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        } else {
            log.warn("Business exception at {}: {} - {}", request.getRequestURI(), errorCode, ex.getMessage());
        }

        // Add Retry-After header for rate limiting
        if (ex instanceof RateLimitExceededException rateLimitEx) {
            return ResponseEntity
                    .status(httpStatus)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(rateLimitEx.getRetryAfterSeconds()))
                    .body(response);
        }

        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * Handles JPA EntityNotFoundException.
     * Converts to our standard error response format.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .detail(ErrorCode.RESOURCE_NOT_FOUND.getMessage())
                .errorCode(ErrorCode.RESOURCE_NOT_FOUND.name())
                .status(HttpStatus.NOT_FOUND.name())
                .statusCode(HttpStatus.NOT_FOUND.value())
                .path(request.getRequestURI())
                .build();

        log.warn("Entity not found at {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles Spring Security access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .detail(ErrorCode.ACCESS_DENIED.getMessage())
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

        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .detail(ErrorCode.DATABASE_ERROR.getMessage())
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

        String message = String.format(ErrorCode.INVALID_PARAMETER.getMessage(), ex.getName(), ex.getValue());

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

        ErrorResponse response = ErrorResponse.builder()
                .detail(ErrorCode.FIELD_REQUIRED.getMessage())
                .errorCode(ErrorCode.FIELD_REQUIRED.name())
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

        ErrorResponse response = ErrorResponse.builder()
                .detail(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage())
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

        String message = String.format(ErrorCode.METHOD_NOT_ALLOWED.getMessage(), ex.getMethod());

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

        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .detail(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
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
