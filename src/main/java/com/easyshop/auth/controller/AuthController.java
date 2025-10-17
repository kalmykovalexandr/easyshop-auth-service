package com.easyshop.auth.controller;

import com.easyshop.auth.service.impl.AuthService;
import com.easyshop.auth.service.OtpServiceInt;
import com.easyshop.auth.model.dto.AuthDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpServiceInt otpServiceInt;

    public AuthController(AuthService authService,
                          OtpServiceInt otpServiceInt) {
        this.authService = authService;
        this.otpServiceInt = otpServiceInt;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> register(@Valid @RequestBody AuthDto dto) {
        authService.register(dto);
        return ResponseEntity.ok().build();
    }

    // this endpoint is used for :
    // 1. REGISTRATION SEND CODE AT THE START
    // 2. REGISTRATION RESEND CODE IF USER REQUIRED
    // 3. FORGOT PASSWORD
    @PostMapping(value = "/send-verification-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sendVerificationCode(@RequestParam String email) {
        otpServiceInt.generateOtp(email);
        return ResponseEntity.accepted().build();
    }

    @PostMapping(value = "/verify-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> verifyCode(@RequestBody Map<String, String> body, HttpServletRequest request) {
        // TODO maybe some dto here instead of Map?
        String email = body.getOrDefault("email", "");
        String code = body.getOrDefault("code", "");
        otpServiceInt.verifyOtp(email, code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/update-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updatePassword(@RequestBody Map<String, String> body) {
        // TODO maybe some dto here instead of Map?
        String email = body.getOrDefault("email", "");
        String newPassword = body.getOrDefault("password", "");
        String confirmPassword = body.getOrDefault("confirmPassword", "");
        authService.updatePassword(email, newPassword, confirmPassword);
        return ResponseEntity.noContent().build();
    }
}
