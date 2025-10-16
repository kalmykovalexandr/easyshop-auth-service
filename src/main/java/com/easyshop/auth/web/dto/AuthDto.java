package com.easyshop.auth.web.dto;

import com.easyshop.auth.validation.UniqueEmail;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.util.Locale;


@Getter
public class AuthDto {

    @Email
    @NotBlank
    @UniqueEmail
    private final String email;

    @NotBlank
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must be at least 8 characters long and contain at least one uppercase letter," +
                " one lowercase letter, one digit, and one special character (@$!%*?&)"
    )
    private final String password;

    public AuthDto(String email, String password) {
        this.email = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        this.password = password;
    }
}

