package com.vanvinh2402.tiltescape.ui;

import static org.junit.Assert.assertEquals;

import android.view.View;
import android.widget.Button;

import com.vanvinh2402.tiltescape.MainActivity;
import com.vanvinh2402.tiltescape.R;
import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameResultRecorder;
import com.vanvinh2402.tiltescape.game.GameState;
import com.vanvinh2402.tiltescape.game.WorldRect;
import com.vanvinh2402.tiltescape.results.GameResult;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class GameplayUiControllerTest {
    @Test
    public void repeatedGameOverRefreshPersistsOnlyOnce() {
        try (ActivityController<MainActivity> activityController =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = activityController.get();
            GameEngine engine = new GameEngine();
            GameView gameView = new GameView(activity, engine, () -> 0.0f);
            List<GameResult> saved = new ArrayList<>();
            GameResultRecorder recorder = new GameResultRecorder(saved::add, () -> 1_000L);
            GameplayUiController controller = new GameplayUiController(
                    activity, engine, gameView, recorder, () -> false);

            engine.startNewRun();
            controller.refresh(engine.snapshot());
            driveToGameOver(engine);
            controller.refresh(engine.snapshot());
            controller.refresh(engine.snapshot());
            controller.refresh(engine.snapshot());

            assertEquals(1, saved.size());
        }
    }

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

    @Test
    public void repeatedControlClicksKeepStateAndDoNotResetAnActiveRun() {
        try (ActivityController<MainActivity> activityController =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = activityController.get();
            GameEngine engine = new GameEngine();
            GameView gameView = new GameView(activity, engine, () -> 0.0f);
            GameplayUiController controller = new GameplayUiController(activity, engine, gameView);
            controller.bind();
            controller.bind();
            gameView.setSnapshotListener(controller::refresh);
            gameView.refreshSnapshot();

            Button start = activity.findViewById(R.id.start_button);
            start.performClick();
            start.performClick();
            assertEquals(GameState.PLAYING, engine.getState());

            Button pause = activity.findViewById(R.id.pause_button);
            pause.performClick();
            pause.performClick();
            assertEquals(GameState.PAUSED, engine.getState());

            Button resume = activity.findViewById(R.id.resume_button);
            resume.performClick();
            resume.performClick();
            assertEquals(GameState.PLAYING, engine.getState());

            pause.performClick();
            Button restart = activity.findViewById(R.id.restart_button);
            restart.performClick();
            restart.performClick();
            engine.update(0.0f, 0.05f);
            engine.update(0.0f, 0.05f);
            float activeTime = engine.snapshot().getSurvivalSeconds();
            restart.performClick();
            assertEquals(0.10f, activeTime, 0.0001f);
            assertEquals(activeTime, engine.snapshot().getSurvivalSeconds(), 0.0001f);

            pause.performClick();
            Button menu = activity.findViewById(R.id.pause_main_menu_button);
            menu.performClick();
            menu.performClick();
            assertEquals(GameState.READY, engine.getState());

            driveToGameOver(engine);
            gameView.refreshSnapshot();
            Button playAgain = activity.findViewById(R.id.play_again_button);
            playAgain.performClick();
            playAgain.performClick();
            engine.update(0.0f, 0.05f);
            engine.update(0.0f, 0.05f);
            activeTime = engine.snapshot().getSurvivalSeconds();
            playAgain.performClick();
            assertEquals(0.10f, activeTime, 0.0001f);
            assertEquals(activeTime, engine.snapshot().getSurvivalSeconds(), 0.0001f);
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
