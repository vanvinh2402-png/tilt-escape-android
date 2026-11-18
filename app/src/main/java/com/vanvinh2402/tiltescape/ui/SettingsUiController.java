package com.vanvinh2402.tiltescape.ui;

import android.app.Activity;
import android.view.View;
import android.widget.RadioGroup;

import com.vanvinh2402.tiltescape.R;
import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameState;
import com.vanvinh2402.tiltescape.input.TiltSensitivity;
import com.vanvinh2402.tiltescape.input.TiltSettingsStore;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class SettingsUiController {
    private final GameEngine engine;
    private final BooleanSupplier mainMenuAllowed;
    private final TiltSettingsStore store;
    private final Consumer<TiltSensitivity> applySensitivity;
    private final View panel;
    private final View readyPanel;
    private final RadioGroup group;

    public SettingsUiController(Activity activity, GameEngine engine, BooleanSupplier mainMenuAllowed,
            TiltSettingsStore store, Consumer<TiltSensitivity> applySensitivity) {
        Objects.requireNonNull(activity, "activity");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.mainMenuAllowed = Objects.requireNonNull(mainMenuAllowed, "mainMenuAllowed");
        this.store = Objects.requireNonNull(store, "store");
        this.applySensitivity = Objects.requireNonNull(applySensitivity, "applySensitivity");
        panel = activity.findViewById(R.id.settings_panel);
        readyPanel = activity.findViewById(R.id.ready_panel);
        group = activity.findViewById(R.id.sensitivity_group);
        activity.findViewById(R.id.settings_button).setOnClickListener(view -> open());
        activity.findViewById(R.id.settings_back_button).setOnClickListener(view -> close());
    }

    public void bind() {
        group.setOnCheckedChangeListener((ignored, checkedId) -> {
            TiltSensitivity sensitivity = sensitivityForId(checkedId);
            if (sensitivity == null || !isVisible()) return;
            store.saveSensitivity(sensitivity);
            applySensitivity.accept(sensitivity);
        });
    }

    public void open() {
        if (engine.getState() != GameState.READY || !mainMenuAllowed.getAsBoolean() || isVisible()) return;
        group.setOnCheckedChangeListener(null);
        group.check(idFor(store.getSensitivity()));
        bind();
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

    public boolean isVisible() { return panel.getVisibility() == View.VISIBLE; }
    public boolean handleBack() { if (!isVisible()) return false; close(); return true; }

    private int idFor(TiltSensitivity sensitivity) {
        switch (sensitivity) {
            case LOW: return R.id.sensitivity_low;
            case HIGH: return R.id.sensitivity_high;
            default: return R.id.sensitivity_medium;
        }
    }

    private TiltSensitivity sensitivityForId(int id) {
        if (id == R.id.sensitivity_low) return TiltSensitivity.LOW;
        if (id == R.id.sensitivity_high) return TiltSensitivity.HIGH;
        if (id == R.id.sensitivity_medium) return TiltSensitivity.MEDIUM;
        return null;
    }
}
