package com.vanvinh2402.tiltescape.results;

import java.util.List;

public interface GameResultRepository extends AutoCloseable {
    long insert(long userId, GameResult result);

    int getHighScore(long userId);

    List<GameResult> getRecent(long userId, int limit);

    List<LeaderboardEntry> getLeaderboard(int limit);

    @Override
    void close();
}
