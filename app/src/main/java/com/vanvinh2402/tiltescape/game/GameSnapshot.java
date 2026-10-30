package com.vanvinh2402.tiltescape.game;

import java.util.List;

public final class GameSnapshot {
    private final GameState state;
    private final WorldRect playerBounds;
    private final List<WorldRect> obstacleBounds;
    private final float playerVelocityX;
    private final float survivalSeconds;
    private final int score;

    GameSnapshot(GameState state, WorldRect playerBounds, List<WorldRect> obstacleBounds,
            float playerVelocityX, float survivalSeconds, int score) {
        this.state = state;
        this.playerBounds = playerBounds;
        this.obstacleBounds = obstacleBounds;
        this.playerVelocityX = playerVelocityX;
        this.survivalSeconds = survivalSeconds;
        this.score = score;
    }

    public GameState getState() { return state; }
    public WorldRect getPlayerBounds() { return playerBounds; }
    public List<WorldRect> getObstacleBounds() { return obstacleBounds; }
    public float getPlayerVelocityX() { return playerVelocityX; }
    public float getSurvivalSeconds() { return survivalSeconds; }
    public int getScore() { return score; }
}
