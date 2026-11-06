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

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase(TiltEscapeDatabaseHelper.DATABASE_NAME);
        repository = new SQLiteGameResultRepository(context);
    }

    @After
    public void tearDown() {
        if (repository != null) repository.close();
        context.deleteDatabase(TiltEscapeDatabaseHelper.DATABASE_NAME);
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
        repository.insert(new GameResult(0L, 8, 12_500L, 1_793_610_000_000L));
        repository.close();
        repository = new SQLiteGameResultRepository(context);

        GameResult stored = repository.getRecent(1).get(0);
        assertTrue(stored.getId() > 0L);
        assertEquals(8, stored.getScore());
        assertEquals(12_500L, stored.getSurvivalMillis());
        assertEquals(1_793_610_000_000L, stored.getPlayedAtEpochMillis());
    }

    @Test
    public void highScoreUsesAllRowsWhileRecentReturnsOnlyTenNewest() {
        for (int index = 0; index < 12; index++) {
            int score = index == 0 ? 99 : index;
            repository.insert(new GameResult(0L, score, index * 1_000L, 10_000L + index));
        }

        List<GameResult> recent = repository.getRecent(10);
        assertEquals(99, repository.getHighScore());
        assertEquals(10, recent.size());
        assertEquals(11, recent.get(0).getScore());
        assertEquals(2, recent.get(9).getScore());
    }

    @Test
    public void sameTimestampOrdersHigherIdFirst() {
        long firstId = repository.insert(new GameResult(0L, 4, 1_000L, 50_000L));
        long secondId = repository.insert(new GameResult(0L, 7, 2_000L, 50_000L));

        List<GameResult> recent = repository.getRecent(2);
        assertTrue(secondId > firstId);
        assertEquals(secondId, recent.get(0).getId());
        assertEquals(firstId, recent.get(1).getId());
    }

    @Test
    public void zeroScoreIsStoredAndNegativeFieldsAreRejected() {
        repository.insert(new GameResult(0L, 0, 0L, 0L));
        assertEquals(0, repository.getRecent(1).get(0).getScore());

        assertIllegalArgument(() -> repository.insert(new GameResult(0L, -1, 0L, 0L)));
        assertIllegalArgument(() -> repository.insert(new GameResult(0L, 0, -1L, 0L)));
        assertIllegalArgument(() -> repository.insert(new GameResult(0L, 0, 0L, -1L)));
    }

    @Test
    public void nonPositiveRecentLimitIsRejected() {
        assertIllegalArgument(() -> repository.getRecent(0));
        assertIllegalArgument(() -> repository.getRecent(-1));
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
