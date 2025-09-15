package com.easyshop.auth.repository;

import com.easyshop.auth.entity.OAuth2Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuth2ClientRepository extends JpaRepository<OAuth2Client, Long> {
    Optional<OAuth2Client> findByClientId(String clientId);
    boolean existsByClientId(String clientId);
}
