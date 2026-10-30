package com.vanvinh2402.tiltescape.game;

public final class GameEngine {
    private static final float MAX_DELTA_SECONDS = 0.05f;
    private final PlayerMotion player = new PlayerMotion();
    private final ObstacleField obstacleField;
    private final DifficultyPolicy difficultyPolicy = new DifficultyPolicy();
    private GameState state = GameState.READY;
    private float survivalSeconds;

    public GameEngine() {
        this(new ObstacleField(new JavaRandomSource()));
    }

    GameEngine(ObstacleField obstacleField) {
        this.obstacleField = obstacleField;
        obstacleField.reset();
    }

    public GameState getState() { return state; }

    public void startOrRestart() {
        if (state == GameState.READY || state == GameState.GAME_OVER) {
            startNewRun();
        }
    }

    public void startNewRun() {
        player.reset();
        obstacleField.reset();
        survivalSeconds = 0.0f;
        state = GameState.PLAYING;
    }

    public void returnToReady() {
        player.reset();
        obstacleField.reset();
        survivalSeconds = 0.0f;
        state = GameState.READY;
    }

    public void pause() {
        if (state == GameState.PLAYING) state = GameState.PAUSED;
    }

    public void resume() {
        if (state == GameState.PAUSED) state = GameState.PLAYING;
    }

    public void update(float horizontalInput, float deltaSeconds) {
        if (state != GameState.PLAYING || !Float.isFinite(deltaSeconds) || deltaSeconds <= 0.0f) {
            return;
        }
        float safeDelta = Math.min(deltaSeconds, MAX_DELTA_SECONDS);
        player.update(horizontalInput, safeDelta);
        DifficultySettings difficulty = difficultyPolicy.forSurvivalSeconds(survivalSeconds);
        obstacleField.update(safeDelta, difficulty.getFallSpeed(),
                difficulty.getSpawnInterval(), difficulty.getMaximumActive());
        survivalSeconds += safeDelta;
        for (WorldRect obstacle : obstacleField.snapshotBounds()) {
            if (player.getBounds().overlaps(obstacle)) {
                state = GameState.GAME_OVER;
                break;
            }
        }
    }

    public GameSnapshot snapshot() {
        return new GameSnapshot(state, player.getBounds(), obstacleField.snapshotBounds(),
                player.getVelocityX(), survivalSeconds,
                (int) Math.floor(survivalSeconds * 10.0f),
                difficultyPolicy.forSurvivalSeconds(survivalSeconds).getTier());
    }
}
