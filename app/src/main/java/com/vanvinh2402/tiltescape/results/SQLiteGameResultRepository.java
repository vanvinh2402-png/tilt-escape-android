package com.vanvinh2402.tiltescape.results;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vanvinh2402.tiltescape.data.TiltEscapeDatabaseHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SQLiteGameResultRepository implements GameResultRepository {
    private static final String[] RESULT_COLUMNS = {
            "id", "score", "survival_millis", "played_at_epoch_millis"
    };

    private final TiltEscapeDatabaseHelper databaseHelper;

    public SQLiteGameResultRepository(Context context) {
        databaseHelper = new TiltEscapeDatabaseHelper(context);
    }

    @Override
    public long insert(long userId, GameResult result) {
        validateUserId(userId);
        validate(result);
        ContentValues values = new ContentValues();
        values.put("score", result.getScore());
        values.put("survival_millis", result.getSurvivalMillis());
        values.put("played_at_epoch_millis", result.getPlayedAtEpochMillis());
        values.put("user_id", userId);
        return databaseHelper.getWritableDatabase().insertOrThrow(
                TiltEscapeDatabaseHelper.TABLE_GAME_RESULTS, null, values);
    }

    @Override
    public int getHighScore(long userId) {
        validateUserId(userId);
        try (Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                "SELECT COALESCE(MAX(score), 0) AS high_score FROM game_results "
                        + "WHERE user_id = ?",
                new String[] {Long.toString(userId)})) {
            if (!cursor.moveToFirst()) return 0;
            return cursor.getInt(cursor.getColumnIndexOrThrow("high_score"));
        }
    }

    @Override
    public List<GameResult> getRecent(long userId, int limit) {
        validateUserId(userId);
        if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
        List<GameResult> results = new ArrayList<>();
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        try (Cursor cursor = database.query(
                TiltEscapeDatabaseHelper.TABLE_GAME_RESULTS,
                RESULT_COLUMNS,
                "user_id = ?",
                new String[] {Long.toString(userId)},
                null,
                null,
                "played_at_epoch_millis DESC, id DESC",
                Integer.toString(limit))) {
            int idIndex = cursor.getColumnIndexOrThrow("id");
            int scoreIndex = cursor.getColumnIndexOrThrow("score");
            int survivalIndex = cursor.getColumnIndexOrThrow("survival_millis");
            int playedAtIndex = cursor.getColumnIndexOrThrow("played_at_epoch_millis");
            while (cursor.moveToNext()) {
                results.add(new GameResult(
                        cursor.getLong(idIndex),
                        cursor.getInt(scoreIndex),
                        cursor.getLong(survivalIndex),
                        cursor.getLong(playedAtIndex)));
            }
        }
        return Collections.unmodifiableList(results);
    }

    @Override
    public List<LeaderboardEntry> getLeaderboard(int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
        List<LeaderboardEntry> entries = new ArrayList<>();
        String sql = "SELECT users.username AS username, MAX(game_results.score) AS high_score "
                + "FROM game_results INNER JOIN users ON users.id = game_results.user_id "
                + "GROUP BY users.id, users.username, users.username_normalized "
                + "ORDER BY high_score DESC, users.username_normalized ASC LIMIT ?";
        try (Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                sql, new String[] {Integer.toString(limit)})) {
            int usernameIndex = cursor.getColumnIndexOrThrow("username");
            int scoreIndex = cursor.getColumnIndexOrThrow("high_score");
            while (cursor.moveToNext()) {
                entries.add(new LeaderboardEntry(cursor.getString(usernameIndex), cursor.getInt(scoreIndex)));
            }
        }
        return Collections.unmodifiableList(entries);
    }

    @Override
    public void close() {
        databaseHelper.close();
    }

    private static void validate(GameResult result) {
        if (result == null) throw new IllegalArgumentException("result must not be null");
        if (result.getScore() < 0) throw new IllegalArgumentException("score must be non-negative");
        if (result.getSurvivalMillis() < 0L) {
            throw new IllegalArgumentException("survivalMillis must be non-negative");
        }
        if (result.getPlayedAtEpochMillis() < 0L) {
            throw new IllegalArgumentException("playedAtEpochMillis must be non-negative");
        }
    }

    private static void validateUserId(long userId) {
        if (userId <= 0L) throw new IllegalArgumentException("userId must be positive");
    }
}
