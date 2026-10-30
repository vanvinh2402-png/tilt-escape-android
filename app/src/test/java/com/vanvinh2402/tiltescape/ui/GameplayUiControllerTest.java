package com.vanvinh2402.tiltescape.ui;

import static org.junit.Assert.assertEquals;

import android.view.View;
import android.widget.Button;

import com.vanvinh2402.tiltescape.MainActivity;
import com.vanvinh2402.tiltescape.R;
import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameState;
import com.vanvinh2402.tiltescape.game.WorldRect;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public final class GameplayUiControllerTest {
    @Test
    public void gameOverOffersPlayAgainAndMainMenu() {
        try (ActivityController<MainActivity> activityController =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = activityController.get();
            GameEngine engine = new GameEngine();
            GameView gameView = new GameView(activity, engine, () -> 0.0f);
            GameplayUiController controller = new GameplayUiController(activity, engine, gameView);
            controller.bind();
            gameView.setSnapshotListener(controller::refresh);

            driveToGameOver(engine);
            gameView.refreshSnapshot();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.game_over_panel).getVisibility());

            activity.<Button>findViewById(R.id.play_again_button).performClick();
            assertEquals(GameState.PLAYING, engine.getState());
            assertEquals(View.GONE, activity.findViewById(R.id.game_over_panel).getVisibility());

            driveToGameOver(engine);
            gameView.refreshSnapshot();
            activity.<Button>findViewById(R.id.game_over_main_menu_button).performClick();
            assertEquals(GameState.READY, engine.getState());
            assertEquals(View.VISIBLE, activity.findViewById(R.id.ready_panel).getVisibility());
            gameView.stop();
        }
    }

    private static void driveToGameOver(GameEngine engine) {
        if (engine.getState() != GameState.PLAYING) engine.startNewRun();
        for (int i = 0; i < 400 && engine.getState() == GameState.PLAYING; i++) {
            WorldRect player = engine.snapshot().getPlayerBounds();
            WorldRect obstacle = engine.snapshot().getObstacleBounds().get(0);
            float playerCenter = (player.getLeft() + player.getRight()) / 2.0f;
            float obstacleCenter = (obstacle.getLeft() + obstacle.getRight()) / 2.0f;
            engine.update(Math.signum(obstacleCenter - playerCenter), 0.05f);
        }
        assertEquals(GameState.GAME_OVER, engine.getState());
    }
}
