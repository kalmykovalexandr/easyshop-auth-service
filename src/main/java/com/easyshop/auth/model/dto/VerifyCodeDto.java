package com.easyshop.auth.model.dto;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class VerifyCodeDto {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String code;

    @Nullable
    private Boolean activateUser;

}
