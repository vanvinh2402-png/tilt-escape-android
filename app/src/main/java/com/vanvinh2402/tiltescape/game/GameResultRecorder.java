package com.vanvinh2402.tiltescape.game;

import com.vanvinh2402.tiltescape.results.GameResult;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class GameResultRecorder {
    private final Consumer<GameResult> resultSink;
    private final LongSupplier clock;
    private boolean armed;

    public GameResultRecorder(Consumer<GameResult> resultSink, LongSupplier clock) {
        this.resultSink = Objects.requireNonNull(resultSink, "resultSink");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void observe(GameSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.getState() == GameState.PLAYING) {
            armed = true;
            return;
        }
        if (snapshot.getState() == GameState.READY) {
            armed = false;
            return;
        }
        if (snapshot.getState() != GameState.GAME_OVER || !armed) return;

        armed = false;
        float seconds = snapshot.getSurvivalSeconds();
        long survivalMillis = Float.isFinite(seconds) && seconds > 0.0f
                ? Math.round(seconds * 1_000.0f)
                : 0L;
        long playedAt = Math.max(0L, clock.getAsLong());
        resultSink.accept(new GameResult(
                0L,
                snapshot.getScore(),
                survivalMillis,
                playedAt));
    }
}
