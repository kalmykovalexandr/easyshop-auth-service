package com.easyshop.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to verify an already verified account.
 * Results in HTTP 409 Conflict response.
 */
public class AlreadyVerifiedException extends ApplicationException {

    public AlreadyVerifiedException() {
        super(ErrorCode.ALREADY_VERIFIED, HttpStatus.CONFLICT);
    }
}
