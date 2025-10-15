package com.easyshop.auth.repository;

import com.easyshop.auth.entity.EmailVerificationCode;
import com.easyshop.auth.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {
    Optional<EmailVerificationCode> findFirstByUserOrderByCreatedAtDesc(User user);
    void deleteByUser(User user);
    void deleteByExpiresAtBefore(LocalDateTime threshold);
}


