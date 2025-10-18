package com.easyshop.auth.service;


import com.easyshop.auth.model.dto.AuthDto;
import com.easyshop.auth.model.dto.PasswordResetDto;

public interface AuthServiceInt {
    void register(AuthDto dto);
    void resetPassword(PasswordResetDto request);
}
