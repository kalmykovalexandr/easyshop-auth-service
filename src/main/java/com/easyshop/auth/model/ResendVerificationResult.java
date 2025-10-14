package com.easyshop.auth.model;

public record ResendVerificationResult(Status status, String message, Long retryAfterSeconds) {

    public enum Status {
        SUCCESS,
        NOT_FOUND,
        ALREADY_VERIFIED,
        RATE_LIMITED,
        ERROR
    }

    public static ResendVerificationResult success(String message) {
        return new ResendVerificationResult(Status.SUCCESS, message, null);
    }

    public static ResendVerificationResult notFound(String message) {
        return new ResendVerificationResult(Status.NOT_FOUND, message, null);
    }

    public static ResendVerificationResult alreadyVerified(String message) {
        return new ResendVerificationResult(Status.ALREADY_VERIFIED, message, null);
    }

    public static ResendVerificationResult rateLimited(String message, long retryAfterSeconds) {
        return new ResendVerificationResult(Status.RATE_LIMITED, message, retryAfterSeconds);
    }

    public static ResendVerificationResult error(String message) {
        return new ResendVerificationResult(Status.ERROR, message, null);
    }
}
