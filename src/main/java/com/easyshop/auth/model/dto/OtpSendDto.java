package com.easyshop.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

@Getter
@Setter
public class OtpSendDto {

    @Email
    @NotBlank
    private String email;

    public OtpSendDto(String email) {
        this.email = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
    }

}
