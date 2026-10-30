package com.vanvinh2402.tiltescape.game;

final class DifficultyPolicy {
    DifficultySettings forSurvivalSeconds(float seconds) {
        float safe=Float.isFinite(seconds)&&seconds>0?seconds:0;
        int tier=(int)Math.floor(safe/15.0f);
        return new DifficultySettings(tier, Math.min(.30f+tier*.05f,.65f),
                Math.max(1.40f-tier*.12f,.65f), tier<2?1:tier<4?2:3);
    }
}
