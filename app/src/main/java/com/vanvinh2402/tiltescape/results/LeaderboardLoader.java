package com.vanvinh2402.tiltescape.results;

import java.util.List;

public interface LeaderboardLoader {
    void loadLeaderboard(int limit, Callback callback);

    interface Callback {
        void onLoaded(List<LeaderboardEntry> entries);
        void onError();
    }
}
