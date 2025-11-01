package com.easyshop.auth.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

/**
 * Custom DaoAuthenticationProvider that verifies credentials before reporting account status failures.
 * <p>
 * Spring Security's default provider checks the account status (disabled/locked) before
 * validating the password. That means a disabled user always receives a {@link DisabledException},
 * even when the submitted password is wrong. Our UX requires that an incorrect password still
 * ends with a generic credentials error. This provider defers the final decision: if a disabled
 * exception is raised, we re-evaluate the password and convert the failure to {@link BadCredentialsException}
 * when the password does not match.
 */
public class DeferredAccountStatusDaoAuthenticationProvider extends DaoAuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try {
            return super.authenticate(authentication);
        } catch (DisabledException disabled) {
            if (authentication instanceof UsernamePasswordAuthenticationToken token) {
                String presentedPassword = token.getCredentials() != null ? token.getCredentials().toString() : null;
                if (!StringUtils.hasText(presentedPassword)) {
                    throw badCredentials(disabled);
                }

                UserDetails user = retrieveUser(token.getName(), token);
                if (!getPasswordEncoder().matches(presentedPassword, user.getPassword())) {
                    throw badCredentials(disabled);
                }
            }
            throw disabled;
        }
    }

    private BadCredentialsException badCredentials(Exception cause) {
        return new BadCredentialsException(
                this.messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"),
                cause
        );
    }
}
