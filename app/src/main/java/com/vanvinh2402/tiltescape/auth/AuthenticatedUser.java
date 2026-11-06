package com.vanvinh2402.tiltescape.auth;

import java.util.Objects;

public final class AuthenticatedUser {
    private final long id;
    private final String username;

    public AuthenticatedUser(long id, String username) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
