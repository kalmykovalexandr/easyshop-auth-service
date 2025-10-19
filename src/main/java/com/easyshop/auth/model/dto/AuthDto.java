package com.easyshop.auth.model.dto;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class AuthDto {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must be at least 8 characters long and contain at least one uppercase letter," +
                " one lowercase letter, one digit, and one special character (@$!%*?&)"
    )
    private String password;

    @JsonCreator
    public AuthDto(@JsonProperty("email") String email,
                   @JsonProperty("password") String password) {
        this.email = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        this.password = password;
    }
}

