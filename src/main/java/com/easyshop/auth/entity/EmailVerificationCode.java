package com.easyshop.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an email verification code for user registration.
 * Codes are hashed using BCrypt for secure storage.
 */
@Entity
@Table(name = "email_verification_code", schema = "auth")
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "code_hash", length = 60, nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    public EmailVerificationCode() {
    }

    public EmailVerificationCode(User user, String codeHash, LocalDateTime expiresAt) {
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.attempts = 0;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    /**
     * Increments the attempt counter.
     */
    public void incrementAttempts() {
        this.attempts++;
    }

    /**
     * Checks if the code has expired.
     *
     * @param now current time
     * @return true if expired
     */
    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    /**
     * Checks if maximum attempts have been exceeded.
     *
     * @param maxAttempts maximum allowed attempts
     * @return true if attempts exceeded
     */
    public boolean hasExceededAttempts(int maxAttempts) {
        return attempts >= maxAttempts;
    }
}


