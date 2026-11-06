package com.vanvinh2402.tiltescape.auth;

public interface AccountRepository extends AutoCloseable {
    long insert(
            String username,
            String normalizedUsername,
            PasswordHash passwordHash,
            long createdAtEpochMillis);

    AccountRecord findByNormalizedUsername(String normalizedUsername);

    AccountRecord findById(long id);

    @Override
    void close();
}
