package com.vanvinh2402.tiltescape.auth;

public interface SessionStore {
    Long getActiveUserId();

    void saveActiveUserId(long userId);

    void clear();
}
