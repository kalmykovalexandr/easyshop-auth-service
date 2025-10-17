package com.easyshop.auth.exception;

import lombok.Getter;

/**
 * Exception thrown when rate limit is exceeded.
 * Results in HTTP 429 Too Many Requests response with Retry-After header.
 */
@Getter
public class RateLimitExceededException extends BusinessException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(int retryAfterSeconds) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, retryAfterSeconds);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
