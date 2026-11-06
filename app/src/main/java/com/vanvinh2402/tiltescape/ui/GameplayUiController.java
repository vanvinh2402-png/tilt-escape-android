package com.vanvinh2402.tiltescape.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.vanvinh2402.tiltescape.R;
import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameResultRecorder;
import com.vanvinh2402.tiltescape.game.GameSnapshot;
import com.vanvinh2402.tiltescape.game.GameState;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class GameplayUiController {
    private final Activity activity;
    private final GameEngine engine;
    private final GameView gameView;
    private final GameResultRecorder resultRecorder;
    private final BooleanSupplier achievementsVisible;
    private final BooleanSupplier mainMenuAllowed;
    private final TextView scoreText;
    private final TextView finalScoreText;
    private final View stateOverlay;
    private final View readyPanel;
    private final View pausePanel;
    private final View gameOverPanel;
    private final View pauseButton;
    private boolean bound;

    public GameplayUiController(Activity activity, GameEngine engine, GameView gameView) {
        this(activity, engine, gameView,
                new GameResultRecorder(result -> { }, System::currentTimeMillis),
                () -> false, () -> true);
    }

    public GameplayUiController(Activity activity, GameEngine engine, GameView gameView,
            GameResultRecorder resultRecorder, BooleanSupplier achievementsVisible) {
        this(activity, engine, gameView, resultRecorder, achievementsVisible, () -> true);
    }

    public GameplayUiController(Activity activity, GameEngine engine, GameView gameView,
            GameResultRecorder resultRecorder, BooleanSupplier achievementsVisible,
            BooleanSupplier mainMenuAllowed) {
        this.activity = activity;
        this.engine = engine;
        this.gameView = gameView;
        this.resultRecorder = Objects.requireNonNull(resultRecorder, "resultRecorder");
        this.achievementsVisible = Objects.requireNonNull(
                achievementsVisible, "achievementsVisible");
        this.mainMenuAllowed = Objects.requireNonNull(mainMenuAllowed, "mainMenuAllowed");
        scoreText = activity.findViewById(R.id.score_text);
        finalScoreText = activity.findViewById(R.id.final_score_text);
        stateOverlay = activity.findViewById(R.id.state_overlay);
        readyPanel = activity.findViewById(R.id.ready_panel);
        pausePanel = activity.findViewById(R.id.pause_panel);
        gameOverPanel = activity.findViewById(R.id.game_over_panel);
        pauseButton = activity.findViewById(R.id.pause_button);
    }

    public void bind() {
        if (bound) return;
        bound = true;
        activity.findViewById(R.id.start_button).setOnClickListener(view -> start());
        pauseButton.setOnClickListener(view -> pause());
        activity.findViewById(R.id.resume_button).setOnClickListener(view -> resume());
        activity.findViewById(R.id.restart_button).setOnClickListener(view -> restart());
        activity.findViewById(R.id.pause_main_menu_button).setOnClickListener(view -> mainMenu());
        activity.findViewById(R.id.play_again_button).setOnClickListener(view -> playAgain());
        activity.findViewById(R.id.game_over_main_menu_button)
                .setOnClickListener(view -> mainMenu());
    }

    public void refresh(GameSnapshot snapshot) {
        resultRecorder.observe(snapshot);
        GameState state = snapshot.getState();
        scoreText.setText(activity.getString(R.string.score_format, snapshot.getScore()));
        finalScoreText.setText(activity.getString(R.string.final_score_format, snapshot.getScore()));
        readyPanel.setVisibility(
                state == GameState.READY && !achievementsVisible.getAsBoolean()
                        && mainMenuAllowed.getAsBoolean()
                        ? View.VISIBLE : View.GONE);
        pausePanel.setVisibility(state == GameState.PAUSED ? View.VISIBLE : View.GONE);
        gameOverPanel.setVisibility(state == GameState.GAME_OVER ? View.VISIBLE : View.GONE);
        pauseButton.setVisibility(state == GameState.PLAYING ? View.VISIBLE : View.GONE);
        stateOverlay.setVisibility(state == GameState.PLAYING ? View.GONE : View.VISIBLE);
    }

    private void start() {
        if (engine.getState() != GameState.READY || !mainMenuAllowed.getAsBoolean()) return;
        engine.startNewRun();
        gameView.start();
        gameView.refreshSnapshot();
    }

    private void pause() {
        if (engine.getState() != GameState.PLAYING) return;
        engine.pause();
        gameView.stop();
        gameView.refreshSnapshot();
    }

    private void resume() {
        if (engine.getState() != GameState.PAUSED) return;
        engine.resume();
        gameView.start();
        gameView.refreshSnapshot();
    }

    private void restart() {
        if (engine.getState() != GameState.PAUSED) return;
        engine.startNewRun();
        gameView.start();
        gameView.refreshSnapshot();
    }

    private void playAgain() {
        if (engine.getState() != GameState.GAME_OVER) return;
        engine.startNewRun();
        gameView.start();
        gameView.refreshSnapshot();
    }

    private void mainMenu() {
        GameState state = engine.getState();
        if (state != GameState.PAUSED && state != GameState.GAME_OVER) return;
        engine.returnToReady();
        gameView.stop();
        gameView.refreshSnapshot();
    }
}
