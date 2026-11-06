package com.vanvinh2402.tiltescape.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vanvinh2402.tiltescape.MainActivity;
import com.vanvinh2402.tiltescape.R;
import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameState;
import com.vanvinh2402.tiltescape.results.AchievementsData;
import com.vanvinh2402.tiltescape.results.AchievementsLoader;
import com.vanvinh2402.tiltescape.results.GameResult;
import com.vanvinh2402.tiltescape.results.GameResultsCoordinator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class AchievementsUiControllerTest {
    @Test
    public void readyAchievementsButtonOpensWithoutStartingGame() {
        try (Harness harness = new Harness()) {
            harness.activity.<Button>findViewById(R.id.achievements_button).performClick();

            assertEquals(GameState.READY, harness.engine.getState());
            assertEquals(View.GONE, harness.activity.findViewById(R.id.pause_button).getVisibility());
            assertEquals(View.GONE, harness.activity.findViewById(R.id.ready_panel).getVisibility());
            assertEquals(View.VISIBLE,
                    harness.activity.findViewById(R.id.achievements_panel).getVisibility());
            assertEquals(1, harness.loader.loadCount);
            assertEquals(42L, harness.loader.lastUserId);
        }
    }

    @Test
    public void loadingThenEmptyShowsZeroAndEmptyMessage() {
        try (Harness harness = new Harness()) {
            harness.achievements.open();
            assertEquals(View.VISIBLE,
                    harness.activity.findViewById(R.id.achievements_loading).getVisibility());

            harness.loader.deliver(new AchievementsData(0, Collections.emptyList()));

            assertEquals(harness.activity.getString(R.string.achievements_high_score_format, 0),
                    harness.activity.<TextView>findViewById(R.id.achievements_high_score)
                            .getText().toString());
            assertEquals(View.GONE,
                    harness.activity.findViewById(R.id.achievements_loading).getVisibility());
            assertEquals(View.VISIBLE,
                    harness.activity.findViewById(R.id.achievements_empty).getVisibility());
        }
    }

    @Test
    public void populatedDataShowsHighScoreAndTenNewestRows() {
        try (Harness harness = new Harness()) {
            List<GameResult> recent = new ArrayList<>();
            for (int index = 0; index < 10; index++) {
                recent.add(new GameResult(index + 1L, 12 - index, 1_000L,
                        1_793_610_000_000L - index * 60_000L));
            }
            harness.achievements.open();
            harness.loader.deliver(new AchievementsData(24, recent));

            assertEquals(harness.activity.getString(R.string.achievements_high_score_format, 24),
                    harness.activity.<TextView>findViewById(R.id.achievements_high_score)
                            .getText().toString());
            LinearLayout rows = harness.activity.findViewById(R.id.achievements_results_container);
            assertEquals(10, rows.getChildCount());
            assertEquals(harness.activity.getString(R.string.achievements_score_format, 12),
                    rows.getChildAt(0).<TextView>findViewById(R.id.result_score)
                            .getText().toString());
            assertEquals(harness.activity.getString(R.string.achievements_score_format, 3),
                    rows.getChildAt(9).<TextView>findViewById(R.id.result_score)
                            .getText().toString());
            assertFalse(rows.getChildAt(0).<TextView>findViewById(R.id.result_date)
                    .getText().toString().isEmpty());
        }
    }

    @Test
    public void queryFailureShowsRecoverableErrorAndBackRemainsEnabled() {
        try (Harness harness = new Harness()) {
            harness.achievements.open();
            harness.loader.fail();

            assertEquals(View.VISIBLE,
                    harness.activity.findViewById(R.id.achievements_error).getVisibility());
            Button back = harness.activity.findViewById(R.id.achievements_back_button);
            assertTrue(back.isEnabled());
            back.performClick();
            assertEquals(View.VISIBLE,
                    harness.activity.findViewById(R.id.ready_panel).getVisibility());
        }
    }

    @Test
    public void backButtonAndSystemBackReturnToReady() {
        try (Harness harness = new Harness()) {
            harness.achievements.open();
            assertTrue(harness.achievements.handleBack());
            assertEquals(View.VISIBLE,
                    harness.activity.findViewById(R.id.ready_panel).getVisibility());
            assertFalse(harness.achievements.handleBack());

            harness.achievements.open();
            harness.activity.<Button>findViewById(R.id.achievements_back_button).performClick();
            assertEquals(View.VISIBLE,
                    harness.activity.findViewById(R.id.ready_panel).getVisibility());
        }
    }

    @Test
    public void staleLoadCallbackAfterCloseDoesNotRender() {
        try (Harness harness = new Harness()) {
            harness.achievements.open();
            harness.achievements.close();
            harness.loader.deliver(new AchievementsData(
                    99,
                    Collections.singletonList(new GameResult(1L, 99, 1_000L, 1_000L))));

            assertEquals(View.GONE,
                    harness.activity.findViewById(R.id.achievements_panel).getVisibility());
            assertEquals(View.VISIBLE,
                    harness.activity.findViewById(R.id.ready_panel).getVisibility());
            LinearLayout rows = harness.activity.findViewById(R.id.achievements_results_container);
            assertEquals(0, rows.getChildCount());
        }
    }

    private static final class Harness implements AutoCloseable {
        final ActivityController<MainActivity> activityController =
                Robolectric.buildActivity(MainActivity.class).setup();
        final MainActivity activity = activityController.get();
        final GameEngine engine = new GameEngine();
        final GameView gameView = new GameView(activity, engine, () -> 0.0f);
        final FakeLoader loader = new FakeLoader();
        final GameplayUiController gameplay = new GameplayUiController(activity, engine, gameView);
        final AchievementsUiController achievements =
                new AchievementsUiController(activity, engine, loader, () -> 42L);

        Harness() {
            gameplay.bind();
            achievements.bind();
            gameView.setSnapshotListener(gameplay::refresh);
            gameView.refreshSnapshot();
        }

        @Override
        public void close() {
            gameView.stop();
            activityController.close();
        }
    }

    private static final class FakeLoader implements AchievementsLoader {
        int loadCount;
        long lastUserId;
        GameResultsCoordinator.Callback callback;

        @Override
        public void loadAchievements(
                long userId, GameResultsCoordinator.Callback requestedCallback) {
            loadCount++;
            lastUserId = userId;
            callback = requestedCallback;
        }

        void deliver(AchievementsData data) {
            callback.onLoaded(data);
        }

        void fail() {
            callback.onError();
        }
    }
}
