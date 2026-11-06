package com.vanvinh2402.tiltescape.game;

import com.vanvinh2402.tiltescape.results.GameResult;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

public final class GameResultRecorder {
    private final BiConsumer<Long, GameResult> resultSink;
    private final LongSupplier activeUserId;
    private final LongSupplier clock;
    private boolean armed;
    private long runUserId;

    public GameResultRecorder(BiConsumer<Long, GameResult> resultSink,
            LongSupplier activeUserId, LongSupplier clock) {
        this.resultSink = Objects.requireNonNull(resultSink, "resultSink");
        this.activeUserId = Objects.requireNonNull(activeUserId, "activeUserId");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void observe(GameSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.getState() == GameState.PLAYING) {
            if (!armed) {
                runUserId = activeUserId.getAsLong();
                if (runUserId <= 0L) {
                    throw new IllegalStateException("A run requires an authenticated user");
                }
            }
            armed = true;
            return;
        }
        if (snapshot.getState() == GameState.READY) {
            armed = false;
            runUserId = 0L;
            return;
        }
        if (snapshot.getState() != GameState.GAME_OVER || !armed) return;

        armed = false;
        float seconds = snapshot.getSurvivalSeconds();
        long survivalMillis = Float.isFinite(seconds) && seconds > 0.0f
                ? Math.round(seconds * 1_000.0f)
                : 0L;
        long playedAt = Math.max(0L, clock.getAsLong());
        long owner = runUserId;
        runUserId = 0L;
        resultSink.accept(owner, new GameResult(
                0L,
                snapshot.getScore(),
                survivalMillis,
                playedAt));
    }
}
