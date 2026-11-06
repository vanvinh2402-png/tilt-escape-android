package com.vanvinh2402.tiltescape.auth;

import java.util.Objects;

public final class UsernameValidation {
    public enum Error {
        NONE,
        REQUIRED,
        INVALID_LENGTH,
        INVALID_CHARACTERS
    }

    private final Error error;
    private final String displayUsername;
    private final String normalizedUsername;

    private UsernameValidation(Error error, String displayUsername, String normalizedUsername) {
        this.error = Objects.requireNonNull(error, "error");
        this.displayUsername = displayUsername;
        this.normalizedUsername = normalizedUsername;
    }

    static UsernameValidation valid(String displayUsername, String normalizedUsername) {
        return new UsernameValidation(Error.NONE, displayUsername, normalizedUsername);
    }

    static UsernameValidation invalid(Error error) {
        return new UsernameValidation(error, null, null);
    }

    public Error getError() {
        return error;
    }

    public String getDisplayUsername() {
        return displayUsername;
    }

    public String getNormalizedUsername() {
        return normalizedUsername;
    }

    public boolean isValid() {
        return error == Error.NONE;
    }
}
