package com.easyshop.auth.service;

import com.easyshop.auth.model.dto.OtpSendResultDto;
import com.easyshop.auth.model.dto.VerifyCodeDto;
import com.easyshop.auth.model.dto.VerifyCodeResponseDto;

public interface OtpServiceInt {
    default OtpSendResultDto generateOtp(String email) {
        return generateOtp(email, false);
    }

    OtpSendResultDto generateOtp(String email, boolean forceResend);
    VerifyCodeResponseDto verifyOtp(VerifyCodeDto dto);
    void validateResetToken(String email, String resetToken);
}
