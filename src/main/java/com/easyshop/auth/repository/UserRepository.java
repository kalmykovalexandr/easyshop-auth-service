package com.easyshop.auth.repository;

import com.easyshop.auth.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    @Query("SELECT u.email FROM User u WHERE u.enabled = false AND u.createdAt < :cutoff")
    List<String> findEmailsOfUnverifiedOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM User u WHERE u.enabled = false AND u.createdAt < :cutoff")
    int deleteUnverifiedOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
