package com.easyshop.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a verification code is invalid.
 * Results in HTTP 400 Bad Request response.
 */
public class InvalidVerificationCodeException extends ApplicationException {

    public InvalidVerificationCodeException() {
        super(ErrorCode.VERIFICATION_CODE_INVALID, HttpStatus.BAD_REQUEST);
    }
}
