package com.vanvinh2402.tiltescape.game;

public final class GameEngine {
    private final PlayerMotion player;
    private final Obstacle obstacle;
    private GameState state;

    public GameEngine() {
        this(new Obstacle(0.25f, -0.04f, 0.35f));
    }

    GameEngine(Obstacle obstacle) {
        this.player = new PlayerMotion();
        this.obstacle = obstacle;
        this.state = GameState.READY;
    }

    public GameState getState() {
        return state;
    }

    public void startOrRestart() {
        if (state != GameState.READY && state != GameState.GAME_OVER) {
            return;
        }
        player.reset();
        obstacle.reset();
        state = GameState.PLAYING;
    }

    public void pause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        }
    }

    public void resume() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    public void update(float horizontalInput, float deltaSeconds) {
        if (state != GameState.PLAYING
                || !Float.isFinite(deltaSeconds)
                || deltaSeconds <= 0.0f) {
            return;
        }

        player.update(horizontalInput, deltaSeconds);
        obstacle.update(deltaSeconds);
        if (player.getBounds().overlaps(obstacle.getBounds())) {
            state = GameState.GAME_OVER;
        }
    }

    public GameSnapshot snapshot() {
        return new GameSnapshot(
                state,
                player.getBounds(),
                obstacle.getBounds(),
                player.getVelocityX());
    }
}
