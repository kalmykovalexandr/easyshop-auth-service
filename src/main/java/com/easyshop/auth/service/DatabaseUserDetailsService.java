package com.easyshop.auth.service;

import com.easyshop.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UserDetailsService} implementation that loads users from the database using {@link UserRepository}.
 * The user's email is used as the unique identifier for authentication.
 */
@Service
@Transactional(readOnly = true)
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public DatabaseUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByEmail(username.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}

