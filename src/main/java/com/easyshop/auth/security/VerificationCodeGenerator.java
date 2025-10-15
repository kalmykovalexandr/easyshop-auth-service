package com.easyshop.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates cryptographically secure verification codes for email verification.
 */
@Component
public class VerificationCodeGenerator {

    private final SecureRandom secureRandom;
    private final int codeLength;

    public VerificationCodeGenerator(
            @Value("${easyshop.auth.verification-code-length:6}") int codeLength) {
        this.secureRandom = new SecureRandom();
        this.codeLength = Math.max(4, Math.min(codeLength, 8)); // clamp between 4-8
    }

    /**
     * Generates a cryptographically secure numeric verification code.
     *
     * @return numeric code as String (e.g., "123456")
     */
    public String generateCode() {
        int min = (int) Math.pow(10, codeLength - 1);
        int max = (int) Math.pow(10, codeLength) - 1;
        int range = max - min + 1;

        int code = secureRandom.nextInt(range) + min;
        return String.valueOf(code);
    }

    /**
     * Gets the configured code length.
     *
     * @return code length (4-8 digits)
     */
    public int getCodeLength() {
        return codeLength;
    }
}
