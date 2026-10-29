package com.vanvinh2402.tiltescape.input;

public final class TiltSignalProcessor implements TiltInput {
    private static final float GRAVITY = 9.80665f;
    private static final float DEAD_ZONE_G = 0.08f;
    private static final float FULL_TILT_G = 0.60f;
    private static final float FILTER_TIME_CONSTANT_SECONDS = 0.10f;

    private boolean initialized;
    private long lastTimestampNanos;
    private float filteredTiltG;
    private float horizontalInput;

    public void onSample(float rawX, long timestampNanos) {
        if (!Float.isFinite(rawX) || timestampNanos <= 0L) {
            return;
        }

        float normalizedTiltG = -rawX / GRAVITY;
        if (!initialized) {
            initialized = true;
            lastTimestampNanos = timestampNanos;
            filteredTiltG = normalizedTiltG;
            horizontalInput = applyDeadZone(filteredTiltG);
            return;
        }

        if (timestampNanos <= lastTimestampNanos) {
            return;
        }

        float deltaSeconds = (timestampNanos - lastTimestampNanos) / 1_000_000_000.0f;
        float alpha = 1.0f - (float) Math.exp(-deltaSeconds / FILTER_TIME_CONSTANT_SECONDS);
        filteredTiltG += alpha * (normalizedTiltG - filteredTiltG);
        lastTimestampNanos = timestampNanos;
        horizontalInput = applyDeadZone(filteredTiltG);
    }

    public void reset() {
        initialized = false;
        lastTimestampNanos = 0L;
        filteredTiltG = 0.0f;
        horizontalInput = 0.0f;
    }

    @Override
    public float getHorizontalInput() {
        return horizontalInput;
    }

    private static float applyDeadZone(float tiltG) {
        float magnitude = Math.abs(tiltG);
        if (magnitude <= DEAD_ZONE_G) {
            return 0.0f;
        }
        float scaled = (magnitude - DEAD_ZONE_G) / (FULL_TILT_G - DEAD_ZONE_G);
        return Math.copySign(Math.min(1.0f, scaled), tiltG);
    }
}
