package com.easyshop.auth.controller;

import com.easyshop.auth.model.dto.AuthDto;
import com.easyshop.auth.model.dto.OtpSendDto;
import com.easyshop.auth.model.dto.PasswordResetDto;
import com.easyshop.auth.model.dto.VerifyCodeDto;
import com.easyshop.auth.service.AuthServiceInt;
import com.easyshop.auth.service.OtpServiceInt;
import com.easyshop.auth.service.impl.AuthService;
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

    private final AuthServiceInt authService;
    private final OtpServiceInt otpService;

    public AuthController(AuthService authService,
                          OtpServiceInt otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> register(@Valid @RequestBody AuthDto dto) {
        authService.register(dto);
        return ResponseEntity.accepted().build();
    }

    @PostMapping(value = "/send-verification-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> sendVerificationCode(@RequestParam OtpSendDto dto) {
        otpService.generateOtp(dto.getEmail());
        return ResponseEntity.accepted().build();
    }

    @PostMapping(value = "/verify-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> verifyCode(@Valid @RequestBody VerifyCodeDto dto) {
        otpService.verifyOtp(dto);
        return ResponseEntity.accepted().build();
    }

    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetDto request) {
        authService.resetPassword(request);
        return ResponseEntity.accepted().build();
    }
}
