package com.easyshop.auth.service;

import com.easyshop.auth.entity.OAuth2Client;
import com.easyshop.auth.repository.OAuth2ClientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class OAuth2ClientInitializer implements CommandLineRunner {
    
    private final OAuth2ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${WEBAPP_REDIRECT_URI:http://localhost:3000/callback}")
    private String webappRedirectUri;
    
    @Value("${WEBAPP_POST_LOGOUT_REDIRECT_URI:http://localhost:3000}")
    private String webappPostLogoutRedirectUri;
    
    @Value("${GATEWAY_CLIENT_SECRET:gateway-secret}")
    private String gatewayClientSecret;
    
    @Value("${PRODUCT_SERVICE_CLIENT_SECRET:product-service-secret}")
    private String productServiceClientSecret;
    
    @Value("${PURCHASE_SERVICE_CLIENT_SECRET:purchase-service-secret}")
    private String purchaseServiceClientSecret;
    
    public OAuth2ClientInitializer(OAuth2ClientRepository clientRepository, PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public void run(String... args) throws Exception {
        initializeDefaultClients();
    }
    
    private void initializeDefaultClients() {
        // Initialize webapp client
        if (!clientRepository.existsByClientId("webapp")) {
            OAuth2Client webappClient = OAuth2Client.builder()
                    .clientId("webapp")
                    .clientSecret(null) // PKCE client
                    .clientAuthenticationMethods("[\"NONE\"]")
                    .authorizationGrantTypes("[\"authorization_code\",\"refresh_token\"]")
                    .redirectUris("[\"" + webappRedirectUri + "\",\"" + webappPostLogoutRedirectUri + "\"]")
                    .scopes("[\"openid\",\"profile\",\"read\",\"write\"]")
                    .clientSettings("{\"requireAuthorizationConsent\":false,\"requireProofKey\":true}")
                    .tokenSettings("{\"accessTokenTimeToLive\":3600,\"refreshTokenTimeToLive\":604800,\"idTokenSignatureAlgorithm\":\"RS256\"}")
                    .build();
            clientRepository.save(webappClient);
        }
        
        // Initialize gateway client
        if (!clientRepository.existsByClientId("gateway")) {
            OAuth2Client gatewayClient = OAuth2Client.builder()
                    .clientId("gateway")
                    .clientSecret(passwordEncoder.encode(gatewayClientSecret))
                    .clientAuthenticationMethods("[\"client_secret_basic\"]")
                    .authorizationGrantTypes("[\"client_credentials\"]")
                    .redirectUris("[]")
                    .scopes("[\"read\",\"write\"]")
                    .clientSettings("{}")
                    .tokenSettings("{\"accessTokenTimeToLive\":3600}")
                    .build();
            clientRepository.save(gatewayClient);
        }
        
        // Initialize product-service client
        if (!clientRepository.existsByClientId("product-service")) {
            OAuth2Client productServiceClient = OAuth2Client.builder()
                    .clientId("product-service")
                    .clientSecret(passwordEncoder.encode(productServiceClientSecret))
                    .clientAuthenticationMethods("[\"client_secret_basic\"]")
                    .authorizationGrantTypes("[\"client_credentials\"]")
                    .redirectUris("[]")
                    .scopes("[\"read\",\"write\"]")
                    .clientSettings("{}")
                    .tokenSettings("{\"accessTokenTimeToLive\":3600}")
                    .build();
            clientRepository.save(productServiceClient);
        }
        
        // Initialize purchase-service client
        if (!clientRepository.existsByClientId("purchase-service")) {
            OAuth2Client purchaseServiceClient = OAuth2Client.builder()
                    .clientId("purchase-service")
                    .clientSecret(passwordEncoder.encode(purchaseServiceClientSecret))
                    .clientAuthenticationMethods("[\"client_secret_basic\"]")
                    .authorizationGrantTypes("[\"client_credentials\"]")
                    .redirectUris("[]")
                    .scopes("[\"read\",\"write\"]")
                    .clientSettings("{}")
                    .tokenSettings("{\"accessTokenTimeToLive\":3600}")
                    .build();
            clientRepository.save(purchaseServiceClient);
        }
    }
}
