package com.vanvinh2402.tiltescape.game;

public final class FrameClock {
    private static final float NANOS_PER_SECOND = 1_000_000_000.0f;
    private static final float MAX_DELTA_SECONDS = 0.05f;
    private long previousFrameTimeNanos;
    private boolean initialized;

    public float nextDeltaSeconds(long frameTimeNanos) {
        if (!initialized) {
            previousFrameTimeNanos = frameTimeNanos;
            initialized = true;
            return 0.0f;
        }
        if (frameTimeNanos <= previousFrameTimeNanos) {
            return 0.0f;
        }
        long elapsedNanos = frameTimeNanos - previousFrameTimeNanos;
        previousFrameTimeNanos = frameTimeNanos;
        return Math.min(elapsedNanos / NANOS_PER_SECOND, MAX_DELTA_SECONDS);
    }

    public void reset() {
        previousFrameTimeNanos = 0L;
        initialized = false;
    }
}
