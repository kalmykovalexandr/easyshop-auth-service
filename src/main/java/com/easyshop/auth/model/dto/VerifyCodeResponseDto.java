package com.easyshop.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VerifyCodeResponseDto {
    private String resetToken;
}
