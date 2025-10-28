package com.easyshop.auth.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordResetDto {

    private final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Pattern(regexp = PASSWORD_PATTERN)
    private String password;

    @NotBlank
    @Pattern(regexp = PASSWORD_PATTERN)
    private String confirmPassword;

    @NotBlank
    private String resetToken;

    @AssertTrue
    public boolean isPasswordsMatch() {
        return password.equals(confirmPassword);
    }
}
