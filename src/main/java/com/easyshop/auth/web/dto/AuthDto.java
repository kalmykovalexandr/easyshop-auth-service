package com.easyshop.auth.web.dto;

import jakarta.validation.constraints.*;

public record AuthDto(@Email @NotBlank String email, @NotBlank String password) {
}

