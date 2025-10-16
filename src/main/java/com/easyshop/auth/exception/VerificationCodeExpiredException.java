package com.easyshop.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a verification code has expired.
 * Results in HTTP 410 Gone response.
 */
public class VerificationCodeExpiredException extends ApplicationException {

    public VerificationCodeExpiredException() {
        super(ErrorCode.VERIFICATION_CODE_EXPIRED, HttpStatus.GONE);
    }
}
