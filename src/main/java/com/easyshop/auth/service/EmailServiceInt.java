package com.easyshop.auth.service;

public interface EmailServiceInt {
    void sendVerificationEmail(String email, String otpCode);
}
