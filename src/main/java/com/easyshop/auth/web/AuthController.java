package com.easyshop.auth.web;

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
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/readyz")
    public ResponseEntity<Void> ready() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthDto d) {
        if (!service.register(d)) {
            String message = "Email already used or password does not meet requirements. " + service.getPasswordValidationMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message));
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDto d) {
        boolean isValid = service.login(d);
        if (!isValid)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid credentials"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/auth/password-requirements")
    public ResponseEntity<String> getPasswordRequirements() {
        return ResponseEntity.ok(service.getPasswordValidationMessage());
    }

}
