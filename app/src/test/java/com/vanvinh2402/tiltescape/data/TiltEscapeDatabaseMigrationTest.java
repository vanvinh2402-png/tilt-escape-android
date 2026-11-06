package com.vanvinh2402.tiltescape.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public final class TiltEscapeDatabaseMigrationTest {
    private static final String DATABASE_NAME = "tilt_escape.db";
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase(DATABASE_NAME);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(DATABASE_NAME);
    }

    @Test
    public void versionOneUpgradePreservesRowsAndAddsAccountSchema() {
        LegacyVersionOneHelper legacyHelper = new LegacyVersionOneHelper(context);
        SQLiteDatabase legacyDatabase = legacyHelper.getWritableDatabase();
        insertLegacyResult(legacyDatabase, 0, 0L, 50_000L);
        insertLegacyResult(legacyDatabase, 7, 2_000L, 50_000L);
        legacyHelper.close();

        TiltEscapeDatabaseHelper helper = new TiltEscapeDatabaseHelper(context);
        SQLiteDatabase database = helper.getWritableDatabase();

        assertEquals(2, database.getVersion());
        assertEquals(setOf("id", "score", "survival_millis",
                        "played_at_epoch_millis", "user_id"),
                pragmaNames(database, "PRAGMA table_info(game_results)", "name"));
        assertEquals(setOf("id", "username", "username_normalized", "password_hash",
                        "password_salt", "password_algorithm", "password_iterations",
                        "created_at_epoch_millis"),
                pragmaNames(database, "PRAGMA table_info(users)", "name"));

        try (Cursor cursor = database.rawQuery(
                "SELECT id, score, survival_millis, played_at_epoch_millis, user_id "
                        + "FROM game_results ORDER BY id", null)) {
            assertEquals(2, cursor.getCount());
            assertTrue(cursor.moveToFirst());
            assertEquals(1L, cursor.getLong(0));
            assertEquals(0, cursor.getInt(1));
            assertEquals(0L, cursor.getLong(2));
            assertEquals(50_000L, cursor.getLong(3));
            assertTrue(cursor.isNull(4));
            assertTrue(cursor.moveToNext());
            assertEquals(2L, cursor.getLong(0));
            assertEquals(7, cursor.getInt(1));
            assertEquals(2_000L, cursor.getLong(2));
            assertEquals(50_000L, cursor.getLong(3));
            assertTrue(cursor.isNull(4));
        }

        assertEquals(1, singleInt(database, "PRAGMA foreign_keys"));
        Set<String> resultIndexes =
                pragmaNames(database, "PRAGMA index_list(game_results)", "name");
        assertTrue(resultIndexes.contains("idx_game_results_played_at"));
        assertTrue(resultIndexes.contains("idx_game_results_score"));
        assertTrue(resultIndexes.contains("idx_game_results_user_played_at"));
        assertTrue(pragmaNames(database, "PRAGMA index_list(users)", "name")
                .contains("idx_users_username_normalized"));
        helper.close();
    }

    private static void insertLegacyResult(
            SQLiteDatabase database, int score, long survivalMillis, long playedAt) {
        ContentValues values = new ContentValues();
        values.put("score", score);
        values.put("survival_millis", survivalMillis);
        values.put("played_at_epoch_millis", playedAt);
        database.insertOrThrow("game_results", null, values);
    }

    private static int singleInt(SQLiteDatabase database, String sql) {
        try (Cursor cursor = database.rawQuery(sql, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
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

    private static final class LegacyVersionOneHelper extends SQLiteOpenHelper {
        LegacyVersionOneHelper(Context context) {
            super(context.getApplicationContext(), DATABASE_NAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS game_results ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "score INTEGER NOT NULL CHECK(score >= 0), "
                    + "survival_millis INTEGER NOT NULL CHECK(survival_millis >= 0), "
                    + "played_at_epoch_millis INTEGER NOT NULL "
                    + "CHECK(played_at_epoch_millis >= 0))");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_game_results_played_at "
                    + "ON game_results(played_at_epoch_millis DESC, id DESC)");
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_game_results_score "
                    + "ON game_results(score DESC)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            throw new AssertionError("Legacy helper must never upgrade");
        }
    }
}
