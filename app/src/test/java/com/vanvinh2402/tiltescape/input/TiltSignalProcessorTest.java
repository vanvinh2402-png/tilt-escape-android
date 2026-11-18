package com.vanvinh2402.tiltescape.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TiltSignalProcessorTest {
    private static final float GRAVITY = 9.80665f;
    private static final float EPSILON = 0.0001f;

    @Test
    public void firstValidSampleInitializesImmediatelyWithCorrectDirection() {
        TiltSignalProcessor processor = new TiltSignalProcessor();

        processor.onSample(-0.34f * GRAVITY, 1_000_000_000L);

        assertEquals(0.50f, processor.getHorizontalInput(), 0.001f);

        processor.reset();
        processor.onSample(0.34f * GRAVITY, 2_000_000_000L);
        assertEquals(-0.50f, processor.getHorizontalInput(), 0.001f);
    }

    @Test
    public void deadZoneSuppressesSmallHandMovement() {
        TiltSignalProcessor processor = new TiltSignalProcessor();

        processor.onSample(-0.07f * GRAVITY, 1_000_000_000L);

        assertEquals(0.0f, processor.getHorizontalInput(), EPSILON);
    }

    @Test
    public void fullTiltAndLargerSamplesStayClamped() {
        TiltSignalProcessor processor = new TiltSignalProcessor();
        processor.onSample(-0.60f * GRAVITY, 1_000_000_000L);
        assertEquals(1.0f, processor.getHorizontalInput(), EPSILON);

        processor.reset();
        processor.onSample(2.0f * GRAVITY, 2_000_000_000L);
        assertEquals(-1.0f, processor.getHorizontalInput(), EPSILON);
    }

    @Test
    public void filteringDependsOnElapsedTimeNotSampleCount() {
        TiltSignalProcessor tenFastSamples = new TiltSignalProcessor();
        TiltSignalProcessor fiveSlowSamples = new TiltSignalProcessor();
        long start = 1_000_000_000L;
        tenFastSamples.onSample(0.0f, start);
        fiveSlowSamples.onSample(0.0f, start);

        for (int i = 1; i <= 10; i++) {
            tenFastSamples.onSample(-0.60f * GRAVITY, start + i * 20_000_000L);
        }
        for (int i = 1; i <= 5; i++) {
            fiveSlowSamples.onSample(-0.60f * GRAVITY, start + i * 40_000_000L);
        }

        assertEquals(
                tenFastSamples.getHorizontalInput(),
                fiveSlowSamples.getHorizontalInput(),
                0.001f);
    }

    @Test
    public void invalidSamplesAndTimestampsPreserveLastValidOutput() {
        TiltSignalProcessor processor = new TiltSignalProcessor();
        processor.onSample(-0.34f * GRAVITY, 1_000_000_000L);
        float valid = processor.getHorizontalInput();

        processor.onSample(Float.NaN, 1_100_000_000L);
        processor.onSample(Float.POSITIVE_INFINITY, 1_200_000_000L);
        processor.onSample(0.0f, 1_000_000_000L);
        processor.onSample(0.0f, 900_000_000L);

        assertEquals(valid, processor.getHorizontalInput(), EPSILON);
        assertTrue(Float.isFinite(processor.getHorizontalInput()));
    }

    @Test
    public void resetReturnsToNeutralAndAcceptsNewTimeline() {
        TiltSignalProcessor processor = new TiltSignalProcessor();
        processor.onSample(-0.60f * GRAVITY, 5_000_000_000L);

        processor.reset();

        assertEquals(0.0f, processor.getHorizontalInput(), EPSILON);
        processor.onSample(0.0f, 1L);
        assertEquals(0.0f, processor.getHorizontalInput(), EPSILON);
    }

    @Test
    public void sensitivityChangesResponseButKeepsItClamped() {
        TiltSignalProcessor processor = new TiltSignalProcessor();
        processor.onSample(-0.34f * GRAVITY, 1_000_000_000L);
        float medium = processor.getHorizontalInput();

        processor.setSensitivityMultiplier(TiltSensitivity.HIGH.getMultiplier());
        float high = processor.getHorizontalInput();
        processor.setSensitivityMultiplier(TiltSensitivity.LOW.getMultiplier());
        float low = processor.getHorizontalInput();

        assertTrue(high > medium);
        assertTrue(low < medium);
        processor.setSensitivityMultiplier(100.0f);
        assertTrue(processor.getHorizontalInput() <= 1.0f);
        assertTrue(processor.getHorizontalInput() >= -1.0f);
    }
}
