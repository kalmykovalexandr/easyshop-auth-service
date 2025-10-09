package com.easyshop.auth.web;

import com.easyshop.auth.service.AuthService;
import com.easyshop.auth.web.dto.AuthDto;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @GetMapping("/healthz")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/readyz")
    public ResponseEntity<Map<String, Object>> ready() {
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody AuthDto dto) {
        if (!service.register(dto)) {
            String message = "Email already used or password does not meet requirements. "
                    + service.getPasswordValidationMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "ok", false,
                            "detail", message
                    ));
        }
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Registration successful"
        ));
    }

    @GetMapping("/api/auth/password-requirements")
    public ResponseEntity<String> getPasswordRequirements() {
        return ResponseEntity.ok(service.getPasswordValidationMessage());
    }
}