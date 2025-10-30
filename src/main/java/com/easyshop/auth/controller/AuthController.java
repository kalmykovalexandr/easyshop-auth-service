package com.easyshop.auth.controller;

import com.easyshop.auth.model.dto.AuthDto;
import com.easyshop.auth.model.dto.OtpSendDto;
import com.easyshop.auth.model.dto.OtpSendResultDto;
import com.easyshop.auth.model.dto.PasswordResetDto;
import com.easyshop.auth.model.dto.VerifyCodeDto;
import com.easyshop.auth.model.dto.VerifyCodeResponseDto;
import com.easyshop.auth.service.AuthServiceInt;
import com.easyshop.auth.service.OtpServiceInt;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthServiceInt authService;
    private final OtpServiceInt otpService;

    public AuthController(AuthServiceInt authService,
                          OtpServiceInt otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OtpSendResultDto> register(@Valid @RequestBody AuthDto dto) {
        OtpSendResultDto result = authService.register(dto);
        return ResponseEntity.accepted().body(result);
    }

    @PostMapping(value = "/send-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OtpSendResultDto> sendVerificationCode(@Valid @RequestBody OtpSendDto dto) {
        OtpSendResultDto result = otpService.generateOtp(dto.getEmail());
        return ResponseEntity.accepted().body(result);
    }

    @PostMapping(value = "/verify-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifyCodeResponseDto> verifyCode(@Valid @RequestBody VerifyCodeDto dto) {
        VerifyCodeResponseDto response = otpService.verifyOtp(dto);
        return response == null ?
                ResponseEntity.noContent().build() : // registration flow
                ResponseEntity.ok(response);         // forgot login flow
    }

    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetDto request) {
        authService.resetPassword(request);
        return ResponseEntity.accepted().build();
    }
}
