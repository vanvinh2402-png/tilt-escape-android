package com.vanvinh2402.tiltescape.results;

public interface AchievementsLoader {
    void loadAchievements(long userId, GameResultsCoordinator.Callback callback);
}
