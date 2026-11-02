package com.vanvinh2402.tiltescape.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public final class ObstacleFieldTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void resetSpawnsOneObstacleInsideWorld() {
        ObstacleField field = new ObstacleField(new SequenceRandomSource(0));
        field.reset();

        List<WorldRect> bounds = field.snapshotBounds();
        assertEquals(1, bounds.size());
        assertTrue(bounds.get(0).getLeft() >= 0.0f);
        assertTrue(bounds.get(0).getRight() <= 1.0f);
    }

    @Test
    public void consecutiveCandidateLaneIsAdvancedAndLaterLaneCanVary() {
        ObstacleField field = new ObstacleField(new SequenceRandomSource(0, 0, 4));
        field.reset();
        field.update(0.30f, 1.0f, 0.10f, 3);
        field.update(0.30f, 1.0f, 0.10f, 3);

        List<WorldRect> bounds = field.snapshotBounds();
        assertEquals(3, bounds.size());
        assertEquals(0.08f, centerX(bounds.get(0)), EPSILON);
        assertEquals(0.29f, centerX(bounds.get(1)), EPSILON);
        assertEquals(0.92f, centerX(bounds.get(2)), EPSILON);
    }

    @Test
    public void doesNotSpawnBeforeIntervalOrWithoutVerticalSeparation() {
        ObstacleField field = new ObstacleField(new SequenceRandomSource(2, 3));
        field.reset();
        field.update(0.50f, 0.0f, 1.0f, 3);
        assertEquals(1, field.snapshotBounds().size());

        field.update(0.60f, 0.0f, 1.0f, 3);
        assertEquals(1, field.snapshotBounds().size());
    }

    @Test
    public void activeCountIsCappedAndBelowWorldObstaclesAreRemoved() {
        ObstacleField capped = new ObstacleField(new SequenceRandomSource(0, 1, 2));
        capped.reset();
        capped.update(0.30f, 1.0f, 0.10f, 2);
        capped.update(0.30f, 1.0f, 0.10f, 2);
        assertEquals(2, capped.snapshotBounds().size());

        ObstacleField removed = new ObstacleField(new SequenceRandomSource(0));
        removed.reset();
        removed.update(2.0f, 1.0f, 100.0f, 1);
        assertEquals(0, removed.snapshotBounds().size());
    }

    @Test
    public void updateReturnsEveryObstaclePassedInThatCall() {
        ObstacleField field = new ObstacleField(new SequenceRandomSource(0, 1));
        field.reset();

        assertEquals(0, field.update(0.30f, 1.0f, 0.10f, 2));
        assertEquals(2, field.snapshotBounds().size());
        assertEquals(2, field.update(2.0f, 1.0f, 100.0f, 2));
        assertEquals(0, field.snapshotBounds().size());
        assertEquals(0, field.update(0.10f, 1.0f, 100.0f, 2));
    }

    @Test
    public void invalidDeltaReturnsZeroWithoutChangingField() {
        ObstacleField field = new ObstacleField(new SequenceRandomSource(0));
        field.reset();
        float initialTop = field.snapshotBounds().get(0).getTop();

        assertEquals(0, field.update(Float.NaN, 1.0f, 0.0f, 2));
        assertEquals(0, field.update(Float.POSITIVE_INFINITY, 1.0f, 0.0f, 2));
        assertEquals(0, field.update(0.0f, 1.0f, 0.0f, 2));
        assertEquals(0, field.update(-1.0f, 1.0f, 0.0f, 2));
        assertEquals(1, field.snapshotBounds().size());
        assertEquals(initialTop, field.snapshotBounds().get(0).getTop(), EPSILON);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void snapshotBoundsCannotBeModified() {
        ObstacleField field = new ObstacleField(new SequenceRandomSource(0));
        field.reset();
        field.snapshotBounds().add(new WorldRect(0, 0, 1, 1));
    }

    private static float centerX(WorldRect rect) {
        return (rect.getLeft() + rect.getRight()) / 2.0f;
    }

    private static final class SequenceRandomSource implements RandomSource {
        private final int[] values;
        private int index;

        SequenceRandomSource(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            int value = values[Math.min(index, values.length - 1)];
            index++;
            return value;
        }
    }
}
