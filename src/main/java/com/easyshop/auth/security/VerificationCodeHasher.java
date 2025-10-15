package com.easyshop.auth.security;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;

/**
 * Handles hashing and verification of OTP codes using BCrypt.
 * Provides secure storage and constant-time comparison.
 */
@Component
public class VerificationCodeHasher {

    private static final int BCRYPT_ROUNDS = 10;

    /**
     * Hashes a verification code using BCrypt.
     *
     * @param plainCode the plain text code
     * @return BCrypt hash of the code
     */
    public String hashCode(String plainCode) {
        if (plainCode == null || plainCode.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or blank");
        }
        return BCrypt.hashpw(plainCode, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verifies a plain code against a hashed code using constant-time comparison.
     *
     * @param plainCode the plain text code to verify
     * @param hashedCode the stored BCrypt hash
     * @return true if the code matches, false otherwise
     */
    public boolean verifyCode(String plainCode, String hashedCode) {
        if (plainCode == null || plainCode.isBlank() || hashedCode == null || hashedCode.isBlank()) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainCode, hashedCode);
        } catch (IllegalArgumentException e) {
            // Invalid hash format
            return false;
        }
    }

    /**
     * Constant-time comparison utility (for additional safety).
     *
     * @param a first byte array
     * @param b second byte array
     * @return true if arrays are equal, false otherwise
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length != b.length) {
            return false;
        }
        return MessageDigest.isEqual(a, b);
    }
}
