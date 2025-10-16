package com.easyshop.auth.validation;

import com.easyshop.auth.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Validator for @UniqueEmail annotation.
 * Checks if the email already exists in the database.
 */
@Slf4j
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userRepository;

    public UniqueEmailValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            // Let @NotBlank handle empty validation
            return true;
        }

        // Email is already normalized in AuthDto constructor
        boolean exists = userRepository.existsByEmail(email);

        if (exists) {
            log.debug("Email validation failed: {} already exists", email);
        }

        return !exists;
    }
}
