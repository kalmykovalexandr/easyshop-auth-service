package com.easyshop.auth.service;

import com.easyshop.auth.entity.OAuth2Client;
import com.easyshop.auth.repository.OAuth2ClientRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class DatabaseRegisteredClientRepository implements RegisteredClientRepository {

    private static final TypeReference<Set<String>> STRING_SET_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private static final String ACCESS_TOKEN_TTL_KEY = "settings.token.access-token-time-to-live";
    private static final String REFRESH_TOKEN_TTL_KEY = "settings.token.refresh-token-time-to-live";
    private static final String AUTH_CODE_TTL_KEY = "settings.token.authorization-code-time-to-live";
    private static final String DEVICE_CODE_TTL_KEY = "settings.token.device-code-time-to-live";
    private static final String USER_CODE_TTL_KEY = "settings.token.user-code-time-to-live";
    private static final String ACCESS_TOKEN_FORMAT_KEY = "settings.token.access-token-format";
    private static final String ID_TOKEN_SIGNATURE_ALG_KEY = "settings.token.id-token-signature-algorithm";

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

            readStringSet(client.getClientAuthenticationMethods())
                    .forEach(method -> builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method)));

            readStringSet(client.getAuthorizationGrantTypes())
                    .forEach(grant -> builder.authorizationGrantType(new AuthorizationGrantType(grant)));

            if (client.getRedirectUris() != null && !client.getRedirectUris().isEmpty()) {
                readStringSet(client.getRedirectUris()).forEach(builder::redirectUri);
            }

            readStringSet(client.getScopes()).forEach(builder::scope);

            if (client.getClientSettings() != null && !client.getClientSettings().isEmpty()) {
                Map<String, Object> settings = objectMapper.readValue(client.getClientSettings(), MAP_TYPE);
                builder.clientSettings(ClientSettings.withSettings(settings).build());
            }

            if (client.getTokenSettings() != null && !client.getTokenSettings().isEmpty()) {
                Map<String, Object> settings = objectMapper.readValue(client.getTokenSettings(), MAP_TYPE);
                builder.tokenSettings(TokenSettings.withSettings(convertTokenSettings(settings)).build());
            }

            return builder.build();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to convert OAuth2Client to RegisteredClient", ex);
        }
    }

    private Map<String, Object> convertTokenSettings(Map<String, Object> settings) {
        Map<String, Object> mutable = new HashMap<>(settings);
        convertDuration(mutable, ACCESS_TOKEN_TTL_KEY);
        convertDuration(mutable, REFRESH_TOKEN_TTL_KEY);
        convertDuration(mutable, AUTH_CODE_TTL_KEY);
        convertDuration(mutable, DEVICE_CODE_TTL_KEY);
        convertDuration(mutable, USER_CODE_TTL_KEY);
        convertAccessTokenFormat(mutable, ACCESS_TOKEN_FORMAT_KEY);
        convertSignatureAlgorithm(mutable, ID_TOKEN_SIGNATURE_ALG_KEY);
        return mutable;
    }

    private void convertDuration(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof String strValue && !strValue.isBlank()) {
            map.put(key, Duration.parse(strValue));
        }
    }

    private void convertAccessTokenFormat(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map<?, ?> nested) {
            Object formatValue = nested.get("value");
            if (formatValue instanceof String strValue) {
                map.put(key, toTokenFormat(strValue));
            }
        } else if (value instanceof String strValue) {
            map.put(key, toTokenFormat(strValue));
        }
    }

    private OAuth2TokenFormat toTokenFormat(String value) {
        return switch (value == null ? "" : value.toLowerCase()) {
            case "reference" -> OAuth2TokenFormat.REFERENCE;
            default -> OAuth2TokenFormat.SELF_CONTAINED;
        };
    }

    private void convertSignatureAlgorithm(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof String strValue && !strValue.isBlank()) {
            map.put(key, SignatureAlgorithm.from(strValue));
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
