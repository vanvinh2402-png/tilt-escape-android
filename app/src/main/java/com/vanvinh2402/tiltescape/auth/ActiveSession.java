package com.vanvinh2402.tiltescape.auth;

import java.util.Objects;

public final class ActiveSession {
    private AuthenticatedUser user;

    public boolean isAuthenticated() {
        return user != null;
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public long requireUserId() {
        if (user == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return user.getId();
    }

    public void authenticate(AuthenticatedUser user) {
        this.user = Objects.requireNonNull(user, "user");
    }

    public void clear() {
        user = null;
    }
}
