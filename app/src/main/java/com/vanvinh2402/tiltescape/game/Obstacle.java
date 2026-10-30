package com.vanvinh2402.tiltescape.game;

final class Obstacle {
    static final float WIDTH = 0.16f;
    static final float HEIGHT = 0.08f;

    private final float initialCenterX;
    private final float initialCenterY;
    private final float legacyVerticalSpeed;
    private float centerX;
    private float centerY;

    Obstacle(float centerX, float centerY) {
        this(centerX, centerY, 0.0f);
    }

    Obstacle(float initialCenterX, float initialCenterY, float verticalSpeed) {
        this.initialCenterX = initialCenterX;
        this.initialCenterY = initialCenterY;
        this.legacyVerticalSpeed = verticalSpeed;
        reset();
    }

    void reset() {
        centerX = initialCenterX;
        centerY = initialCenterY;
    }

    void update(float deltaSeconds) {
        update(deltaSeconds, legacyVerticalSpeed);
        if (isBelowWorld()) {
            centerX = initialCenterX;
            centerY = -HEIGHT / 2.0f;
        }
    }

    void update(float deltaSeconds, float verticalSpeed) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds <= 0.0f) {
            return;
        }
        centerY += verticalSpeed * deltaSeconds;
    }

    boolean isBelowWorld() {
        return getBounds().getTop() >= 1.0f;
    }

    WorldRect getBounds() {
        return new WorldRect(
                centerX - WIDTH / 2.0f,
                centerY - HEIGHT / 2.0f,
                centerX + WIDTH / 2.0f,
                centerY + HEIGHT / 2.0f);
    }
}
