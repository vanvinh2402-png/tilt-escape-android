package com.vanvinh2402.tiltescape.input;

public enum TiltSensitivity {
    LOW(0.75f),
    MEDIUM(1.0f),
    HIGH(1.25f);

    private final float multiplier;

    TiltSensitivity(float multiplier) {
        this.multiplier = multiplier;
    }

    public float getMultiplier() {
        return multiplier;
    }

    public static TiltSensitivity fromStorageValue(String value) {
        if (value == null) return MEDIUM;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return MEDIUM;
        }
    }
}
