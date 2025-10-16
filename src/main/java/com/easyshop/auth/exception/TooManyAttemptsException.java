package com.easyshop.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when too many verification attempts have been made.
 * Results in HTTP 429 Too Many Requests response.
 */
public class TooManyAttemptsException extends ApplicationException {

    public TooManyAttemptsException() {
        super(ErrorCode.TOO_MANY_VERIFICATION_ATTEMPTS, HttpStatus.TOO_MANY_REQUESTS);
    }
}
