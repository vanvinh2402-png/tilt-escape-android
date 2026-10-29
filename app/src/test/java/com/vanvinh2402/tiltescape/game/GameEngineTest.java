package com.vanvinh2402.tiltescape.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GameEngineTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void initialStateIsReadyAndStartBeginsPlay() {
        GameEngine engine = new GameEngine();

        assertEquals(GameState.READY, engine.getState());

        engine.startOrRestart();

        assertEquals(GameState.PLAYING, engine.getState());
    }

    @Test
    public void repeatedStartDoesNotResetActiveRun() {
        GameEngine engine = engineWithObstacle(0.10f, -0.20f, 0.0f);
        engine.startOrRestart();
        engine.update(1.0f, 0.1f);
        float movedX = engine.snapshot().getPlayerBounds().getLeft();

        engine.startOrRestart();

        assertEquals(movedX, engine.snapshot().getPlayerBounds().getLeft(), EPSILON);
    }

    @Test
    public void pauseAndResumeOnlyApplyToMatchingStates() {
        GameEngine engine = new GameEngine();

        engine.pause();
        assertEquals(GameState.READY, engine.getState());
        engine.resume();
        assertEquals(GameState.READY, engine.getState());

        engine.startOrRestart();
        engine.pause();
        assertEquals(GameState.PAUSED, engine.getState());
        engine.resume();
        assertEquals(GameState.PLAYING, engine.getState());
    }

    @Test
    public void restartFromGameOverResetsPlayerAndObstacle() {
        GameEngine engine = engineWithObstacle(0.50f, 0.86f, 0.0f);
        engine.startOrRestart();
        engine.update(1.0f, 0.1f);
        assertEquals(GameState.GAME_OVER, engine.getState());

        engine.startOrRestart();
        GameSnapshot snapshot = engine.snapshot();

        assertEquals(GameState.PLAYING, snapshot.getState());
        assertEquals(0.50f, centerX(snapshot.getPlayerBounds()), EPSILON);
        assertEquals(0.0f, snapshot.getPlayerVelocityX(), EPSILON);
        assertEquals(0.86f, centerY(snapshot.getObstacleBounds()), EPSILON);
    }

    @Test
    public void obstacleMovementUsesElapsedTime() {
        GameEngine tenSmallSteps = engineWithObstacle(0.10f, 0.10f, 0.20f);
        GameEngine fiveLargeSteps = engineWithObstacle(0.10f, 0.10f, 0.20f);
        tenSmallSteps.startOrRestart();
        fiveLargeSteps.startOrRestart();

        for (int i = 0; i < 10; i++) {
            tenSmallSteps.update(0.0f, 0.01f);
        }
        for (int i = 0; i < 5; i++) {
            fiveLargeSteps.update(0.0f, 0.02f);
        }

        assertEquals(
                tenSmallSteps.snapshot().getObstacleBounds().getTop(),
                fiveLargeSteps.snapshot().getObstacleBounds().getTop(),
                EPSILON);
    }

    @Test
    public void obstacleLeavingBottomResetsAboveTop() {
        GameEngine engine = engineWithObstacle(0.10f, 1.10f, 0.20f);
        engine.startOrRestart();

        engine.update(0.0f, 0.1f);

        assertTrue(engine.snapshot().getObstacleBounds().getBottom() <= 0.0f);
        assertEquals(0.10f, centerX(engine.snapshot().getObstacleBounds()), EPSILON);
    }

    @Test
    public void separatedRectanglesRemainPlaying() {
        GameEngine engine = engineWithObstacle(0.10f, 0.10f, 0.0f);
        engine.startOrRestart();

        engine.update(0.0f, 0.1f);

        assertEquals(GameState.PLAYING, engine.getState());
    }

    @Test
    public void overlapTransitionsToGameOverOnlyOnce() {
        GameEngine engine = engineWithObstacle(0.50f, 0.86f, 0.0f);
        engine.startOrRestart();
        engine.update(0.0f, 0.1f);
        WorldRect collisionPosition = engine.snapshot().getObstacleBounds();

        engine.update(1.0f, 1.0f);

        assertEquals(GameState.GAME_OVER, engine.getState());
        assertEquals(collisionPosition.getTop(), engine.snapshot().getObstacleBounds().getTop(), EPSILON);
    }

    @Test
    public void pausedAndGameOverStatesDoNotMoveEntities() {
        GameEngine paused = engineWithObstacle(0.10f, 0.10f, 0.20f);
        paused.startOrRestart();
        paused.pause();
        GameSnapshot pausedBefore = paused.snapshot();
        paused.update(1.0f, 1.0f);

        assertEquals(pausedBefore.getPlayerBounds().getLeft(), paused.snapshot().getPlayerBounds().getLeft(), EPSILON);
        assertEquals(pausedBefore.getObstacleBounds().getTop(), paused.snapshot().getObstacleBounds().getTop(), EPSILON);

        GameEngine over = engineWithObstacle(0.50f, 0.86f, 0.0f);
        over.startOrRestart();
        over.update(0.0f, 0.1f);
        GameSnapshot overBefore = over.snapshot();
        over.update(1.0f, 1.0f);

        assertEquals(overBefore.getPlayerBounds().getLeft(), over.snapshot().getPlayerBounds().getLeft(), EPSILON);
        assertEquals(overBefore.getObstacleBounds().getTop(), over.snapshot().getObstacleBounds().getTop(), EPSILON);
    }

    private static GameEngine engineWithObstacle(float centerX, float centerY, float speed) {
        return new GameEngine(new Obstacle(centerX, centerY, speed));
    }

    private static float centerX(WorldRect rect) {
        return (rect.getLeft() + rect.getRight()) / 2.0f;
    }

    private static float centerY(WorldRect rect) {
        return (rect.getTop() + rect.getBottom()) / 2.0f;
    }
}
