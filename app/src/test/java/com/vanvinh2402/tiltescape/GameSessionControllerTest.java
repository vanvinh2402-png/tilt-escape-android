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
    public void resumeRegistersAvailableSensorAndStartsLoopOnce() {
        GameEngine engine = new GameEngine();
        FakeGameLoop loop = new FakeGameLoop();
        FakeSensorRegistration sensor = new FakeSensorRegistration(true);
        GameSessionController controller = new GameSessionController(engine, loop, sensor);

        controller.onResume();
        controller.onResume();

        assertTrue(controller.canPlay());
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
