package com.vanvinh2402.tiltescape;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameLoop;
import com.vanvinh2402.tiltescape.game.GameState;
import com.vanvinh2402.tiltescape.input.SensorRegistration;

import org.junit.Test;

public final class GameSessionControllerTest {
    @Test
    public void resumeRegistersAvailableSensorButDoesNotStartReadyLoop() {
        GameEngine engine = new GameEngine();
        FakeGameLoop loop = new FakeGameLoop();
        FakeSensorRegistration sensor = new FakeSensorRegistration(true);
        GameSessionController controller = new GameSessionController(engine, loop, sensor);

        controller.onResume();
        controller.onResume();

        assertTrue(controller.canPlay());
        assertEquals(1, sensor.registerCalls);
        assertEquals(0, loop.startCalls);
    }

    @Test
    public void resumeStartsLoopOnceOnlyWhenAlreadyPlaying() {
        GameEngine engine = new GameEngine();
        engine.startNewRun();
        FakeGameLoop loop = new FakeGameLoop();
        FakeSensorRegistration sensor = new FakeSensorRegistration(true);
        GameSessionController controller = new GameSessionController(engine, loop, sensor);

        controller.onResume();
        controller.onResume();

        assertEquals(1, sensor.registerCalls);
        assertEquals(1, loop.startCalls);
    }

    @Test
    public void pauseStopsSessionAndPausesPlayingEngineOnce() {
        GameEngine engine = new GameEngine();
        engine.startOrRestart();
        FakeGameLoop loop = new FakeGameLoop();
        FakeSensorRegistration sensor = new FakeSensorRegistration(true);
        GameSessionController controller = new GameSessionController(engine, loop, sensor);
        controller.onResume();

        controller.onPause();
        controller.onPause();

        assertEquals(GameState.PAUSED, engine.getState());
        assertEquals(1, loop.stopCalls);
        assertEquals(1, sensor.unregisterCalls);
    }

    @Test
    public void missingSensorCannotPlayOrStartSession() {
        GameEngine engine = new GameEngine();
        FakeGameLoop loop = new FakeGameLoop();
        FakeSensorRegistration sensor = new FakeSensorRegistration(false);
        GameSessionController controller = new GameSessionController(engine, loop, sensor);

        controller.onResume();
        controller.onPause();

        assertFalse(controller.canPlay());
        assertEquals(0, sensor.registerCalls);
        assertEquals(0, sensor.unregisterCalls);
        assertEquals(0, loop.startCalls);
        assertEquals(0, loop.stopCalls);
    }

    @Test
    public void twoLifecycleCyclesCoverReadyPlayingPausedAndGameOver() {
        GameEngine[] engines = {readyEngine(), playingEngine(), pausedEngine(), gameOverEngine()};
        GameState[] expectedStates = {
                GameState.READY, GameState.PAUSED, GameState.PAUSED, GameState.GAME_OVER};
        int[] expectedStarts = {0, 1, 0, 0};

        for (int i = 0; i < engines.length; i++) {
            FakeGameLoop loop = new FakeGameLoop();
            FakeSensorRegistration sensor = new FakeSensorRegistration(true);
            GameSessionController controller = new GameSessionController(engines[i], loop, sensor);

            controller.onResume();
            controller.onResume();
            controller.onPause();
            controller.onPause();
            controller.onResume();
            controller.onResume();
            controller.onPause();
            controller.onPause();

            assertEquals(expectedStates[i], engines[i].getState());
            assertEquals(expectedStarts[i], loop.startCalls);
            assertEquals(2, loop.stopCalls);
            assertEquals(2, sensor.registerCalls);
            assertEquals(2, sensor.unregisterCalls);
        }
    }

    private static GameEngine readyEngine() {
        return new GameEngine();
    }

    private static GameEngine playingEngine() {
        GameEngine engine = new GameEngine();
        engine.startNewRun();
        return engine;
    }

    private static GameEngine pausedEngine() {
        GameEngine engine = new GameEngine();
        engine.startNewRun();
        engine.pause();
        return engine;
    }

    private static GameEngine gameOverEngine() {
        GameEngine engine = new GameEngine();
        engine.startNewRun();
        for (int i = 0; i < 400 && engine.getState() == GameState.PLAYING; i++) {
            float playerCenter = (engine.snapshot().getPlayerBounds().getLeft()
                    + engine.snapshot().getPlayerBounds().getRight()) / 2.0f;
            float obstacleCenter = (engine.snapshot().getObstacleBounds().get(0).getLeft()
                    + engine.snapshot().getObstacleBounds().get(0).getRight()) / 2.0f;
            engine.update(Math.signum(obstacleCenter - playerCenter), 0.05f);
        }
        assertEquals(GameState.GAME_OVER, engine.getState());
        return engine;
    }

    private static final class FakeGameLoop implements GameLoop {
        int startCalls;
        int stopCalls;

        @Override
        public void start() {
            startCalls++;
        }

        @Override
        public void stop() {
            stopCalls++;
        }
    }

    private static final class FakeSensorRegistration implements SensorRegistration {
        private final boolean available;
        int registerCalls;
        int unregisterCalls;

        FakeSensorRegistration(boolean available) {
            this.available = available;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public void register() {
            registerCalls++;
        }

        @Override
        public void unregister() {
            unregisterCalls++;
        }
    }
}
