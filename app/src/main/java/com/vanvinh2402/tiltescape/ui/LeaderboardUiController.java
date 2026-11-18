package com.vanvinh2402.tiltescape.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vanvinh2402.tiltescape.R;
import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameState;
import com.vanvinh2402.tiltescape.results.LeaderboardEntry;
import com.vanvinh2402.tiltescape.results.LeaderboardLoader;

import java.util.List;
import java.util.Objects;

/** Controls the local, per-player-high-score leaderboard panel. */
public final class LeaderboardUiController {
    private static final int LEADERBOARD_LIMIT = 10;

    private final Activity activity;
    private final GameEngine engine;
    private final LeaderboardLoader loader;
    private final View panel;
    private final View readyPanel;
    private final View empty;
    private final LinearLayout resultsContainer;
    private boolean bound;
    private int requestGeneration;

    public LeaderboardUiController(Activity activity, GameEngine engine, LeaderboardLoader loader) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.loader = Objects.requireNonNull(loader, "loader");
        panel = activity.findViewById(R.id.leaderboard_panel);
        readyPanel = activity.findViewById(R.id.ready_panel);
        empty = activity.findViewById(R.id.leaderboard_empty);
        resultsContainer = activity.findViewById(R.id.leaderboard_results_container);
    }

    public void bind() {
        if (bound) return;
        bound = true;
        activity.findViewById(R.id.leaderboard_button).setOnClickListener(view -> open());
        activity.findViewById(R.id.leaderboard_back_button).setOnClickListener(view -> close());
    }

    public void open() {
        if (engine.getState() != GameState.READY || isVisible()) return;
        int generation = ++requestGeneration;
        readyPanel.setVisibility(View.GONE);
        panel.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        resultsContainer.removeAllViews();
        loader.loadLeaderboard(LEADERBOARD_LIMIT, new LeaderboardLoader.Callback() {
            @Override
            public void onLoaded(List<LeaderboardEntry> entries) {
                if (!isCurrent(generation)) return;
                render(entries);
            }

            @Override
            public void onError() {
                if (!isCurrent(generation)) return;
                resultsContainer.removeAllViews();
                empty.setVisibility(View.VISIBLE);
            }
        });
    }

    public void close() {
        if (!isVisible()) return;
        requestGeneration++;
        panel.setVisibility(View.GONE);
        readyPanel.setVisibility(View.VISIBLE);
    }

    public boolean isVisible() {
        return panel.getVisibility() == View.VISIBLE;
    }

    public boolean handleBack() {
        if (!isVisible()) return false;
        close();
        return true;
    }

    private boolean isCurrent(int generation) {
        return generation == requestGeneration && isVisible();
    }

    private void render(List<LeaderboardEntry> entries) {
        resultsContainer.removeAllViews();
        empty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = activity.getLayoutInflater();
        for (int index = 0; index < entries.size(); index++) {
            LeaderboardEntry entry = entries.get(index);
            View row = inflater.inflate(R.layout.item_game_result, resultsContainer, false);
            row.<TextView>findViewById(R.id.result_position).setText(activity.getString(
                    R.string.achievements_position_format, index + 1));
            row.<TextView>findViewById(R.id.result_date).setText(entry.getUsername());
            row.<TextView>findViewById(R.id.result_score).setText(activity.getString(
                    R.string.achievements_score_format, entry.getScore()));
            resultsContainer.addView(row);
        }
    }
}
