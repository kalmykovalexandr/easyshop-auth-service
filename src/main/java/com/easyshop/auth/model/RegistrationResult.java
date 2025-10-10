package com.easyshop.auth.model;

public record RegistrationResult(boolean success, boolean emailInUse, boolean weakPassword) {

    public static RegistrationResult successful() {
        return new RegistrationResult(true, false, false);
    }

    public static RegistrationResult failure(boolean emailInUse, boolean weakPassword) {
        return new RegistrationResult(false, emailInUse, weakPassword);
    }

    public boolean hasErrors() {
        return !success;
    }
}
