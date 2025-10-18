package com.easyshop.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@Getter
@With
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OtpState {

    private String code;
    private Integer attempts;
    private Instant otpExpiresAt;
    private Instant cooldownUntil;
    private String resetToken;
    private Instant resetTokenExpiresAt;

    public static OtpState empty() {
        return new OtpState();
    }

    public OtpState startOtp(String code, Instant now, Duration ttl, Duration cooldown) {
        return this.withCode(code)
                .withAttempts(0)
                .withOtpExpiresAt(now.plus(ttl))
                .withCooldownUntil(now.plus(cooldown))
                .withResetToken(null)
                .withResetTokenExpiresAt(null);
    }

    public OtpState incrementAttempts() {
        int current = attempts != null ? attempts : 0;
        return this.withAttempts(current + 1);
    }

    public OtpState clearOtp() {
        return this.withCode(null)
                .withAttempts(0)
                .withOtpExpiresAt(null)
                .withCooldownUntil(null);
    }

    public OtpState issueResetToken(String token, Instant expiresAt) {
        return this.withResetToken(token)
                .withResetTokenExpiresAt(expiresAt);
    }

    public boolean hasOtp() {
        return code != null;
    }

    public boolean isOtpExpired(Instant now) {
        return otpExpiresAt != null && !otpExpiresAt.isAfter(now);
    }

    public boolean inCooldown(Instant now) {
        return cooldownUntil != null && cooldownUntil.isAfter(now);
    }

    public boolean hasResetToken() {
        return resetToken != null && resetTokenExpiresAt != null;
    }

    public boolean isResetTokenExpired(Instant now) {
        return resetTokenExpiresAt != null && !resetTokenExpiresAt.isAfter(now);
    }

    public long ttlSeconds(Instant now) {
        Instant latest = latestExpiry();
        if (latest == null) {
            return 0;
        }
        long seconds = latest.getEpochSecond() - now.getEpochSecond();
        return Math.max(seconds, 0);
    }

    public boolean isEmpty() {
        return code == null && (resetToken == null || resetToken.isBlank());
    }

    private Instant latestExpiry() {
        Instant candidate = otpExpiresAt;
        if (cooldownUntil != null && (candidate == null || cooldownUntil.isAfter(candidate))) {
            candidate = cooldownUntil;
        }
        if (resetTokenExpiresAt != null && (candidate == null || resetTokenExpiresAt.isAfter(candidate))) {
            candidate = resetTokenExpiresAt;
        }
        return candidate;
    }
}
