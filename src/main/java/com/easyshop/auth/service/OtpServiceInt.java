package com.easyshop.auth.service;

public interface OtpServiceInt {
    void generateOtp(String key);
    boolean verifyOtp(String key, String code);
}
