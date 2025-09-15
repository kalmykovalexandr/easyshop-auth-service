package com.easyshop.auth.service;

import com.easyshop.auth.entity.OAuth2Client;
import com.easyshop.auth.repository.OAuth2ClientRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
public class DatabaseRegisteredClientRepository implements RegisteredClientRepository {
    
    private final OAuth2ClientRepository clientRepository;
    private final ObjectMapper objectMapper;
    
    public DatabaseRegisteredClientRepository(OAuth2ClientRepository clientRepository, ObjectMapper objectMapper) {
        this.clientRepository = clientRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void save(RegisteredClient registeredClient) {
        // Not implemented - clients are managed through OAuth2ClientInitializer
        throw new UnsupportedOperationException("Use OAuth2ClientInitializer to manage clients");
    }
    
    @Override
    public RegisteredClient findById(String id) {
        return clientRepository.findById(Long.valueOf(id))
                .map(this::convertToRegisteredClient)
                .orElse(null);
    }
    
    @Override
    public RegisteredClient findByClientId(String clientId) {
        return clientRepository.findByClientId(clientId)
                .map(this::convertToRegisteredClient)
                .orElse(null);
    }
    
    private RegisteredClient convertToRegisteredClient(OAuth2Client client) {
        try {
            RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(client.getClientId())
                    .clientSecret(client.getClientSecret());
            
            // Parse client authentication methods
            Set<String> authMethods = objectMapper.readValue(client.getClientAuthenticationMethods(), new TypeReference<Set<String>>() {});
            authMethods.forEach(method -> builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method)));
            
            // Parse authorization grant types
            Set<String> grantTypes = objectMapper.readValue(client.getAuthorizationGrantTypes(), new TypeReference<Set<String>>() {});
            grantTypes.forEach(grantType -> builder.authorizationGrantType(new AuthorizationGrantType(grantType)));
            
            // Parse redirect URIs
            if (client.getRedirectUris() != null && !client.getRedirectUris().isEmpty()) {
                Set<String> redirectUris = objectMapper.readValue(client.getRedirectUris(), new TypeReference<Set<String>>() {});
                redirectUris.forEach(builder::redirectUri);
            }
            
            // Parse scopes
            Set<String> scopes = objectMapper.readValue(client.getScopes(), new TypeReference<Set<String>>() {});
            scopes.forEach(builder::scope);
            
            // Parse client settings
            if (client.getClientSettings() != null && !client.getClientSettings().isEmpty()) {
                ClientSettings clientSettings = objectMapper.readValue(client.getClientSettings(), ClientSettings.class);
                builder.clientSettings(clientSettings);
            }
            
            // Parse token settings
            if (client.getTokenSettings() != null && !client.getTokenSettings().isEmpty()) {
                TokenSettings tokenSettings = objectMapper.readValue(client.getTokenSettings(), TokenSettings.class);
                builder.tokenSettings(tokenSettings);
            }
            
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert OAuth2Client to RegisteredClient", e);
        }
    }
}
