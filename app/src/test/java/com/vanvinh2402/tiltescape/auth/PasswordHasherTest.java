package com.vanvinh2402.tiltescape.auth;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.security.SecureRandom;

import org.junit.Test;

public class PasswordHasherTest {
    private static final int ITERATIONS = 1_000;

    @Test
    public void hashesWithUniqueSixteenByteSaltsAndThirtyTwoByteKeys() {
        PasswordHasher hasher = new PasswordHasher(new IncrementingSecureRandom(), 26, ITERATIONS);

        PasswordHash first = hasher.hash("secret1".toCharArray());
        PasswordHash second = hasher.hash("secret1".toCharArray());

        assertEquals("PBKDF2WithHmacSHA256", first.getAlgorithm());
        assertEquals(ITERATIONS, first.getIterations());
        assertEquals(16, first.getSalt().length);
        assertEquals(32, first.getHash().length);
        assertFalse(java.util.Arrays.equals(first.getSalt(), second.getSalt()));
        assertFalse(java.util.Arrays.equals(first.getHash(), second.getHash()));
    }

    @Test
    public void verifiesOnlyTheCorrectPassword() {
        PasswordHasher hasher = new PasswordHasher(new IncrementingSecureRandom(), 26, ITERATIONS);
        PasswordHash stored = hasher.hash("correct horse".toCharArray());

        assertTrue(hasher.verify("correct horse".toCharArray(), stored));
        assertFalse(hasher.verify("wrong battery".toCharArray(), stored));
    }

    @Test
    public void usesSha1FallbackOnAndroidApiTwentyFourAndTwentyFive() {
        PasswordHasher api24 = new PasswordHasher(new IncrementingSecureRandom(), 24, ITERATIONS);
        PasswordHasher api25 = new PasswordHasher(new IncrementingSecureRandom(), 25, ITERATIONS);

        PasswordHash hash24 = api24.hash("secret1".toCharArray());
        PasswordHash hash25 = api25.hash("secret1".toCharArray());

        assertEquals("PBKDF2WithHmacSHA1", hash24.getAlgorithm());
        assertEquals("PBKDF2WithHmacSHA1", hash25.getAlgorithm());
        assertTrue(api24.verify("secret1".toCharArray(), hash24));
        assertTrue(api25.verify("secret1".toCharArray(), hash25));
    }

    @Test
    public void passwordHashDefensivelyCopiesSensitiveBytes() {
        byte[] salt = new byte[] {1, 2, 3};
        byte[] key = new byte[] {4, 5, 6};
        PasswordHash stored = new PasswordHash("PBKDF2WithHmacSHA256", ITERATIONS, salt, key);

        salt[0] = 9;
        key[0] = 9;
        byte[] returnedSalt = stored.getSalt();
        byte[] returnedKey = stored.getHash();
        returnedSalt[1] = 9;
        returnedKey[1] = 9;

        assertArrayEquals(new byte[] {1, 2, 3}, stored.getSalt());
        assertArrayEquals(new byte[] {4, 5, 6}, stored.getHash());
        assertNotEquals(returnedSalt[1], stored.getSalt()[1]);
        assertNotEquals(returnedKey[1], stored.getHash()[1]);
    }

    private static final class IncrementingSecureRandom extends SecureRandom {
        private int nextValue = 1;

        @Override
        public void nextBytes(byte[] bytes) {
            for (int index = 0; index < bytes.length; index++) {
                bytes[index] = (byte) nextValue++;
            }
        }
    }
}
