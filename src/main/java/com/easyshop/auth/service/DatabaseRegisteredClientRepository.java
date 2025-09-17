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

import java.util.Optional;
import java.util.Set;

@Service
public class DatabaseRegisteredClientRepository implements RegisteredClientRepository {

    private static final TypeReference<Set<String>> STRING_SET_TYPE = new TypeReference<>() {};

    private final OAuth2ClientRepository clientRepository;
    private final ObjectMapper objectMapper;

    public DatabaseRegisteredClientRepository(OAuth2ClientRepository clientRepository, ObjectMapper objectMapper) {
        this.clientRepository = clientRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("Use OAuth2ClientInitializer to manage clients");
    }

    @Override
    public RegisteredClient findById(String id) {
        if (id == null) {
            return null;
        }
        return parseLong(id)
                .flatMap(clientRepository::findById)
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
            RegisteredClient.Builder builder = RegisteredClient.withId(String.valueOf(client.getId()))
                    .clientId(client.getClientId());

            if (client.getClientSecret() != null) {
                builder.clientSecret(client.getClientSecret());
            }

            // Client authentication methods
            readStringSet(client.getClientAuthenticationMethods())
                    .forEach(method -> builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method)));

            // Authorization grant types
            readStringSet(client.getAuthorizationGrantTypes())
                    .forEach(grant -> builder.authorizationGrantType(new AuthorizationGrantType(grant)));

            if (client.getRedirectUris() != null && !client.getRedirectUris().isEmpty()) {
                readStringSet(client.getRedirectUris()).forEach(builder::redirectUri);
            }

            readStringSet(client.getScopes()).forEach(builder::scope);

            if (client.getClientSettings() != null && !client.getClientSettings().isEmpty()) {
                builder.clientSettings(objectMapper.readValue(client.getClientSettings(), ClientSettings.class));
            }

            if (client.getTokenSettings() != null && !client.getTokenSettings().isEmpty()) {
                builder.tokenSettings(objectMapper.readValue(client.getTokenSettings(), TokenSettings.class));
            }

            return builder.build();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to convert OAuth2Client to RegisteredClient", ex);
        }
    }

    private Set<String> readStringSet(String json) throws Exception {
        return objectMapper.readValue(json, STRING_SET_TYPE);
    }

    private Optional<Long> parseLong(String id) {
        try {
            return Optional.of(Long.parseLong(id));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
