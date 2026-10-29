package com.vanvinh2402.tiltescape.game;

public final class WorldRect {
    private final float left;
    private final float top;
    private final float right;
    private final float bottom;

    public WorldRect(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public float getLeft() {
        return left;
    }

    public float getTop() {
        return top;
    }

    public float getRight() {
        return right;
    }

    public float getBottom() {
        return bottom;
    }

    public boolean overlaps(WorldRect other) {
        return left < other.right
                && right > other.left
                && top < other.bottom
                && bottom > other.top;
    }
}
