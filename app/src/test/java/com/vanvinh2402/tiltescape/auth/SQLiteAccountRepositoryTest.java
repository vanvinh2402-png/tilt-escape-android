package com.vanvinh2402.tiltescape.auth;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import com.vanvinh2402.tiltescape.data.TiltEscapeDatabaseHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class SQLiteAccountRepositoryTest {
    private Context context;
    private SQLiteAccountRepository repository;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase(TiltEscapeDatabaseHelper.DATABASE_NAME);
        repository = new SQLiteAccountRepository(context);
    }

    @After
    public void tearDown() {
        if (repository != null) repository.close();
        context.deleteDatabase(TiltEscapeDatabaseHelper.DATABASE_NAME);
    }

    @Test
    public void insertAndReadByIdAndNormalizedUsernamePreservesEveryField() {
        PasswordHash passwordHash = new PasswordHash(
                "PBKDF2WithHmacSHA256", 120_000,
                new byte[] {1, 2, 3}, new byte[] {4, 5, 6});

        long id = repository.insert(
                "Player_01", "player_01", passwordHash, 1_793_610_000_000L);

        assertTrue(id > 0L);
        assertRecord(repository.findById(id), id);
        assertRecord(repository.findByNormalizedUsername("player_01"), id);
    }

    @Test
    public void duplicateNormalizedUsernameIsRejectedByUniqueIndex() {
        PasswordHash hash = passwordHash();
        repository.insert("Player", "player", hash, 1L);

        try {
            repository.insert("player", "player", hash, 2L);
            fail("Expected SQLiteConstraintException");
        } catch (SQLiteConstraintException expected) {
            // The database remains the final concurrency-safe uniqueness boundary.
        }
    }

    @Test
    public void absentAndNullLookupsReturnNull() {
        assertNull(repository.findById(99L));
        assertNull(repository.findByNormalizedUsername("missing"));
        assertNull(repository.findByNormalizedUsername(null));
    }

    @Test
    public void negativeCreationTimeIsRejected() {
        try {
            repository.insert("Player", "player", passwordHash(), -1L);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Repository validation gives a stable error before SQLite access.
        }
    }

    private static void assertRecord(AccountRecord record, long expectedId) {
        assertEquals(expectedId, record.getId());
        assertEquals("Player_01", record.getUsername());
        assertEquals("player_01", record.getNormalizedUsername());
        assertEquals("PBKDF2WithHmacSHA256", record.getPasswordHash().getAlgorithm());
        assertEquals(120_000, record.getPasswordHash().getIterations());
        assertArrayEquals(new byte[] {1, 2, 3}, record.getPasswordHash().getSalt());
        assertArrayEquals(new byte[] {4, 5, 6}, record.getPasswordHash().getHash());
        assertEquals(1_793_610_000_000L, record.getCreatedAtEpochMillis());
    }

    private static PasswordHash passwordHash() {
        return new PasswordHash(
                "PBKDF2WithHmacSHA256", 120_000,
                new byte[] {1, 2, 3}, new byte[] {4, 5, 6});
    }
}
