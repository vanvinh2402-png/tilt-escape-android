package com.vanvinh2402.tiltescape.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameState;
import com.vanvinh2402.tiltescape.game.WorldRect;
import com.vanvinh2402.tiltescape.input.TiltInput;

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
        for (int i = 0; i < 400 && engine.getState() == GameState.PLAYING; i++) {
            WorldRect player = engine.snapshot().getPlayerBounds();
            WorldRect obstacle = engine.snapshot().getObstacleBounds().get(0);
            float playerCenter = (player.getLeft() + player.getRight()) / 2.0f;
            float obstacleCenter = (obstacle.getLeft() + obstacle.getRight()) / 2.0f;
            engine.update(Math.signum(obstacleCenter - playerCenter), 0.05f);
        }
        assertEquals(GameState.GAME_OVER, engine.getState());
        GameView view = new GameView(RuntimeEnvironment.getApplication(), engine, () -> 0.0f);

        view.performClick();

        WorldRect player = engine.snapshot().getPlayerBounds();
        assertEquals(GameState.PLAYING, engine.getState());
        assertEquals(0.44f, player.getLeft(), EPSILON);
        assertEquals(0.56f, player.getRight(), EPSILON);
    }

    @Test
    public void frameLoopDoesNotReadTiltOutsidePlayingAndTapRestartsIt() {
        GameEngine engine = new GameEngine();
        CountingTiltInput tiltInput = new CountingTiltInput();
        GameView view = new GameView(RuntimeEnvironment.getApplication(), engine, tiltInput);

        view.start();
        view.doFrame(1_000_000_000L);
        assertEquals(0, tiltInput.readCount);

        view.performClick();
        view.doFrame(1_016_000_000L);
        assertEquals(1, tiltInput.readCount);

        engine.pause();
        view.doFrame(1_032_000_000L);
        assertEquals(1, tiltInput.readCount);
    }

    @Test
    public void startOutsidePlayingDoesNotArmAFrameForLater() {
        GameEngine engine = new GameEngine();
        CountingTiltInput tiltInput = new CountingTiltInput();
        GameView view = new GameView(RuntimeEnvironment.getApplication(), engine, tiltInput);

        view.start();
        engine.startNewRun();
        view.doFrame(1_000_000_000L);

        assertEquals(0, tiltInput.readCount);
    }

    @Test
    public void completedFrameAndExplicitRefreshNotifyListenerOnceEach() {
        GameEngine engine = new GameEngine();
        GameView view = new GameView(RuntimeEnvironment.getApplication(), engine, () -> 0.0f);
        final int[] notifications = {0};
        view.setSnapshotListener(snapshot -> notifications[0]++);
        engine.startNewRun();
        view.start();
        view.doFrame(1_000_000_000L);
        assertEquals(1, notifications[0]);
        view.refreshSnapshot();
        assertEquals(2, notifications[0]);
    }

    @Test
    public void playfieldHasLocalizedContentDescription() {
        GameView view = new GameView(RuntimeEnvironment.getApplication(), new GameEngine(), () -> 0.0f);
        assertNotNull(view.getContentDescription());
        assertEquals(RuntimeEnvironment.getApplication().getString(
                com.vanvinh2402.tiltescape.R.string.playfield_description),
                view.getContentDescription().toString());
    }

    private static final class CountingTiltInput implements TiltInput {
        int readCount;

        @Override
        public float getHorizontalInput() {
            readCount++;
            return 0.0f;
        }
    }
}
