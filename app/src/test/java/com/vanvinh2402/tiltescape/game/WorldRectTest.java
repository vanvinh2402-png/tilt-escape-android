package com.vanvinh2402.tiltescape.game;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class WorldRectTest {

    @Test
    public void overlapRequiresPositiveArea() {
        WorldRect first = new WorldRect(0.10f, 0.10f, 0.40f, 0.40f);
        WorldRect second = new WorldRect(0.30f, 0.20f, 0.60f, 0.50f);

        assertTrue(first.overlaps(second));
        assertTrue(second.overlaps(first));
    }

    @Test
    public void touchingEdgesDoNotOverlap() {
        WorldRect first = new WorldRect(0.10f, 0.10f, 0.30f, 0.30f);
        WorldRect touching = new WorldRect(0.30f, 0.10f, 0.50f, 0.30f);

        assertFalse(first.overlaps(touching));
    }

    @Test
    public void separatedRectanglesDoNotOverlap() {
        WorldRect first = new WorldRect(0.10f, 0.10f, 0.20f, 0.20f);
        WorldRect separated = new WorldRect(0.70f, 0.70f, 0.90f, 0.90f);

        assertFalse(first.overlaps(separated));
    }
}
