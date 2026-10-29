package com.vanvinh2402.tiltescape.ui;

import static org.junit.Assert.assertEquals;

import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameState;
import com.vanvinh2402.tiltescape.game.WorldRect;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class GameViewTest {
    private static final float EPSILON = 0.000001f;

    @Test
    public void tapStartsReadyGame() {
        GameEngine engine = new GameEngine();
        GameView view = new GameView(RuntimeEnvironment.getApplication(), engine, () -> 0.0f);

        view.performClick();

        assertEquals(GameState.PLAYING, engine.getState());
    }

    @Test
    public void tapResumesPausedGame() {
        GameEngine engine = new GameEngine();
        engine.startOrRestart();
        engine.pause();
        GameView view = new GameView(RuntimeEnvironment.getApplication(), engine, () -> 0.0f);

        view.performClick();

        assertEquals(GameState.PLAYING, engine.getState());
    }

    @Test
    public void tapRestartsGameOverAndResetsGeometry() {
        GameEngine engine = new GameEngine();
        engine.startOrRestart();
        for (int i = 0; i < 6; i++) {
            engine.update(-1.0f, 0.05f);
        }
        for (int i = 0; i < 60 && engine.getState() == GameState.PLAYING; i++) {
            engine.update(0.0f, 0.05f);
        }
        assertEquals(GameState.GAME_OVER, engine.getState());
        GameView view = new GameView(RuntimeEnvironment.getApplication(), engine, () -> 0.0f);

        view.performClick();

        WorldRect player = engine.snapshot().getPlayerBounds();
        assertEquals(GameState.PLAYING, engine.getState());
        assertEquals(0.44f, player.getLeft(), EPSILON);
        assertEquals(0.56f, player.getRight(), EPSILON);
    }
}
