package com.easyshop.auth.web;

import com.easyshop.auth.model.EmailVerificationStatus;
import com.easyshop.auth.model.RegistrationResult;
import com.easyshop.auth.model.ResendVerificationResult;
import com.easyshop.auth.security.VerificationRateLimiter;
import com.easyshop.auth.service.AuthService;
import com.easyshop.auth.web.dto.AuthDto;
import com.easyshop.auth.web.dto.ResendVerificationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for authentication endpoints including registration and email verification.
 */
@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService service;
    private final VerificationRateLimiter rateLimiter;

    public AuthController(AuthService service, VerificationRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
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

    /**
     * Verifies user's email with OTP code.
     * Includes IP-based rate limiting to prevent distributed brute-force attacks.
     */
    @PostMapping(value = "/api/auth/verify-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> verifyCode(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String email = body.getOrDefault("email", "");
        String code = body.getOrDefault("code", "");

        if (email.isBlank() || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "detail", "Invalid payload"
            ));
        }

        // IP-based rate limiting
        String clientIp = extractClientIp(request);
        if (!rateLimiter.allowAttempt(clientIp)) {
            log.warn("IP rate limit exceeded for verification attempt from: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "ok", false,
                    "detail", "Too many verification attempts. Please try again later.",
                    "status", "RATE_LIMITED"
            ));
        }

        // Verify the code
        EmailVerificationStatus status = service.verifyCode(email, code);
        boolean ok = status == EmailVerificationStatus.VERIFIED
                || status == EmailVerificationStatus.ALREADY_VERIFIED;

        if (ok) {
            return ResponseEntity.ok(Map.of("ok", true));
        }

        // Handle different error statuses
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("ok", false);
        errorBody.put("status", status.name());

        return switch (status) {
            case TOO_MANY_ATTEMPTS -> {
                errorBody.put("detail", "Too many incorrect attempts. Please request a new code.");
                yield ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody);
            }
            case EXPIRED -> {
                errorBody.put("detail", "Verification code has expired. Please request a new code.");
                yield ResponseEntity.status(HttpStatus.GONE).body(errorBody);
            }
            case NOT_FOUND -> {
                errorBody.put("detail", "No verification code found. Please register or request a new code.");
                yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody);
            }
            default -> {
                errorBody.put("detail", "Invalid verification code. Please try again.");
                yield ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
            }
        };
    }

    @PostMapping("/api/auth/resend-verification-code")
    public ResponseEntity<Map<String, Object>> resendVerificationCode(@Valid @RequestBody ResendVerificationRequest request) {
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

    /**
     * Extracts client IP address from request, handling X-Forwarded-For header.
     *
     * @param request HTTP servlet request
     * @return client IP address
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP in the chain
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}
