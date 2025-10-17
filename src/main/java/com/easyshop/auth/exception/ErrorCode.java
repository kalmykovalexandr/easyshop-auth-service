package com.easyshop.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Centralized error codes for the authentication service.
 * Each error code contains an English message and HTTP status.
 * Client is responsible for translating error codes to user's language.
 */
@Getter
public enum ErrorCode {

    // ========== Validation Errors (400 Bad Request) ==========
    EMAIL_INVALID("Invalid email format", HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED("Email is required", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_USED("Email is already registered", HttpStatus.CONFLICT),
    PASSWORD_INVALID("Invalid password", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED("Password is required", HttpStatus.BAD_REQUEST),
    PASSWORD_WEAK("Password must be at least 8 characters with uppercase, lowercase, digit, and special character", HttpStatus.BAD_REQUEST),
    PASSWORDS_DO_NOT_MATCH("Passwords do not match", HttpStatus.BAD_REQUEST),
    FIELD_REQUIRED("Required field is missing", HttpStatus.BAD_REQUEST),
    FIELD_INVALID("Invalid field value", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER("Invalid parameter: {0} = {1}", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR("Validation failed", HttpStatus.BAD_REQUEST),

    // ========== Authentication Errors ==========
    INVALID_CREDENTIALS("Invalid email or password", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED("Account is disabled", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED("Account is locked", HttpStatus.FORBIDDEN),
    TOKEN_EXPIRED("Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("Invalid token", HttpStatus.UNAUTHORIZED),

    // ========== Email Verification Errors ==========
    VERIFICATION_CODE_NOT_FOUND("Verification code not found", HttpStatus.NOT_FOUND),
    VERIFICATION_CODE_EXPIRED("Verification code has expired. Request a new code", HttpStatus.GONE),
    VERIFICATION_CODE_INVALID("Invalid verification code", HttpStatus.BAD_REQUEST),
    TOO_MANY_VERIFICATION_ATTEMPTS("Too many incorrect attempts. Request a new code", HttpStatus.BAD_REQUEST),
    ALREADY_VERIFIED("Email is already verified", HttpStatus.CONFLICT),
    VERIFICATION_COOLDOWN("Please wait {0} seconds before requesting a new code", HttpStatus.TOO_MANY_REQUESTS),

    // ========== Password Reset Errors ==========
    PASSWORD_RESET_CODE_NOT_FOUND("Password reset code not found", HttpStatus.NOT_FOUND),
    PASSWORD_RESET_CODE_EXPIRED("Password reset code has expired", HttpStatus.GONE),
    PASSWORD_RESET_CODE_INVALID("Invalid password reset code", HttpStatus.BAD_REQUEST),
    PASSWORD_RESET_TOO_MANY_ATTEMPTS("Too many incorrect attempts for password reset", HttpStatus.BAD_REQUEST),

    // ========== Resource Not Found (404) ==========
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    RESOURCE_NOT_FOUND("Resource not found", HttpStatus.NOT_FOUND),

    // ========== Access Errors (403 Forbidden) ==========
    ACCESS_DENIED("Access denied", HttpStatus.FORBIDDEN),
    INSUFFICIENT_PERMISSIONS("Insufficient permissions", HttpStatus.FORBIDDEN),

    // ========== Rate Limiting (429 Too Many Requests) ==========
    RATE_LIMIT_EXCEEDED("Rate limit exceeded. Try again in {0} seconds", HttpStatus.TOO_MANY_REQUESTS),
    TOO_MANY_REQUESTS("Too many requests", HttpStatus.TOO_MANY_REQUESTS),

    // ========== Server Errors (5xx) ==========
    INTERNAL_SERVER_ERROR("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR("Database error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_ERROR("External service error", HttpStatus.BAD_GATEWAY),
    EMAIL_SEND_ERROR("Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR),

    // ========== Generic Errors ==========
    OPERATION_FAILED("Operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    UNSUPPORTED_MEDIA_TYPE("Unsupported media type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    METHOD_NOT_ALLOWED("HTTP method {0} is not allowed", HttpStatus.METHOD_NOT_ALLOWED);

    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
