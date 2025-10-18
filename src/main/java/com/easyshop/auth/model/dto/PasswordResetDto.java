package com.easyshop.auth.model.dto;

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

    private static final String PASSWORD_COMPLEXITY_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
    private static final String MESSAGE = "Password must be at least 8 characters long and contain at least one uppercase letter," +
                    " one lowercase letter, one digit, and one special character (@$!%*?&)";

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Pattern(
            regexp = PASSWORD_COMPLEXITY_REGEX,
            message = MESSAGE
    )
    private String password;

    @NotBlank
    @Pattern(
            regexp = PASSWORD_COMPLEXITY_REGEX,
            message = MESSAGE
    )
    private String confirmPassword;

    @NotBlank
    private String resetToken;
}
