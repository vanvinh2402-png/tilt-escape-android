package com.vanvinh2402.tiltescape;

import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameLoop;
import com.vanvinh2402.tiltescape.input.SensorRegistration;

public final class GameSessionController {
    private final GameEngine engine;
    private final GameLoop gameLoop;
    private final SensorRegistration sensorRegistration;
    private boolean active;

    public GameSessionController(GameEngine engine, GameLoop gameLoop,
            SensorRegistration sensorRegistration) {
        this.engine = engine;
        this.gameLoop = gameLoop;
        this.sensorRegistration = sensorRegistration;
    }

    public boolean canPlay() {
        return sensorRegistration.isAvailable();
    }

    public void onResume() {
        if (active || !canPlay()) {
            return;
        }
        sensorRegistration.register();
        gameLoop.start();
        active = true;
    }

    public void onPause() {
        if (!active) {
            return;
        }
        engine.pause();
        gameLoop.stop();
        sensorRegistration.unregister();
        active = false;
    }
}
