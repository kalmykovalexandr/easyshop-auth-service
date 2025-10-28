package com.easyshop.auth.service;

import com.easyshop.auth.model.dto.VerifyCodeDto;
import com.easyshop.auth.model.dto.VerifyCodeResponseDto;

public interface OtpServiceInt {
    String generateOtp(String email);
    VerifyCodeResponseDto verifyOtp(VerifyCodeDto dto);
    void validateResetToken(String email, String resetToken);
}
