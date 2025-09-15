package com.easyshop.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth2_clients", schema = "auth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OAuth2Client {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "client_id", unique = true, nullable = false)
    private String clientId;
    
    @Column(name = "client_secret")
    private String clientSecret;
    
    @Column(name = "client_authentication_methods", nullable = false)
    private String clientAuthenticationMethods;
    
    @Column(name = "authorization_grant_types", nullable = false)
    private String authorizationGrantTypes;
    
    @Column(name = "redirect_uris")
    private String redirectUris;
    
    @Column(name = "scopes", nullable = false)
    private String scopes;
    
    @Column(name = "client_settings")
    private String clientSettings;
    
    @Column(name = "token_settings")
    private String tokenSettings;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
