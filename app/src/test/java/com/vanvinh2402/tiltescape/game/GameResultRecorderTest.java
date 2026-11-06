package com.vanvinh2402.tiltescape.game;

import static org.junit.Assert.assertEquals;

import com.vanvinh2402.tiltescape.results.GameResult;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GameResultRecorderTest {
    private static final long PLAYED_AT = 1_793_610_000_000L;

    @Test
    public void playingThenGameOverSavesOneCompleteResult() {
        List<GameResult> saved = new ArrayList<>();
        GameResultRecorder recorder = recorder(saved);

        recorder.observe(snapshot(GameState.PLAYING, 2, 3.0f));
        recorder.observe(snapshot(GameState.GAME_OVER, 3, 4.25f));

        assertEquals(1, saved.size());
        assertEquals(3, saved.get(0).getScore());
        assertEquals(4_250L, saved.get(0).getSurvivalMillis());
        assertEquals(PLAYED_AT, saved.get(0).getPlayedAtEpochMillis());
    }

    @Test
    public void repeatedGameOverSnapshotsSaveOnlyOnce() {
        List<GameResult> saved = new ArrayList<>();
        GameResultRecorder recorder = recorder(saved);

        recorder.observe(snapshot(GameState.PLAYING, 0, 0.0f));
        recorder.observe(snapshot(GameState.GAME_OVER, 1, 1.0f));
        recorder.observe(snapshot(GameState.GAME_OVER, 1, 1.0f));
        recorder.observe(snapshot(GameState.GAME_OVER, 1, 1.0f));

        assertEquals(1, saved.size());
    }

    @Test
    public void pauseResumeReadyAndGameOverWithoutPlayingDoNotSave() {
        List<GameResult> saved = new ArrayList<>();
        GameResultRecorder recorder = recorder(saved);

        recorder.observe(snapshot(GameState.PAUSED, 4, 2.0f));
        recorder.observe(snapshot(GameState.READY, 0, 0.0f));
        recorder.observe(snapshot(GameState.GAME_OVER, 4, 2.0f));
        recorder.observe(snapshot(GameState.PLAYING, 1, 1.0f));
        recorder.observe(snapshot(GameState.PAUSED, 1, 1.0f));
        recorder.observe(snapshot(GameState.READY, 0, 0.0f));
        recorder.observe(snapshot(GameState.GAME_OVER, 1, 1.0f));

        assertEquals(0, saved.size());
    }

    @Test
    public void secondPlayingRunRearmsAndSavesSecondResult() {
        List<GameResult> saved = new ArrayList<>();
        GameResultRecorder recorder = recorder(saved);

        recorder.observe(snapshot(GameState.PLAYING, 0, 0.0f));
        recorder.observe(snapshot(GameState.GAME_OVER, 2, 2.0f));
        recorder.observe(snapshot(GameState.PLAYING, 0, 0.0f));
        recorder.observe(snapshot(GameState.GAME_OVER, 5, 5.0f));

        assertEquals(2, saved.size());
        assertEquals(2, saved.get(0).getScore());
        assertEquals(5, saved.get(1).getScore());
    }

    @Test
    public void runKeepsOwnerCapturedOnFirstPlayingSnapshot() {
        List<Long> owners = new ArrayList<>();
        long[] currentUser = {7L};
        GameResultRecorder recorder = new GameResultRecorder(
                (userId, result) -> owners.add(userId),
                () -> currentUser[0],
                () -> PLAYED_AT);

        recorder.observe(snapshot(GameState.PLAYING, 0, 0.0f));
        currentUser[0] = 9L;
        recorder.observe(snapshot(GameState.PLAYING, 1, 1.0f));
        recorder.observe(snapshot(GameState.GAME_OVER, 2, 2.0f));

        assertEquals(Collections.singletonList(7L), owners);
    }

    @Test
    public void nonFiniteOrNegativeSurvivalIsClampedToZero() {
        List<GameResult> saved = new ArrayList<>();
        GameResultRecorder recorder = recorder(saved);
        float[] invalidValues = {Float.NaN, Float.POSITIVE_INFINITY, -1.0f};

        for (float invalid : invalidValues) {
            recorder.observe(snapshot(GameState.PLAYING, 0, 0.0f));
            recorder.observe(snapshot(GameState.GAME_OVER, 0, invalid));
        }

        assertEquals(3, saved.size());
        assertEquals(0L, saved.get(0).getSurvivalMillis());
        assertEquals(0L, saved.get(1).getSurvivalMillis());
        assertEquals(0L, saved.get(2).getSurvivalMillis());
    }

    private static GameResultRecorder recorder(List<GameResult> saved) {
        return new GameResultRecorder(
                (userId, result) -> saved.add(result), () -> 7L, () -> PLAYED_AT);
    }

    private static GameSnapshot snapshot(GameState state, int score, float survivalSeconds) {
        return new GameSnapshot(
                state,
                new WorldRect(0.0f, 0.0f, 0.1f, 0.1f),
                Collections.emptyList(),
                0.0f,
                survivalSeconds,
                score,
                0);
    }
}
