package com.vanvinh2402.tiltescape.auth;

public final class PasswordValidation {
    public enum Error {
        NONE,
        REQUIRED,
        TOO_SHORT,
        CONFIRMATION_MISMATCH
    }

    private final Error error;

    private PasswordValidation(Error error) {
        this.error = error;
    }

    static PasswordValidation valid() {
        return new PasswordValidation(Error.NONE);
    }

    static PasswordValidation invalid(Error error) {
        return new PasswordValidation(error);
    }

    public boolean isValid() {
        return error == Error.NONE;
    }

    public Error getError() {
        return error;
    }
}
