package com.vanvinh2402.tiltescape.auth;

import java.util.Objects;

public final class AccountRecord {
    private final long id;
    private final String username;
    private final String normalizedUsername;
    private final PasswordHash passwordHash;
    private final long createdAtEpochMillis;

    public AccountRecord(
            long id,
            String username,
            String normalizedUsername,
            PasswordHash passwordHash,
            long createdAtEpochMillis) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
        this.normalizedUsername = Objects.requireNonNull(
                normalizedUsername, "normalizedUsername");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.createdAtEpochMillis = createdAtEpochMillis;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNormalizedUsername() {
        return normalizedUsername;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public long getCreatedAtEpochMillis() {
        return createdAtEpochMillis;
    }
}
