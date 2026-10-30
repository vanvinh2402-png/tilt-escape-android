package com.vanvinh2402.tiltescape.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

final class ObstacleField {
    private static final int LANE_COUNT = 5;
    private static final float SPAWN_CENTER_Y = -Obstacle.HEIGHT / 2.0f;
    private static final float MINIMUM_VERTICAL_SEPARATION = 0.22f;

    private final RandomSource randomSource;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private int previousLane = -1;
    private float timeSinceSpawn;

    ObstacleField(RandomSource randomSource) {
        this.randomSource = randomSource;
    }

    void reset() {
        obstacles.clear();
        timeSinceSpawn = 0.0f;
        spawn();
    }

    void update(float deltaSeconds, float speed, float spawnInterval, int maximumActive) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds <= 0.0f) {
            return;
        }
        Iterator<Obstacle> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Obstacle obstacle = iterator.next();
            obstacle.update(deltaSeconds, speed);
            if (obstacle.isBelowWorld()) {
                iterator.remove();
            }
        }
        timeSinceSpawn += deltaSeconds;
        if (obstacles.size() < Math.max(0, maximumActive)
                && timeSinceSpawn >= Math.max(0.0f, spawnInterval)
                && hasVerticalSeparation()) {
            spawn();
            timeSinceSpawn = 0.0f;
        }
    }

    List<WorldRect> snapshotBounds() {
        List<WorldRect> result = new ArrayList<>(obstacles.size());
        for (Obstacle obstacle : obstacles) {
            result.add(obstacle.getBounds());
        }
        return Collections.unmodifiableList(result);
    }

    private boolean hasVerticalSeparation() {
        for (Obstacle obstacle : obstacles) {
            float centerY = (obstacle.getBounds().getTop() + obstacle.getBounds().getBottom()) / 2.0f;
            if (Math.abs(centerY - SPAWN_CENTER_Y) < MINIMUM_VERTICAL_SEPARATION) {
                return false;
            }
        }
        return true;
    }

    private void spawn() {
        int lane = Math.floorMod(randomSource.nextInt(LANE_COUNT), LANE_COUNT);
        if (lane == previousLane) {
            lane = (lane + 1) % LANE_COUNT;
        }
        previousLane = lane;
        float minimumCenter = Obstacle.WIDTH / 2.0f;
        float laneSpacing = (1.0f - Obstacle.WIDTH) / (LANE_COUNT - 1);
        obstacles.add(new Obstacle(minimumCenter + lane * laneSpacing, SPAWN_CENTER_Y));
    }
}
