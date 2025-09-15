package com.easyshop.auth.web;

import com.easyshop.common.web.ApiResponseDto;
import com.easyshop.auth.service.AuthService;
import com.easyshop.auth.web.dto.AuthDto;
import jakarta.validation.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @GetMapping("/healthz")
    public ApiResponseDto health() {
        return new ApiResponseDto(true, null);
    }

    @GetMapping("/readyz")
    public ApiResponseDto ready() {
        return new ApiResponseDto(true, null);
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<ApiResponseDto> register(@Valid @RequestBody AuthDto d) {
        if (!service.register(d)) {
            String message = "Email already used or password does not meet requirements. " + service.getPasswordValidationMessage();
            return ResponseEntity.badRequest().body(new ApiResponseDto(false, message));
        }
        return ResponseEntity.ok(new ApiResponseDto(true, "Registration successful"));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<ApiResponseDto> login(@Valid @RequestBody AuthDto d) {
        boolean isValid = service.login(d);
        if (!isValid)
            return ResponseEntity.badRequest().body(new ApiResponseDto(false, "Invalid credentials"));
        return ResponseEntity.ok(new ApiResponseDto(true, "Login successful - redirect to OIDC"));
    }

    @GetMapping("/api/auth/password-requirements")
    public ResponseEntity<ApiResponseDto> getPasswordRequirements() {
        return ResponseEntity.ok(new ApiResponseDto(true, service.getPasswordValidationMessage()));
    }

}
