package com.easyshop.auth.repository;

import com.easyshop.auth.entity.EmailVerificationToken;
import com.easyshop.auth.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findFirstByUserOrderByCreatedAtDesc(User user);

    Optional<EmailVerificationToken> findByToken(String token);

    void deleteByUser(User user);

    void deleteByExpiresAtBefore(LocalDateTime threshold);
}
