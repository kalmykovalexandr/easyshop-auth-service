package com.easyshop.auth.model.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/**
 * Unified error response format for all API errors.
 * Provides consistent structure for client-side error handling.
 *
 * Example response:
 * <pre>
 * {
 *   "ok": false,
 *   "detail": "Enter a valid e-mail address.",
 *   "errors": {
 *     "email": "Enter a valid e-mail address."
 *   },
 *   "errorCode": "INVALID_EMAIL",
 *   "status": "BAD_REQUEST",
 *   "statusCode": 400,
 *   "path": "/api/auth/register",
 *   "timestamp": "2025-10-16T19:30:45.123Z"
 * }
 * </pre>
 */
@Getter
@Setter
@Builder
public class ErrorResponse {

    /**
     * Always false for error responses.
     * Allows clients to check success/failure uniformly.
     */
    @Builder.Default
    private boolean ok = false;

    /**
     * Human-readable error message (localized).
     * Main error description for display to users.
     */
    private String detail;

    /**
     * Field-level validation errors (for form validation).
     * Map of field names to error messages.
     * Only present for validation errors.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> errors;

    /**
     * Machine-readable error code.
     * Allows clients to handle specific error scenarios programmatically.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;

    /**
     * HTTP status name (e.g., "BAD_REQUEST", "NOT_FOUND").
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String status;

    /**
     * HTTP status code (e.g., 400, 404, 500).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer statusCode;

    /**
     * Request path where error occurred.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String path;

    /**
     * Timestamp when error occurred.
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Additional retry information for rate-limited requests.
     * Number of seconds to wait before retrying.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long retryAfterSeconds;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant cooldownUntil;

    /**
     * Debug message with technical details.
     * Only included in development/staging environments.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String debugMessage;
}
