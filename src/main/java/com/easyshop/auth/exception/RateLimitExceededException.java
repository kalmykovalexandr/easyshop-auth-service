package com.easyshop.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when rate limit is exceeded.
 * Results in HTTP 429 Too Many Requests response.
 */
@Getter
public class RateLimitExceededException extends ApplicationException {

    private final Long retryAfterSeconds;

    public RateLimitExceededException(Long retryAfterSeconds) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS, retryAfterSeconds);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
