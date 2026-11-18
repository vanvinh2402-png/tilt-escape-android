package com.vanvinh2402.tiltescape.results;

import java.util.Objects;

public final class LeaderboardEntry {
    private final String username;
    private final int score;

    public LeaderboardEntry(String username, int score) {
        this.username = Objects.requireNonNull(username, "username");
        if (score < 0) throw new IllegalArgumentException("score must be non-negative");
        this.score = score;
    }

    public String getUsername() { return username; }
    public int getScore() { return score; }
}
