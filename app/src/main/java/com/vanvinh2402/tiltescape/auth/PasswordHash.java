package com.vanvinh2402.tiltescape.auth;

import java.util.Arrays;
import java.util.Objects;

public final class PasswordHash {
    private final String algorithm;
    private final int iterations;
    private final byte[] salt;
    private final byte[] hash;

    public PasswordHash(String algorithm, int iterations, byte[] salt, byte[] hash) {
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        this.iterations = iterations;
        this.salt = Arrays.copyOf(Objects.requireNonNull(salt, "salt"), salt.length);
        this.hash = Arrays.copyOf(Objects.requireNonNull(hash, "hash"), hash.length);
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getIterations() {
        return iterations;
    }

    public byte[] getSalt() {
        return Arrays.copyOf(salt, salt.length);
    }

    public byte[] getHash() {
        return Arrays.copyOf(hash, hash.length);
    }
}
