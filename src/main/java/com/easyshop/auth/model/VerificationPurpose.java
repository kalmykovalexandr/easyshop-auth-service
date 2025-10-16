package com.easyshop.auth.model;

/**
 * Purpose of verification code.
 */
public enum VerificationPurpose {
    /**
     * Email verification during registration.
     * For users who are not yet activated (enabled = false).
     */
    REGISTRATION,

    /**
     * Password reset verification.
     * For users who are already activated (enabled = true).
     */
    PASSWORD_RESET
}
