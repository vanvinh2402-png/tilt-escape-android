package com.vanvinh2402.tiltescape.input;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Objects;

public final class TiltSettingsStore {
    private static final String PREFERENCES_NAME = "tilt_escape_settings";
    private static final String SENSITIVITY_KEY = "tilt_sensitivity";
    private final SharedPreferences preferences;

    public TiltSettingsStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public TiltSensitivity getSensitivity() {
        try {
            return TiltSensitivity.fromStorageValue(preferences.getString(SENSITIVITY_KEY, null));
        } catch (ClassCastException ignored) {
            return TiltSensitivity.MEDIUM;
        }
    }

    public void saveSensitivity(TiltSensitivity sensitivity) {
        preferences.edit().putString(SENSITIVITY_KEY,
                Objects.requireNonNull(sensitivity, "sensitivity").name()).apply();
    }
}
