package com.vanvinh2402.tiltescape.game;

import java.util.Random;

final class JavaRandomSource implements RandomSource {
    private final Random random = new Random();

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }
}
