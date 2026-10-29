package com.vanvinh2402.tiltescape.game;

final class Obstacle {
    private static final float WIDTH = 0.16f;
    private static final float HEIGHT = 0.08f;

    private final float initialCenterX;
    private final float initialCenterY;
    private final float verticalSpeed;

    private float centerX;
    private float centerY;

    Obstacle(float initialCenterX, float initialCenterY, float verticalSpeed) {
        this.initialCenterX = initialCenterX;
        this.initialCenterY = initialCenterY;
        this.verticalSpeed = verticalSpeed;
        reset();
    }

    void reset() {
        centerX = initialCenterX;
        centerY = initialCenterY;
    }

    void update(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds <= 0.0f) {
            return;
        }
        centerY += verticalSpeed * deltaSeconds;
        if (getBounds().getTop() >= 1.0f) {
            centerX = initialCenterX;
            centerY = -HEIGHT / 2.0f;
        }
    }

    WorldRect getBounds() {
        float halfWidth = WIDTH / 2.0f;
        float halfHeight = HEIGHT / 2.0f;
        return new WorldRect(
                centerX - halfWidth,
                centerY - halfHeight,
                centerX + halfWidth,
                centerY + halfHeight);
    }
}
