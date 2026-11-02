package com.vanvinh2402.tiltescape.results;

import java.util.List;

public interface GameResultRepository extends AutoCloseable {
    long insert(GameResult result);

    int getHighScore();

    List<GameResult> getRecent(int limit);

    @Override
    void close();
}
