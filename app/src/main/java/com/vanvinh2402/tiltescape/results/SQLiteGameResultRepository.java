package com.vanvinh2402.tiltescape.results;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
    public long insert(GameResult result) {
        validate(result);
        ContentValues values = new ContentValues();
        values.put("score", result.getScore());
        values.put("survival_millis", result.getSurvivalMillis());
        values.put("played_at_epoch_millis", result.getPlayedAtEpochMillis());
        return databaseHelper.getWritableDatabase().insertOrThrow(
                TiltEscapeDatabaseHelper.TABLE_GAME_RESULTS, null, values);
    }

    @Override
    public int getHighScore() {
        try (Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                "SELECT COALESCE(MAX(score), 0) AS high_score FROM game_results", null)) {
            if (!cursor.moveToFirst()) return 0;
            return cursor.getInt(cursor.getColumnIndexOrThrow("high_score"));
        }
    }

    @Override
    public List<GameResult> getRecent(int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
        List<GameResult> results = new ArrayList<>();
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        try (Cursor cursor = database.query(
                TiltEscapeDatabaseHelper.TABLE_GAME_RESULTS,
                RESULT_COLUMNS,
                null,
                null,
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
}
