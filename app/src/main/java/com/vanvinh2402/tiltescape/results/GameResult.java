package com.vanvinh2402.tiltescape.results;

public final class GameResult {
    private final long id;
    private final int score;
    private final long survivalMillis;
    private final long playedAtEpochMillis;

    public GameResult(long id, int score, long survivalMillis, long playedAtEpochMillis) {
        this.id = id;
        this.score = score;
        this.survivalMillis = survivalMillis;
        this.playedAtEpochMillis = playedAtEpochMillis;
    }

    public long getId() {
        return id;
    }

    public int getScore() {
        return score;
    }

    public long getSurvivalMillis() {
        return survivalMillis;
    }

    public long getPlayedAtEpochMillis() {
        return playedAtEpochMillis;
    }
}
