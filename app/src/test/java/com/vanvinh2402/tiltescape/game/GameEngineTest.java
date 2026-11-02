package com.vanvinh2402.tiltescape.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GameEngineTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void scoreIncrementsOnlyAfterACompleteObstaclePass() {
        GameEngine engine = engineInLane(0);
        engine.startNewRun();
        assertEquals(0, engine.snapshot().getScore());

        for (int i = 0; i < 71; i++) {
            engine.update(0.0f, 0.05f);
        }
        assertEquals(0, engine.snapshot().getScore());
        assertEquals(3.55f, engine.snapshot().getSurvivalSeconds(), EPSILON);

        for (int i = 0; i < 2 && engine.snapshot().getScore() == 0; i++) {
            engine.update(0.0f, 0.05f);
        }
        assertEquals(1, engine.snapshot().getScore());
        assertTrue(engine.snapshot().getSurvivalSeconds() > 3.55f);
    }

    @Test
    public void invalidAndOversizedDeltasCannotAwardAbnormalTime() {
        GameEngine engine = engineInLane(0);
        engine.startNewRun();
        engine.update(0.0f, Float.NaN);
        engine.update(0.0f, Float.POSITIVE_INFINITY);
        engine.update(0.0f, 0.0f);
        engine.update(0.0f, -1.0f);
        assertEquals(0.0f, engine.snapshot().getSurvivalSeconds(), EPSILON);

        engine.update(0.0f, 1.0f);
        assertEquals(0.05f, engine.snapshot().getSurvivalSeconds(), EPSILON);
        assertEquals(0, engine.snapshot().getScore());
    }

    @Test
    public void pauseReadyAndGameOverFreezeTimeAndScore() {
        GameEngine paused = engineInLane(0);
        paused.startNewRun();
        advanceUntilScore(paused, 1);
        float pausedTime = paused.snapshot().getSurvivalSeconds();
        paused.pause();
        paused.update(0.0f, 1.0f);
        assertEquals(pausedTime, paused.snapshot().getSurvivalSeconds(), EPSILON);
        assertEquals(1, paused.snapshot().getScore());

        paused.returnToReady();
        paused.update(0.0f, 1.0f);
        assertEquals(GameState.READY, paused.getState());
        assertEquals(0, paused.snapshot().getScore());

        GameEngine over = new GameEngine(
                new ObstacleField(new SequenceRandomSource(1, 2)));
        over.startNewRun();
        while (over.getState() == GameState.PLAYING) {
            over.update(0.0f, 0.05f);
        }
        int finalScore = over.snapshot().getScore();
        float finalTime = over.snapshot().getSurvivalSeconds();
        over.update(1.0f, 1.0f);
        assertEquals(GameState.GAME_OVER, over.getState());
        assertEquals(0, finalScore);
        assertEquals(finalScore, over.snapshot().getScore());
        assertEquals(finalTime, over.snapshot().getSurvivalSeconds(), EPSILON);
    }

    @Test
    public void startNewRunResetsPausedOrGameOverRunAndField() {
        GameEngine engine = engineInLane(0);
        engine.startNewRun();
        advanceUntilScore(engine, 1);
        engine.pause();

        engine.startNewRun();
        GameSnapshot snapshot = engine.snapshot();
        assertEquals(GameState.PLAYING, snapshot.getState());
        assertEquals(0, snapshot.getScore());
        assertEquals(0.0f, snapshot.getSurvivalSeconds(), EPSILON);
        assertEquals(0.50f, centerX(snapshot.getPlayerBounds()), EPSILON);
        assertEquals(1, snapshot.getObstacleBounds().size());
    }

    @Test
    public void snapshotContainsMultipleObstaclesAndCollisionChecksCollection() {
        ObstacleField field = new ObstacleField(new SequenceRandomSource(0, 2));
        GameEngine engine = new GameEngine(field);
        engine.startNewRun();
        field.update(0.30f, 1.0f, 0.10f, 2);
        assertEquals(2, engine.snapshot().getObstacleBounds().size());

        for (int i = 0; i < 100 && engine.getState() == GameState.PLAYING; i++) {
            engine.update(0.0f, 0.05f);
        }
        assertEquals(GameState.GAME_OVER, engine.getState());
    }

    @Test
    public void startOrRestartRemainsIdempotentDuringPlay() {
        GameEngine engine = engineInLane(0);
        engine.startOrRestart();
        engine.update(1.0f, 0.05f);
        float moved = engine.snapshot().getPlayerBounds().getLeft();
        engine.startOrRestart();
        assertEquals(moved, engine.snapshot().getPlayerBounds().getLeft(), EPSILON);
    }

    @Test
    public void difficultyTierAdvancesDuringPlayButNotWhilePaused() {
        GameEngine engine = engineInLane(0);
        engine.startNewRun();
        for (int i = 0; i < 300; i++) {
            engine.update(0.0f, 0.05f);
        }
        assertEquals(1, engine.snapshot().getDifficultyTier());
        engine.pause();
        for (int i = 0; i < 300; i++) {
            engine.update(0.0f, 0.05f);
        }
        assertEquals(1, engine.snapshot().getDifficultyTier());
    }

    private static GameEngine engineInLane(int lane) {
        return new GameEngine(new ObstacleField(new SequenceRandomSource(lane)));
    }

    private static void advanceUntilScore(GameEngine engine, int expectedScore) {
        for (int i = 0; i < 100 && engine.snapshot().getScore() < expectedScore; i++) {
            engine.update(0.0f, 0.05f);
        }
        assertEquals(expectedScore, engine.snapshot().getScore());
    }

    private static float centerX(WorldRect rect) {
        return (rect.getLeft() + rect.getRight()) / 2.0f;
    }

    private static final class SequenceRandomSource implements RandomSource {
        private final int[] values;
        private int index;

        SequenceRandomSource(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            int result = values[Math.min(index, values.length - 1)];
            index++;
            return result;
        }
    }
}
