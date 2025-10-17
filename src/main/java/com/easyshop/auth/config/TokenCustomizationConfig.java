package com.easyshop.auth.config;

import com.easyshop.auth.model.entity.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.List;

@Configuration
public class TokenCustomizationConfig {

    private static final String ACCESS_TOKEN_VALUE = "access_token";
    private static final String ID_TOKEN_VALUE = "id_token";

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> oauth2TokenCustomizer() {
        return context -> {
            Authentication authentication = context.getPrincipal();
            if (authentication == null) {
                return;
            }

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof User user)) {
                return;
            }

            List<String> roles = user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            String tokenValue = context.getTokenType().getValue();

            if (ACCESS_TOKEN_VALUE.equals(tokenValue)) {
                context.getClaims()
                        .claim("roles", roles)
                        .claim("email", user.getEmail())
                        .claim("preferred_username", user.getUsername());
            }

            if (ID_TOKEN_VALUE.equals(tokenValue)) {
                context.getClaims()
                        .claim("email", user.getEmail())
                        .claim("preferred_username", user.getUsername())
                        .claim("roles", roles);
            }
        };
    }
}
