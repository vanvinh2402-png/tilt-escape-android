package com.vanvinh2402.tiltescape.game;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class DifficultyPolicyTest {
    private static final float EPSILON = 0.0001f;
    private final DifficultyPolicy policy = new DifficultyPolicy();

    @Test public void boundariesProduceExpectedSettings() {
        assertSettings(0, .30f, 1.40f, 1, policy.forSurvivalSeconds(0));
        assertSettings(0, .30f, 1.40f, 1, policy.forSurvivalSeconds(14.999f));
        assertSettings(1, .35f, 1.28f, 1, policy.forSurvivalSeconds(15));
        assertSettings(2, .40f, 1.16f, 2, policy.forSurvivalSeconds(30));
        assertSettings(4, .50f, .92f, 3, policy.forSurvivalSeconds(60));
    }

    @Test public void valuesAreCappedAtHighTiers() {
        DifficultySettings value = policy.forSurvivalSeconds(10_000f);
        assertEquals(.65f, value.getFallSpeed(), EPSILON);
        assertEquals(.65f, value.getSpawnInterval(), EPSILON);
        assertEquals(3, value.getMaximumActive());
    }

    @Test public void invalidTimesUseTierZero() {
        assertEquals(0, policy.forSurvivalSeconds(-1).getTier());
        assertEquals(0, policy.forSurvivalSeconds(Float.NaN).getTier());
        assertEquals(0, policy.forSurvivalSeconds(Float.POSITIVE_INFINITY).getTier());
    }

    private static void assertSettings(int tier, float speed, float interval, int maximum,
            DifficultySettings actual) {
        assertEquals(tier, actual.getTier());
        assertEquals(speed, actual.getFallSpeed(), EPSILON);
        assertEquals(interval, actual.getSpawnInterval(), EPSILON);
        assertEquals(maximum, actual.getMaximumActive());
    }
}
