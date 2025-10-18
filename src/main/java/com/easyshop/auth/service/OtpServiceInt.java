package com.easyshop.auth.service;

import com.easyshop.auth.model.dto.VerifyCodeDto;

public interface OtpServiceInt {
    void generateOtp(String email);
    void verifyOtp(VerifyCodeDto dto);
}
