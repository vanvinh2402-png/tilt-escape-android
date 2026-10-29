package com.vanvinh2402.tiltescape.game;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class FrameClockTest {
    private static final float EPSILON = 0.000001f;

    @Test
    public void firstFrameHasNoElapsedTime() {
        FrameClock clock = new FrameClock();

        assertEquals(0.0f, clock.nextDeltaSeconds(1_000_000_000L), EPSILON);
    }

    @Test
    public void convertsNanosecondsToSeconds() {
        FrameClock clock = new FrameClock();
        clock.nextDeltaSeconds(1_000_000_000L);

        assertEquals(0.016f, clock.nextDeltaSeconds(1_016_000_000L), EPSILON);
    }

    @Test
    public void rejectsNonIncreasingFrameTimesWithoutMovingBaselineBackwards() {
        FrameClock clock = new FrameClock();
        clock.nextDeltaSeconds(1_000_000_000L);

        assertEquals(0.0f, clock.nextDeltaSeconds(999_000_000L), EPSILON);
        assertEquals(0.010f, clock.nextDeltaSeconds(1_010_000_000L), EPSILON);
    }

    @Test
    public void capsLongFrameGap() {
        FrameClock clock = new FrameClock();
        clock.nextDeltaSeconds(1_000_000_000L);

        assertEquals(0.05f, clock.nextDeltaSeconds(2_000_000_000L), EPSILON);
    }

    @Test
    public void resetMakesNextFrameTheFirstFrameAgain() {
        FrameClock clock = new FrameClock();
        clock.nextDeltaSeconds(1_000_000_000L);
        clock.nextDeltaSeconds(1_010_000_000L);

        clock.reset();

        assertEquals(0.0f, clock.nextDeltaSeconds(2_000_000_000L), EPSILON);
    }
}
