package com.easyshop.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

public class AccountStatusAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final String DISABLED_URL = "/login?error=disabled";
    private static final String DEFAULT_URL = "/login?error=credentials";

    public AccountStatusAuthenticationFailureHandler() {
        super(DEFAULT_URL);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {
        if (exception instanceof DisabledException) {
            super.setDefaultFailureUrl(DISABLED_URL);
        } else {
            super.setDefaultFailureUrl(DEFAULT_URL);
        }
        super.onAuthenticationFailure(request, response, exception);
    }
}