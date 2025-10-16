package com.easyshop.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all application-specific business logic errors.
 * Extends RuntimeException so it doesn't require explicit exception handling.
 *
 * All custom business exceptions should extend this class.
 * GlobalExceptionHandler will catch and format these exceptions uniformly.
 */
@Getter
public abstract class ApplicationException extends RuntimeException {

    /**
     * Error code for machine-readable error identification.
     */
    private final ErrorCode errorCode;

    /**
     * HTTP status to return for this exception.
     */
    private final HttpStatus httpStatus;

    /**
     * Arguments for parameterized error messages (i18n).
     * Example: "User {0} not found" with args = ["john@example.com"]
     */
    private final Object[] messageArgs;

    /**
     * Creates an application exception with error code and HTTP status.
     *
     * @param errorCode the error code
     * @param httpStatus the HTTP status to return
     */
    protected ApplicationException(ErrorCode errorCode, HttpStatus httpStatus) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.messageArgs = null;
    }

    /**
     * Creates an application exception with error code, HTTP status, and message arguments.
     *
     * @param errorCode the error code
     * @param httpStatus the HTTP status to return
     * @param messageArgs arguments for parameterized error messages
     */
    protected ApplicationException(ErrorCode errorCode, HttpStatus httpStatus, Object... messageArgs) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.messageArgs = messageArgs;
    }

    /**
     * Creates an application exception with a cause.
     *
     * @param errorCode the error code
     * @param httpStatus the HTTP status to return
     * @param cause the underlying cause
     */
    protected ApplicationException(ErrorCode errorCode, HttpStatus httpStatus, Throwable cause) {
        super(errorCode.getMessageKey(), cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.messageArgs = null;
    }

    /**
     * Creates an application exception with message arguments and a cause.
     *
     * @param errorCode the error code
     * @param httpStatus the HTTP status to return
     * @param cause the underlying cause
     * @param messageArgs arguments for parameterized error messages
     */
    protected ApplicationException(ErrorCode errorCode, HttpStatus httpStatus, Throwable cause, Object... messageArgs) {
        super(errorCode.getMessageKey(), cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.messageArgs = messageArgs;
    }
}
