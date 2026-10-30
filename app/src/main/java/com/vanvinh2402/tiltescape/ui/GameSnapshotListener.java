package com.vanvinh2402.tiltescape.ui;

import com.vanvinh2402.tiltescape.game.GameSnapshot;

public interface GameSnapshotListener {
    void onSnapshotChanged(GameSnapshot snapshot);
}
