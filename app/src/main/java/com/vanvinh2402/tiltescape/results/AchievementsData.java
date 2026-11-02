package com.vanvinh2402.tiltescape.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class AchievementsData {
    private final int highScore;
    private final List<GameResult> recent;

    public AchievementsData(int highScore, List<GameResult> recent) {
        if (highScore < 0) throw new IllegalArgumentException("highScore must be non-negative");
        this.highScore = highScore;
        this.recent = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(recent, "recent")));
    }

    public int getHighScore() {
        return highScore;
    }

    public List<GameResult> getRecent() {
        return recent;
    }
}
