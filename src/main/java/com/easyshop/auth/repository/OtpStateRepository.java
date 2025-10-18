package com.easyshop.auth.repository;

import com.easyshop.auth.exception.BusinessException;
import com.easyshop.auth.exception.ErrorCode;
import com.easyshop.auth.model.OtpState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OtpStateRepository {

    private final RedisTemplate<String, String> redis;
    private final ObjectMapper objectMapper;

    OtpStateRepository(RedisTemplate<String, String> redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public Optional<OtpState> load(String email) {
        String raw = redis.opsForValue().get(otpKey(email));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(objectMapper.readValue(raw, OtpState.class));
        } catch (MismatchedInputException ex) {
            delete(email);
            return Optional.empty();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize OTP state", ex);
        }
    }

    public OtpState findOrThrow(String email, ErrorCode notFoundError) {
        return load(email).orElseThrow(() -> new BusinessException(notFoundError));
    }

    public void save(String email, OtpState state, Instant now) {
        long ttlSeconds = state.ttlSeconds(now);
        if (ttlSeconds <= 0 || state.isEmpty()) {
            delete(email);
            return;
        }
        try {
            redis.opsForValue().set(
                    otpKey(email),
                    objectMapper.writeValueAsString(state),
                    Duration.ofSeconds(ttlSeconds)
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize OTP state", ex);
        }
    }

    public void delete(String email) {
        try {
            redis.delete(otpKey(email));
        } catch (DataAccessException ignored) {
        }
    }

    private String otpKey(String email) {
        return "otp:%s".formatted(email);
    }
}
