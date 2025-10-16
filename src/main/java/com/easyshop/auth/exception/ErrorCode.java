package com.easyshop.auth.exception;

import lombok.Getter;

/**
 * Centralized error codes for the authentication service.
 * Each error code maps to a message key in messages.properties for i18n support.
 */
@Getter
public enum ErrorCode {

    // ========== Validation Errors (400 Bad Request) ==========
    INVALID_EMAIL("error.validation.email.invalid"),
    INVALID_PASSWORD("error.validation.password.invalid"),
    EMAIL_ALREADY_EXISTS("auth.register.error.emailUsed"),
    PASSWORDS_DO_NOT_MATCH("error.validation.password.mismatch"),
    MISSING_REQUIRED_FIELD("error.validation.field.required"),
    INVALID_PARAMETER("error.validation.parameter.invalid"),

    // ========== Authentication Errors ==========
    INVALID_CREDENTIALS("error.auth.invalidCredentials"),
    ACCOUNT_DISABLED("error.auth.accountDisabled"),
    ACCOUNT_LOCKED("error.auth.accountLocked"),
    TOKEN_EXPIRED("error.auth.tokenExpired"),
    TOKEN_INVALID("error.auth.tokenInvalid"),

    // ========== Email Verification Errors ==========
    VERIFICATION_CODE_NOT_FOUND("error.verification.code.notFound"),
    VERIFICATION_CODE_EXPIRED("auth.verify.expired"),
    VERIFICATION_CODE_INVALID("error.verification.code.invalid"),
    TOO_MANY_VERIFICATION_ATTEMPTS("error.verification.tooManyAttempts"),
    ALREADY_VERIFIED("auth.verify.already"),
    VERIFICATION_RATE_LIMITED("error.verification.rateLimited"),
    VERIFICATION_COOLDOWN("auth.resend.cooldown"),

    // ========== Password Reset Errors ==========
    PASSWORD_RESET_CODE_NOT_FOUND("password.reset.code.notfound"),
    PASSWORD_RESET_CODE_EXPIRED("password.reset.code.expired"),
    PASSWORD_RESET_CODE_INVALID("password.reset.code.invalid"),
    PASSWORD_RESET_TOO_MANY_ATTEMPTS("password.reset.code.toomany"),

    // ========== Resource Not Found (404) ==========
    USER_NOT_FOUND("error.user.notFound"),
    RESOURCE_NOT_FOUND("error.resource.notFound"),

    // ========== Access Errors (403 Forbidden) ==========
    ACCESS_DENIED("error.access.denied"),
    INSUFFICIENT_PERMISSIONS("error.access.insufficientPermissions"),

    // ========== Rate Limiting (429 Too Many Requests) ==========
    RATE_LIMIT_EXCEEDED("error.rateLimit.exceeded"),
    TOO_MANY_REQUESTS("error.rateLimit.tooManyRequests"),

    // ========== Server Errors (5xx) ==========
    INTERNAL_SERVER_ERROR("error.internal.server"),
    DATABASE_ERROR("error.internal.database"),
    EXTERNAL_SERVICE_ERROR("error.internal.externalService"),
    EMAIL_SEND_ERROR("error.internal.emailSend"),

    // ========== Generic Errors ==========
    OPERATION_FAILED("error.operation.failed"),
    UNSUPPORTED_MEDIA_TYPE("error.unsupportedMediaType"),
    METHOD_NOT_ALLOWED("error.methodNotAllowed");

    private final String messageKey;

    ErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * Gets the i18n message key for this error code.
     * The key is used to look up localized error messages.
     *
     * @return the message key
     */
    public String getMessageKey() {
        return messageKey;
    }
}
