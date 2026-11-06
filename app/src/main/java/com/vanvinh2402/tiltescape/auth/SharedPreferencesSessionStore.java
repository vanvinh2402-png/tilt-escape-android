package com.vanvinh2402.tiltescape.auth;

import android.content.Context;
import android.content.SharedPreferences;

public final class SharedPreferencesSessionStore implements SessionStore {
    private static final String PREFERENCES_NAME = "tilt_escape_session";
    private static final String ACTIVE_USER_ID = "active_user_id";

    private final SharedPreferences preferences;

    public SharedPreferencesSessionStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public Long getActiveUserId() {
        if (!preferences.contains(ACTIVE_USER_ID)) return null;
        try {
            long userId = preferences.getLong(ACTIVE_USER_ID, 0L);
            if (userId > 0L) return userId;
        } catch (ClassCastException ignored) {
            // Old or externally corrupted preference data must not block app startup.
        }
        clear();
        return null;
    }

    @Override
    public void saveActiveUserId(long userId) {
        if (userId <= 0L) {
            throw new IllegalArgumentException("userId must be positive");
        }
        preferences.edit().putLong(ACTIVE_USER_ID, userId).apply();
    }

    @Override
    public void clear() {
        preferences.edit().remove(ACTIVE_USER_ID).apply();
    }
}
