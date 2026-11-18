package com.vanvinh2402.tiltescape.ui;

import android.app.Activity;
import android.view.View;

import com.vanvinh2402.tiltescape.R;
import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameState;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class HowToPlayUiController {
    private final GameEngine engine;
    private final BooleanSupplier mainMenuAllowed;
    private final Runnable startAction;
    private final View panel;
    private final View readyPanel;
    private boolean bound;

    public HowToPlayUiController(Activity activity, GameEngine engine,
            BooleanSupplier mainMenuAllowed, Runnable startAction) {
        Objects.requireNonNull(activity, "activity");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.mainMenuAllowed = Objects.requireNonNull(mainMenuAllowed, "mainMenuAllowed");
        this.startAction = Objects.requireNonNull(startAction, "startAction");
        panel = activity.findViewById(R.id.how_to_play_panel);
        readyPanel = activity.findViewById(R.id.ready_panel);
    }

    public void bind() {
        if (bound) return;
        bound = true;
        panel.getRootView().findViewById(R.id.how_to_play_button)
                .setOnClickListener(view -> open());
        panel.getRootView().findViewById(R.id.how_to_play_start_button)
                .setOnClickListener(view -> start());
        panel.getRootView().findViewById(R.id.how_to_play_back_button)
                .setOnClickListener(view -> close());
    }

    public void open() {
        if (engine.getState() != GameState.READY || !mainMenuAllowed.getAsBoolean() || isVisible()) {
            return;
        }
        readyPanel.setVisibility(View.GONE);
        panel.setVisibility(View.VISIBLE);
    }

    public void close() {
        if (!isVisible()) return;
        panel.setVisibility(View.GONE);
        if (engine.getState() == GameState.READY && mainMenuAllowed.getAsBoolean()) {
            readyPanel.setVisibility(View.VISIBLE);
        }
    }

    public boolean isVisible() {
        return panel.getVisibility() == View.VISIBLE;
    }

    public boolean handleBack() {
        if (!isVisible()) return false;
        close();
        return true;
    }

    private void start() {
        if (!isVisible() || engine.getState() != GameState.READY || !mainMenuAllowed.getAsBoolean()) {
            return;
        }
        panel.setVisibility(View.GONE);
        startAction.run();
    }
}
