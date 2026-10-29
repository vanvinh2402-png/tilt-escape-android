package com.vanvinh2402.tiltescape.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PlayerMotionTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void neutralInputLeavesResetPlayerStill() {
        PlayerMotion player = new PlayerMotion();

        player.update(0.0f, 0.1f);

        assertEquals(0.50f, player.getCenterX(), EPSILON);
        assertEquals(0.0f, player.getVelocityX(), EPSILON);
    }

    @Test
    public void inputControlsHorizontalDirection() {
        PlayerMotion right = new PlayerMotion();
        PlayerMotion left = new PlayerMotion();

        right.update(1.0f, 0.1f);
        left.update(-1.0f, 0.1f);

        assertTrue(right.getCenterX() > 0.50f);
        assertTrue(right.getVelocityX() > 0.0f);
        assertTrue(left.getCenterX() < 0.50f);
        assertTrue(left.getVelocityX() < 0.0f);
    }

    @Test
    public void neutralInputBrakesPlayerToRest() {
        PlayerMotion player = new PlayerMotion();
        player.update(1.0f, 0.1f);

        player.update(0.0f, 0.1f);

        assertEquals(0.0f, player.getVelocityX(), EPSILON);
    }

    @Test
    public void comparableElapsedTimeProducesComparableDisplacement() {
        PlayerMotion tenSmallSteps = new PlayerMotion();
        PlayerMotion fiveLargeSteps = new PlayerMotion();

        for (int i = 0; i < 10; i++) {
            tenSmallSteps.update(1.0f, 0.01f);
        }
        for (int i = 0; i < 5; i++) {
            fiveLargeSteps.update(1.0f, 0.02f);
        }

        assertEquals(tenSmallSteps.getCenterX(), fiveLargeSteps.getCenterX(), 0.005f);
    }

    @Test
    public void rightBoundaryClampsPositionAndClearsOutwardVelocity() {
        PlayerMotion player = new PlayerMotion();

        for (int i = 0; i < 30; i++) {
            player.update(1.0f, 0.1f);
        }

        assertEquals(1.0f, player.getBounds().getRight(), EPSILON);
        assertEquals(0.0f, player.getVelocityX(), EPSILON);
    }

    @Test
    public void leftBoundaryClampsPositionAndClearsOutwardVelocity() {
        PlayerMotion player = new PlayerMotion();

        for (int i = 0; i < 30; i++) {
            player.update(-1.0f, 0.1f);
        }

        assertEquals(0.0f, player.getBounds().getLeft(), EPSILON);
        assertEquals(0.0f, player.getVelocityX(), EPSILON);
    }

    @Test
    public void invalidOrNonPositiveDeltaDoesNotMovePlayer() {
        PlayerMotion player = new PlayerMotion();
        float initialX = player.getCenterX();

        player.update(1.0f, 0.0f);
        player.update(1.0f, -0.1f);
        player.update(1.0f, Float.NaN);
        player.update(1.0f, Float.POSITIVE_INFINITY);

        assertEquals(initialX, player.getCenterX(), EPSILON);
        assertEquals(0.0f, player.getVelocityX(), EPSILON);
    }
}
