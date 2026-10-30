package com.vanvinh2402.tiltescape.game;

final class DifficultySettings {
    private final int tier;
    private final float fallSpeed;
    private final float spawnInterval;
    private final int maximumActive;
    DifficultySettings(int tier, float fallSpeed, float spawnInterval, int maximumActive) {
        this.tier=tier; this.fallSpeed=fallSpeed; this.spawnInterval=spawnInterval; this.maximumActive=maximumActive;
    }
    int getTier(){return tier;} float getFallSpeed(){return fallSpeed;}
    float getSpawnInterval(){return spawnInterval;} int getMaximumActive(){return maximumActive;}
}
