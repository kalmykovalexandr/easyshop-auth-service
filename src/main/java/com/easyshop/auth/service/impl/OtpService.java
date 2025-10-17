package com.easyshop.auth.service.impl;

import com.easyshop.auth.exception.RateLimitExceededException;
import com.easyshop.auth.service.OtpServiceInt;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpService implements OtpServiceInt {

    private final EmailService authEmailService;
    private final RedisTemplate<String, String> redis;

    private final Duration ttl = Duration.ofMinutes(10);
    private final int maxAttempts = 5;
    private final Duration resendCooldown = Duration.ofSeconds(60);

    public OtpService(RedisTemplate<String, String> redis,
                      EmailService authEmailService) {
        this.redis = redis;
        this.authEmailService = authEmailService;
    }

    @Override
    public void generateOtp(String email) {
        // TODO ONLY IF OPERATION IS RESEND CODE DURING REGISTRATION, DELETE OLD CODE FROM REDIS

        // TODO IP-based rate limiting

        if (Boolean.TRUE.equals(redis.hasKey(kCooldown(email)))) {
            throw new RateLimitExceededException(maxAttempts);
        }

        SecureRandom rnd = new SecureRandom();
        int n = rnd.nextInt(90_000_000) + 10_000_000;
        String code = Integer.toString(n);
        redis.opsForValue().set(k(email), code, ttl);
        redis.opsForValue().set(kCooldown(email), "1", resendCooldown);
        redis.delete(kAttempts(email));

        // TODO send email via Kafka messages
        authEmailService.sendVerificationEmail(email, code);
    }

    @Override
    public boolean verifyOtp(String subject, String code) {
        Long tries = redis.opsForValue().increment(kAttempts(subject));
        if (tries == 1L) redis.expire(kAttempts(subject), ttl);
        if (tries != null && tries > maxAttempts) {
            throw new RateLimitExceededException(maxAttempts);
        }

        String expected = redis.opsForValue().get(k(subject));
        boolean ok = expected != null && expected.equals(code);

        if (ok) {
            redis.delete(k(subject));
            redis.delete(kAttempts(subject));
        }
        return ok;
    }

    private String k(String subject) {
        return "otp:" + subject;
    }

    private String kAttempts(String s) {
        return "otp:attempts:" + s;
    }

    private String kCooldown(String s) {
        return "otp:cooldown:" + s;
    }
}
