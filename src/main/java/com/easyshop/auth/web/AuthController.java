package com.easyshop.auth.web;

import com.easyshop.auth.model.EmailVerificationStatus;
import com.easyshop.auth.model.RegistrationResult;
import com.easyshop.auth.model.ResendVerificationResult;
import com.easyshop.auth.model.VerificationPurpose;
import com.easyshop.auth.security.VerificationRateLimiter;
import com.easyshop.auth.service.AuthService;
import com.easyshop.auth.web.dto.AuthDto;
import com.easyshop.auth.web.dto.ResendVerificationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final VerificationRateLimiter rateLimiter;

    public AuthController(AuthService authService, VerificationRateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> register(@Valid @RequestBody AuthDto dto) {
        authService.register(dto);
        String success = authService.getRegistrationSuccessMessage();
        return ResponseEntity.ok(success);
    }

    @GetMapping("/password-requirements")
    public ResponseEntity<String> getPasswordRequirements() {
        return ResponseEntity.ok(authService.getPasswordValidationMessage());
    }

    /**
     * Verifies OTP code for both email verification and password reset.
     * Includes IP-based rate limiting to prevent distributed brute-force attacks.
     *
     * For email verification: activates user account on success
     * For password reset: only verifies the code without activating account
     */
    @PostMapping(value = "/verify-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> verifyCode(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String email = body.getOrDefault("email", "");
        String code = body.getOrDefault("code", "");
        String purpose = body.getOrDefault("purpose", "registration"); // "registration" or "password_reset"

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

        // Verify the code with appropriate behavior based on purpose
        boolean enableUserOnSuccess = !"password_reset".equals(purpose);
        EmailVerificationStatus status = authService.verifyCode(email, code, enableUserOnSuccess);

        boolean ok = status == EmailVerificationStatus.VERIFIED
                || status == EmailVerificationStatus.ALREADY_VERIFIED;

        if (ok) {
            return ResponseEntity.ok(Map.of("ok", true, "status", "VERIFIED"));
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

    @PostMapping(value = "/resend-verification-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> resendVerificationCode(@Valid @RequestBody ResendVerificationRequest request) {
        ResendVerificationResult result = authService.resendVerification(request.email());
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
     * Initiates password reset by sending OTP code to user's email.
     * Uses unified sendVerificationCode method with PASSWORD_RESET purpose.
     * Always returns success to prevent email enumeration.
     */
    @PostMapping(value = "/forgot-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim();

        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "detail", "Email is required"
            ));
        }

        // Send password reset code (always returns success for security)
        ResendVerificationResult result = authService.sendVerificationCode(email, VerificationPurpose.PASSWORD_RESET);

        // Handle cooldown
        if (result.status() == ResendVerificationResult.Status.RATE_LIMITED) {
            Map<String, Object> body2 = new LinkedHashMap<>();
            body2.put("ok", false);
            body2.put("detail", result.message());
            if (result.retryAfterSeconds() != null) {
                body2.put("retryAfterSeconds", result.retryAfterSeconds());
            }
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
            if (result.retryAfterSeconds() != null) {
                builder = builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
            }
            return builder.body(body2);
        }

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", result.message()
        ));
    }

    /**
     * Completes password reset with new password.
     */
    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "");
        String newPassword = body.getOrDefault("password", "");
        String confirmPassword = body.getOrDefault("confirmPassword", "");

        if (email.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "detail", "All fields are required"
            ));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "detail", "Passwords do not match"
            ));
        }

        boolean success = authService.completePasswordReset(email, newPassword);

        if (!success) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "detail", "Could not reset password. Please ensure your password meets the requirements."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Password reset successfully"
        ));
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
