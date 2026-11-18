package com.vanvinh2402.tiltescape.results;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.vanvinh2402.tiltescape.data.TiltEscapeDatabaseHelper;
import com.vanvinh2402.tiltescape.auth.PasswordHash;
import com.vanvinh2402.tiltescape.auth.SQLiteAccountRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public final class SQLiteGameResultRepositoryTest {
    private Context context;
    private SQLiteGameResultRepository repository;
    private SQLiteAccountRepository accountRepository;
    private long userId;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase(TiltEscapeDatabaseHelper.DATABASE_NAME);
        repository = new SQLiteGameResultRepository(context);
        accountRepository = new SQLiteAccountRepository(context);
        userId = accountRepository.insert("Default", "default",
                new PasswordHash("PBKDF2WithHmacSHA256", 1,
                        new byte[] {1}, new byte[] {2}), 0L);
    }

    @After
    public void tearDown() {
        if (repository != null) repository.close();
        if (accountRepository != null) accountRepository.close();
        context.deleteDatabase(TiltEscapeDatabaseHelper.DATABASE_NAME);
    }

    @Test
    public void resultsAreIsolatedByAccountAndExcludeLegacyGuestRows() {
        SQLiteAccountRepository accounts = new SQLiteAccountRepository(context);
        PasswordHash hash = new PasswordHash("PBKDF2WithHmacSHA256", 1,
                new byte[] {1}, new byte[] {2});
        long firstUser = accounts.insert("First", "first", hash, 1L);
        long secondUser = accounts.insert("Second", "second", hash, 2L);

        repository.insert(firstUser, new GameResult(0L, 5, 1_000L, 10_000L));
        repository.insert(firstUser, new GameResult(0L, 12, 2_000L, 20_000L));
        repository.insert(secondUser, new GameResult(0L, 99, 3_000L, 30_000L));
        TiltEscapeDatabaseHelper helper = new TiltEscapeDatabaseHelper(context);
        ContentValues guest = new ContentValues();
        guest.put("score", 500);
        guest.put("survival_millis", 4_000L);
        guest.put("played_at_epoch_millis", 40_000L);
        helper.getWritableDatabase().insertOrThrow("game_results", null, guest);
        helper.close();

        assertEquals(12, repository.getHighScore(firstUser));
        assertEquals(2, repository.getRecent(firstUser, 10).size());
        assertEquals(12, repository.getRecent(firstUser, 10).get(0).getScore());
        assertEquals(99, repository.getHighScore(secondUser));
        assertEquals(1, repository.getRecent(secondUser, 10).size());
        assertIllegalArgument(() ->
                repository.insert(0L, new GameResult(0L, 1, 1L, 1L)));
        assertIllegalArgument(() -> repository.getHighScore(-1L));
        assertIllegalArgument(() -> repository.getRecent(0L, 10));
        accounts.close();
    }

    @Test
    public void createsVersionTwoTablesAndIndexes() {
        TiltEscapeDatabaseHelper helper = new TiltEscapeDatabaseHelper(context);
        SQLiteDatabase database = helper.getWritableDatabase();

        assertEquals(2, database.getVersion());
        assertEquals(setOf("id", "score", "survival_millis", "played_at_epoch_millis", "user_id"),
                pragmaNames(database, "PRAGMA table_info(game_results)", "name"));
        Set<String> indexes = pragmaNames(database, "PRAGMA index_list(game_results)", "name");
        assertTrue(indexes.contains("idx_game_results_played_at"));
        assertTrue(indexes.contains("idx_game_results_score"));
        helper.close();
    }

    @Test
    public void insertAndReopenPreservesEveryField() {
        repository.insert(userId, new GameResult(0L, 8, 12_500L, 1_793_610_000_000L));
        repository.close();
        repository = new SQLiteGameResultRepository(context);

        GameResult stored = repository.getRecent(userId, 1).get(0);
        assertTrue(stored.getId() > 0L);
        assertEquals(8, stored.getScore());
        assertEquals(12_500L, stored.getSurvivalMillis());
        assertEquals(1_793_610_000_000L, stored.getPlayedAtEpochMillis());
    }

    @Test
    public void highScoreUsesAllRowsWhileRecentReturnsOnlyTenNewest() {
        for (int index = 0; index < 12; index++) {
            int score = index == 0 ? 99 : index;
            repository.insert(userId,
                    new GameResult(0L, score, index * 1_000L, 10_000L + index));
        }

        List<GameResult> recent = repository.getRecent(userId, 10);
        assertEquals(99, repository.getHighScore(userId));
        assertEquals(10, recent.size());
        assertEquals(11, recent.get(0).getScore());
        assertEquals(2, recent.get(9).getScore());
    }

    @Test
    public void sameTimestampOrdersHigherIdFirst() {
        long firstId = repository.insert(userId,
                new GameResult(0L, 4, 1_000L, 50_000L));
        long secondId = repository.insert(userId,
                new GameResult(0L, 7, 2_000L, 50_000L));

        List<GameResult> recent = repository.getRecent(userId, 2);
        assertTrue(secondId > firstId);
        assertEquals(secondId, recent.get(0).getId());
        assertEquals(firstId, recent.get(1).getId());
    }

    @Test
    public void zeroScoreIsStoredAndNegativeFieldsAreRejected() {
        repository.insert(userId, new GameResult(0L, 0, 0L, 0L));
        assertEquals(0, repository.getRecent(userId, 1).get(0).getScore());

        assertIllegalArgument(() ->
                repository.insert(userId, new GameResult(0L, -1, 0L, 0L)));
        assertIllegalArgument(() ->
                repository.insert(userId, new GameResult(0L, 0, -1L, 0L)));
        assertIllegalArgument(() ->
                repository.insert(userId, new GameResult(0L, 0, 0L, -1L)));
    }

    @Test
    public void nonPositiveRecentLimitIsRejected() {
        assertIllegalArgument(() -> repository.getRecent(userId, 0));
        assertIllegalArgument(() -> repository.getRecent(userId, -1));
    }

    @Test
    public void leaderboardUsesEachAccountHighScoreInDescendingOrder() {
        long alpha = accountRepository.insert("Alpha", "alpha",
                new PasswordHash("PBKDF2WithHmacSHA256", 1,
                        new byte[] {3}, new byte[] {4}), 1L);
        long bravo = accountRepository.insert("Bravo", "bravo",
                new PasswordHash("PBKDF2WithHmacSHA256", 1,
                        new byte[] {5}, new byte[] {6}), 2L);
        repository.insert(alpha, new GameResult(0L, 4, 1L, 1L));
        repository.insert(alpha, new GameResult(0L, 12, 1L, 2L));
        repository.insert(bravo, new GameResult(0L, 9, 1L, 3L));

        List<LeaderboardEntry> leaderboard = repository.getLeaderboard(10);

        assertEquals(2, leaderboard.size());
        assertEquals("Alpha", leaderboard.get(0).getUsername());
        assertEquals(12, leaderboard.get(0).getScore());
        assertEquals("Bravo", leaderboard.get(1).getUsername());
        assertEquals(9, leaderboard.get(1).getScore());
        assertIllegalArgument(() -> repository.getLeaderboard(0));
    }

    @Test
    public void databaseConstraintsRejectNegativeValues() {
        TiltEscapeDatabaseHelper helper = new TiltEscapeDatabaseHelper(context);
        ContentValues values = new ContentValues();
        values.put("score", -1);
        values.put("survival_millis", 0L);
        values.put("played_at_epoch_millis", 0L);
        try {
            helper.getWritableDatabase().insertOrThrow("game_results", null, values);
            fail("Expected SQLiteConstraintException");
        } catch (SQLiteConstraintException expected) {
            // Constraint is part of the handoff schema, not only repository validation.
        } finally {
            helper.close();
        }
    }

    private static Set<String> pragmaNames(SQLiteDatabase database, String sql, String column) {
        Set<String> values = new HashSet<>();
        try (Cursor cursor = database.rawQuery(sql, null)) {
            int columnIndex = cursor.getColumnIndexOrThrow(column);
            while (cursor.moveToNext()) values.add(cursor.getString(columnIndex));
        }
        return values;
    }

    private static Set<String> setOf(String... values) {
        Set<String> result = new HashSet<>();
        for (String value : values) result.add(value);
        return result;
    }

    private static void assertIllegalArgument(Runnable action) {
        try {
            action.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected validation boundary.
        }
    }
}
