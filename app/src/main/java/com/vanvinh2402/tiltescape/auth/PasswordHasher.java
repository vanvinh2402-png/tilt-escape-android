package com.vanvinh2402.tiltescape.auth;

import android.os.Build;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Objects;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    public static final int DEFAULT_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String SHA256_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String SHA1_ALGORITHM = "PBKDF2WithHmacSHA1";

    private final SecureRandom secureRandom;
    private final String algorithm;
    private final int iterations;

    public PasswordHasher() {
        this(new SecureRandom(), Build.VERSION.SDK_INT, DEFAULT_ITERATIONS);
    }

    PasswordHasher(SecureRandom secureRandom, int apiLevel, int iterations) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        this.iterations = iterations;
        this.algorithm = apiLevel >= Build.VERSION_CODES.O ? SHA256_ALGORITHM : SHA1_ALGORITHM;
    }

    public PasswordHash hash(char[] password) {
        Objects.requireNonNull(password, "password");
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return new PasswordHash(algorithm, iterations, salt,
                derive(password, algorithm, iterations, salt));
    }

    public boolean verify(char[] password, PasswordHash stored) {
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(stored, "stored");
        byte[] candidate = derive(password, stored.getAlgorithm(), stored.getIterations(),
                stored.getSalt());
        return MessageDigest.isEqual(candidate, stored.getHash());
    }

    private byte[] derive(char[] password, String hashAlgorithm, int hashIterations, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, hashIterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(hashAlgorithm).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to hash password", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
