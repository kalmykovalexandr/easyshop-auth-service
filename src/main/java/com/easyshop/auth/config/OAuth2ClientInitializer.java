package com.easyshop.auth.config;

import com.easyshop.auth.model.entity.OAuth2Client;
import com.easyshop.auth.repository.OAuth2ClientRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OAuth2ClientInitializer implements CommandLineRunner {

    private static final TypeReference<LinkedHashSet<String>> STRING_SET_TYPE = new TypeReference<>() {
    };

    private final OAuth2ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${WEBAPP_REDIRECT_URI:http://localhost:5173/auth/callback}")
    private String webappRedirectUri;

    @Value("${WEBAPP_POST_LOGOUT_REDIRECT_URI:http://localhost:5173}")
    private String webappPostLogoutRedirectUri;

    @Value("${GATEWAY_CLIENT_SECRET:gateway-secret}")
    private String gatewayClientSecret;

    @Value("${PRODUCT_SERVICE_CLIENT_SECRET:product-service-secret}")
    private String productServiceClientSecret;

    @Value("${PURCHASE_SERVICE_CLIENT_SECRET:purchase-service-secret}")
    private String purchaseServiceClientSecret;

    public OAuth2ClientInitializer(OAuth2ClientRepository clientRepository,
                                   PasswordEncoder passwordEncoder,
                                   ObjectMapper objectMapper) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        initializeDefaultClients();
    }

    private void initializeDefaultClients() {
        ensureWebappClient();
        ensureGatewayClient();
        ensureProductServiceClient();
        ensurePurchaseServiceClient();
    }

    private void ensureWebappClient() {
        Set<String> redirectUris = uniqueNonBlank(webappRedirectUri, webappPostLogoutRedirectUri);
        String redirectUrisJson = toJson(redirectUris);

        Optional<OAuth2Client> existingClient = clientRepository.findByClientId("webapp");

        if (existingClient.isPresent()) {
            OAuth2Client client = existingClient.get();
            if (!stringCollectionEquals(client.getRedirectUris(), redirectUris)) {
                client.setRedirectUris(redirectUrisJson);
                clientRepository.save(client);
            }
            return;
        }

        OAuth2Client webappClient = OAuth2Client.builder()
                .clientId("webapp")
                .clientSecret(null)
                .clientAuthenticationMethods(toJson(Set.of(ClientAuthenticationMethod.NONE.getValue())))
                .authorizationGrantTypes(toJson(Set.of(
                        AuthorizationGrantType.AUTHORIZATION_CODE.getValue(),
                        AuthorizationGrantType.REFRESH_TOKEN.getValue()
                )))
                .redirectUris(redirectUrisJson)
                .scopes(toJson(Set.of("openid", "profile", "read", "write")))
                .clientSettings(toJson(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(true)
                        .build().getSettings()))
                .tokenSettings(toJson(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .idTokenSignatureAlgorithm(SignatureAlgorithm.RS256)
                        .reuseRefreshTokens(true)
                        .build().getSettings()))
                .build();
        clientRepository.save(webappClient);
    }

    private void ensureGatewayClient() {
        if (clientRepository.existsByClientId("gateway")) {
            return;
        }
        OAuth2Client gatewayClient = OAuth2Client.builder()
                .clientId("gateway")
                .clientSecret(passwordEncoder.encode(gatewayClientSecret))
                .clientAuthenticationMethods(toJson(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue())))
                .authorizationGrantTypes(toJson(Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())))
                .redirectUris(toJson(Collections.emptySet()))
                .scopes(toJson(Set.of("read", "write")))
                .clientSettings(toJson(ClientSettings.builder().build().getSettings()))
                .tokenSettings(toJson(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .build().getSettings()))
                .build();
        clientRepository.save(gatewayClient);
    }

    private void ensureProductServiceClient() {
        if (clientRepository.existsByClientId("product-service")) {
            return;
        }
        OAuth2Client productServiceClient = OAuth2Client.builder()
                .clientId("product-service")
                .clientSecret(passwordEncoder.encode(productServiceClientSecret))
                .clientAuthenticationMethods(toJson(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue())))
                .authorizationGrantTypes(toJson(Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())))
                .redirectUris(toJson(Collections.emptySet()))
                .scopes(toJson(Set.of("read", "write")))
                .clientSettings(toJson(ClientSettings.builder().build().getSettings()))
                .tokenSettings(toJson(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .build().getSettings()))
                .build();
        clientRepository.save(productServiceClient);
    }

    private void ensurePurchaseServiceClient() {
        if (clientRepository.existsByClientId("purchase-service")) {
            return;
        }
        OAuth2Client purchaseServiceClient = OAuth2Client.builder()
                .clientId("purchase-service")
                .clientSecret(passwordEncoder.encode(purchaseServiceClientSecret))
                .clientAuthenticationMethods(toJson(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue())))
                .authorizationGrantTypes(toJson(Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())))
                .redirectUris(toJson(Collections.emptySet()))
                .scopes(toJson(Set.of("read", "write")))
                .clientSettings(toJson(ClientSettings.builder().build().getSettings()))
                .tokenSettings(toJson(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .build().getSettings()))
                .build();
        clientRepository.save(purchaseServiceClient);
    }

    private Set<String> uniqueNonBlank(String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean stringCollectionEquals(String json, Set<String> expected) {
        return readStringSet(json).equals(expected);
    }

    private Set<String> readStringSet(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptySet();
        }
        try {
            return objectMapper.readValue(json, STRING_SET_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse OAuth2 client configuration", ex);
        }
    }

    private String toJson(Object value) {
        try {
            if (value instanceof ClientSettings clientSettings) {
                return objectMapper.writeValueAsString(clientSettings.getSettings());
            }
            if (value instanceof TokenSettings tokenSettings) {
                return objectMapper.writeValueAsString(tokenSettings.getSettings());
            }
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize OAuth2 client configuration", ex);
        }
    }
}
