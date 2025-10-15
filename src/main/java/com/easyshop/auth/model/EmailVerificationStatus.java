package com.easyshop.auth.model;

public enum EmailVerificationStatus {
    VERIFIED,
    ALREADY_VERIFIED,
    EXPIRED,
    NOT_FOUND,
    INVALID,
    TOO_MANY_ATTEMPTS
}
