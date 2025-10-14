package com.easyshop.auth.web;

import com.easyshop.auth.model.EmailVerificationStatus;
import com.easyshop.auth.model.RegistrationResult;
import com.easyshop.auth.model.ResendVerificationResult;
import com.easyshop.auth.service.AuthService;
import com.easyshop.auth.web.dto.AuthDto;
import com.easyshop.auth.web.dto.ResendVerificationRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
        RegistrationResult result = service.register(dto);

        if (result.hasErrors()) {
            Map<String, String> fieldErrors = new LinkedHashMap<>();

            if (result.emailInUse()) {
                fieldErrors.put("email", service.getEmailInUseMessage());
            }
            if (result.weakPassword()) {
                fieldErrors.put("password", service.getPasswordValidationMessage());
            }

            String detail = String.join(" ", fieldErrors.values());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", false);
            body.put("detail", detail.trim());
            body.put("errors", fieldErrors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        String success = service.getRegistrationSuccessMessage();
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", success
        ));
    }

    @GetMapping("/api/auth/password-requirements")
    public ResponseEntity<String> getPasswordRequirements() {
        return ResponseEntity.ok(service.getPasswordValidationMessage());
    }

    @GetMapping("/api/auth/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestParam("token") String token) {
        EmailVerificationStatus status = service.verifyEmail(token);
        String message = service.getVerificationMessage(status);
        boolean success = status == EmailVerificationStatus.VERIFIED || status == EmailVerificationStatus.ALREADY_VERIFIED;
        HttpStatus httpStatus = success ? HttpStatus.OK : HttpStatus.BAD_REQUEST;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", success);
        body.put("status", status.name());
        body.put("message", message);
        return ResponseEntity.status(httpStatus).body(body);
    }

    @PostMapping("/api/auth/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        ResendVerificationResult result = service.resendVerification(request.email());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", result.status() == ResendVerificationResult.Status.SUCCESS);

        return switch (result.status()) {
            case SUCCESS -> {
                body.put("message", result.message());
                yield ResponseEntity.ok(body);
            }
            case RATE_LIMITED -> {
                body.put("detail", result.message());
                if (result.retryAfterSeconds() != null) {
                    body.put("retryAfterSeconds", result.retryAfterSeconds());
                }
                ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
                if (result.retryAfterSeconds() != null) {
                    builder = builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
                }
                yield builder.body(body);
            }
            case ALREADY_VERIFIED -> {
                body.put("detail", result.message());
                yield ResponseEntity.status(HttpStatus.CONFLICT).body(body);
            }
            case NOT_FOUND -> {
                body.put("detail", result.message());
                yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
            }
            case ERROR -> {
                body.put("detail", result.message());
                yield ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
            }
        };
    }
}
