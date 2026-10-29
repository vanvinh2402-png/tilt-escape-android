package com.vanvinh2402.tiltescape.game;

public final class GameSnapshot {
    private final GameState state;
    private final WorldRect playerBounds;
    private final WorldRect obstacleBounds;
    private final float playerVelocityX;

    GameSnapshot(
            GameState state,
            WorldRect playerBounds,
            WorldRect obstacleBounds,
            float playerVelocityX) {
        this.state = state;
        this.playerBounds = playerBounds;
        this.obstacleBounds = obstacleBounds;
        this.playerVelocityX = playerVelocityX;
    }

    public GameState getState() {
        return state;
    }

    public WorldRect getPlayerBounds() {
        return playerBounds;
    }

    public WorldRect getObstacleBounds() {
        return obstacleBounds;
    }

    public float getPlayerVelocityX() {
        return playerVelocityX;
    }
}
