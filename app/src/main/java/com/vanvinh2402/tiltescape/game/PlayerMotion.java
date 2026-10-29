package com.vanvinh2402.tiltescape.game;

public final class PlayerMotion {
    private static final float WIDTH = 0.12f;
    private static final float HEIGHT = 0.08f;
    private static final float INITIAL_CENTER_X = 0.50f;
    private static final float INITIAL_CENTER_Y = 0.86f;
    private static final float MAX_HORIZONTAL_SPEED = 1.0f;
    private static final float ACCELERATION = 4.0f;
    private static final float BRAKING = 6.0f;

    private float centerX;
    private float velocityX;

    public PlayerMotion() {
        reset();
    }

    public void reset() {
        centerX = INITIAL_CENTER_X;
        velocityX = 0.0f;
    }

    public void update(float input, float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds <= 0.0f) {
            return;
        }

        float safeInput = Float.isFinite(input) ? clamp(input, -1.0f, 1.0f) : 0.0f;
        float targetVelocity = safeInput * MAX_HORIZONTAL_SPEED;
        float rate = safeInput == 0.0f ? BRAKING : ACCELERATION;
        velocityX = moveTowards(velocityX, targetVelocity, rate * deltaSeconds);
        centerX += velocityX * deltaSeconds;

        float halfWidth = WIDTH / 2.0f;
        float minimumCenter = halfWidth;
        float maximumCenter = 1.0f - halfWidth;
        if (centerX < minimumCenter) {
            centerX = minimumCenter;
            if (velocityX < 0.0f) {
                velocityX = 0.0f;
            }
        } else if (centerX > maximumCenter) {
            centerX = maximumCenter;
            if (velocityX > 0.0f) {
                velocityX = 0.0f;
            }
        }
    }

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return INITIAL_CENTER_Y;
    }

    public float getVelocityX() {
        return velocityX;
    }

    public WorldRect getBounds() {
        float halfWidth = WIDTH / 2.0f;
        float halfHeight = HEIGHT / 2.0f;
        return new WorldRect(
                centerX - halfWidth,
                INITIAL_CENTER_Y - halfHeight,
                centerX + halfWidth,
                INITIAL_CENTER_Y + halfHeight);
    }

    private static float moveTowards(float current, float target, float maximumDelta) {
        float difference = target - current;
        if (Math.abs(difference) <= maximumDelta) {
            return target;
        }
        return current + Math.copySign(maximumDelta, difference);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
