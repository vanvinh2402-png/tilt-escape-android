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
import com.vanvinh2402.tiltescape.results.LeaderboardEntry;
import com.vanvinh2402.tiltescape.results.LeaderboardLoader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class LeaderboardUiControllerTest {
    @Test
    public void emptyLeaderboardOpensAndReturnsToReady() {
        try (Harness harness = new Harness()) {
            harness.activity.<Button>findViewById(R.id.leaderboard_button).performClick();

            assertEquals(View.VISIBLE,
                    harness.activity.findViewById(R.id.leaderboard_panel).getVisibility());
            assertEquals(View.GONE, harness.activity.findViewById(R.id.ready_panel).getVisibility());
            assertEquals(10, harness.loader.lastLimit);

            harness.loader.deliver(Collections.emptyList());
            assertEquals(View.VISIBLE,
                    harness.activity.findViewById(R.id.leaderboard_empty).getVisibility());

            assertTrue(harness.leaderboard.handleBack());
            assertEquals(View.VISIBLE, harness.activity.findViewById(R.id.ready_panel).getVisibility());
            assertFalse(harness.leaderboard.handleBack());
        }
    }

    @Test
    public void loadedLeaderboardShowsPlayerNamesAndScoresInOrder() {
        try (Harness harness = new Harness()) {
            harness.leaderboard.open();
            harness.loader.deliver(Arrays.asList(
                    new LeaderboardEntry("Alpha", 12),
                    new LeaderboardEntry("Bravo", 9)));

            LinearLayout rows = harness.activity.findViewById(R.id.leaderboard_results_container);
            assertEquals(2, rows.getChildCount());
            assertEquals("Alpha", rows.getChildAt(0).<TextView>findViewById(R.id.result_date)
                    .getText().toString());
            assertEquals(harness.activity.getString(R.string.achievements_score_format, 12),
                    rows.getChildAt(0).<TextView>findViewById(R.id.result_score)
                            .getText().toString());
            assertEquals("2", rows.getChildAt(1).<TextView>findViewById(R.id.result_position)
                    .getText().toString());
        }
    }

    @Test
    public void staleLeaderboardCallbackAfterCloseDoesNotRenderRows() {
        try (Harness harness = new Harness()) {
            harness.leaderboard.open();
            harness.leaderboard.close();
            harness.loader.deliver(Collections.singletonList(new LeaderboardEntry("Alpha", 12)));

            LinearLayout rows = harness.activity.findViewById(R.id.leaderboard_results_container);
            assertEquals(0, rows.getChildCount());
            assertEquals(View.VISIBLE, harness.activity.findViewById(R.id.ready_panel).getVisibility());
        }
    }

    private static final class Harness implements AutoCloseable {
        final ActivityController<MainActivity> activityController =
                Robolectric.buildActivity(MainActivity.class).setup();
        final MainActivity activity = activityController.get();
        final GameEngine engine = new GameEngine();
        final FakeLoader loader = new FakeLoader();
        final LeaderboardUiController leaderboard =
                new LeaderboardUiController(activity, engine, loader);

        Harness() {
            leaderboard.bind();
        }

        @Override
        public void close() {
            activityController.close();
        }
    }

    private static final class FakeLoader implements LeaderboardLoader {
        int lastLimit;
        Callback callback;

        @Override
        public void loadLeaderboard(int limit, Callback requestedCallback) {
            lastLimit = limit;
            callback = requestedCallback;
        }

        void deliver(List<LeaderboardEntry> entries) {
            callback.onLoaded(entries);
        }
    }
}
