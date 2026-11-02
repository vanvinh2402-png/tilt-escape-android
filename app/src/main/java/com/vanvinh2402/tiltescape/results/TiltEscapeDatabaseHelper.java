package com.vanvinh2402.tiltescape.results;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

final class TiltEscapeDatabaseHelper extends SQLiteOpenHelper {
    static final String DATABASE_NAME = "tilt_escape.db";
    static final int DATABASE_VERSION = 1;
    static final String TABLE_GAME_RESULTS = "game_results";

    static final String CREATE_GAME_RESULTS =
            "CREATE TABLE IF NOT EXISTS game_results ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "score INTEGER NOT NULL CHECK(score >= 0), "
                    + "survival_millis INTEGER NOT NULL CHECK(survival_millis >= 0), "
                    + "played_at_epoch_millis INTEGER NOT NULL CHECK(played_at_epoch_millis >= 0))";
    static final String CREATE_PLAYED_AT_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_game_results_played_at "
                    + "ON game_results(played_at_epoch_millis DESC, id DESC)";
    static final String CREATE_SCORE_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_game_results_score "
                    + "ON game_results(score DESC)";

    TiltEscapeDatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_GAME_RESULTS);
        database.execSQL(CREATE_PLAYED_AT_INDEX);
        database.execSQL(CREATE_SCORE_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion == newVersion) return;
        throw new IllegalStateException(
                "Unsupported database migration from " + oldVersion + " to " + newVersion);
    }
}
