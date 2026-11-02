package com.vanvinh2402.tiltescape.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vanvinh2402.tiltescape.R;
import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameState;
import com.vanvinh2402.tiltescape.results.AchievementsData;
import com.vanvinh2402.tiltescape.results.AchievementsLoader;
import com.vanvinh2402.tiltescape.results.GameResult;
import com.vanvinh2402.tiltescape.results.GameResultsCoordinator;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public final class AchievementsUiController {
    private final Activity activity;
    private final GameEngine engine;
    private final GameView gameView;
    private final AchievementsLoader loader;
    private final View panel;
    private final View readyPanel;
    private final TextView highScore;
    private final View loading;
    private final View empty;
    private final View error;
    private final LinearLayout resultsContainer;
    private boolean bound;
    private int requestGeneration;

    public AchievementsUiController(Activity activity, GameEngine engine, GameView gameView,
            AchievementsLoader loader) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.gameView = Objects.requireNonNull(gameView, "gameView");
        this.loader = Objects.requireNonNull(loader, "loader");
        panel = activity.findViewById(R.id.achievements_panel);
        readyPanel = activity.findViewById(R.id.ready_panel);
        highScore = activity.findViewById(R.id.achievements_high_score);
        loading = activity.findViewById(R.id.achievements_loading);
        empty = activity.findViewById(R.id.achievements_empty);
        error = activity.findViewById(R.id.achievements_error);
        resultsContainer = activity.findViewById(R.id.achievements_results_container);
    }

    public void bind() {
        if (bound) return;
        bound = true;
        activity.findViewById(R.id.achievements_button).setOnClickListener(view -> open());
        activity.findViewById(R.id.achievements_back_button).setOnClickListener(view -> close());
    }

    public void open() {
        if (engine.getState() != GameState.READY || isVisible()) return;
        int generation = ++requestGeneration;
        readyPanel.setVisibility(View.GONE);
        panel.setVisibility(View.VISIBLE);
        loading.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        error.setVisibility(View.GONE);
        resultsContainer.removeAllViews();
        highScore.setText(activity.getString(R.string.achievements_high_score_format, 0));
        loader.loadAchievements(new GameResultsCoordinator.Callback() {
            @Override
            public void onLoaded(AchievementsData data) {
                if (!isCurrent(generation)) return;
                render(data);
            }

            @Override
            public void onError() {
                if (!isCurrent(generation)) return;
                loading.setVisibility(View.GONE);
                empty.setVisibility(View.GONE);
                error.setVisibility(View.VISIBLE);
            }
        });
    }

    public void close() {
        if (!isVisible()) return;
        requestGeneration++;
        panel.setVisibility(View.GONE);
        gameView.refreshSnapshot();
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

    private void render(AchievementsData data) {
        highScore.setText(activity.getString(
                R.string.achievements_high_score_format, data.getHighScore()));
        loading.setVisibility(View.GONE);
        error.setVisibility(View.GONE);
        resultsContainer.removeAllViews();
        empty.setVisibility(data.getRecent().isEmpty() ? View.VISIBLE : View.GONE);
        DateFormat formatter = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
        LayoutInflater inflater = activity.getLayoutInflater();
        for (int index = 0; index < data.getRecent().size(); index++) {
            GameResult result = data.getRecent().get(index);
            View row = inflater.inflate(R.layout.item_game_result, resultsContainer, false);
            row.<TextView>findViewById(R.id.result_position).setText(activity.getString(
                    R.string.achievements_position_format, index + 1));
            row.<TextView>findViewById(R.id.result_date).setText(
                    formatter.format(new Date(result.getPlayedAtEpochMillis())));
            row.<TextView>findViewById(R.id.result_score).setText(activity.getString(
                    R.string.achievements_score_format, result.getScore()));
            resultsContainer.addView(row);
        }
    }
}
