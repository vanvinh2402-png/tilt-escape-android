package com.vanvinh2402.tiltescape.auth;

public final class AuthOutcome {
    public enum Status {
        REGISTERED,
        AUTHENTICATED,
        NO_SESSION,
        DUPLICATE_USERNAME,
        INVALID_CREDENTIALS,
        ERROR
    }

    private final Status status;
    private final AuthenticatedUser user;

    private AuthOutcome(Status status, AuthenticatedUser user) {
        this.status = status;
        this.user = user;
    }

    public static AuthOutcome of(Status status) {
        return new AuthOutcome(status, null);
    }

    public static AuthOutcome authenticated(AuthenticatedUser user) {
        return new AuthOutcome(Status.AUTHENTICATED, user);
    }

    public Status getStatus() {
        return status;
    }

    public AuthenticatedUser getUser() {
        return user;
    }
}
