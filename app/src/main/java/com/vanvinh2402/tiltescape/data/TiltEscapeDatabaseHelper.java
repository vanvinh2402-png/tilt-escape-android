package com.vanvinh2402.tiltescape.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class TiltEscapeDatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "tilt_escape.db";
    public static final int DATABASE_VERSION = 2;
    public static final String TABLE_USERS = "users";
    public static final String TABLE_GAME_RESULTS = "game_results";
    public static final String COLUMN_USER_ID = "user_id";

    private static final String CREATE_USERS =
            "CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT NOT NULL, "
                    + "username_normalized TEXT NOT NULL, "
                    + "password_hash BLOB NOT NULL, "
                    + "password_salt BLOB NOT NULL, "
                    + "password_algorithm TEXT NOT NULL, "
                    + "password_iterations INTEGER NOT NULL CHECK(password_iterations > 0), "
                    + "created_at_epoch_millis INTEGER NOT NULL "
                    + "CHECK(created_at_epoch_millis >= 0))";
    private static final String CREATE_USERNAME_INDEX =
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_normalized "
                    + "ON users(username_normalized)";
    private static final String CREATE_GAME_RESULTS =
            "CREATE TABLE IF NOT EXISTS game_results ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "score INTEGER NOT NULL CHECK(score >= 0), "
                    + "survival_millis INTEGER NOT NULL CHECK(survival_millis >= 0), "
                    + "played_at_epoch_millis INTEGER NOT NULL "
                    + "CHECK(played_at_epoch_millis >= 0), "
                    + "user_id INTEGER REFERENCES users(id))";
    private static final String CREATE_PLAYED_AT_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_game_results_played_at "
                    + "ON game_results(played_at_epoch_millis DESC, id DESC)";
    private static final String CREATE_SCORE_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_game_results_score "
                    + "ON game_results(score DESC)";
    private static final String CREATE_USER_PLAYED_AT_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_game_results_user_played_at "
                    + "ON game_results(user_id, played_at_epoch_millis DESC, id DESC)";

    public TiltEscapeDatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase database) {
        super.onConfigure(database);
        database.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        createUsers(database);
        database.execSQL(CREATE_GAME_RESULTS);
        createResultIndexes(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion == newVersion) return;
        if (oldVersion == 1 && newVersion == 2) {
            createUsers(database);
            database.execSQL(
                    "ALTER TABLE game_results ADD COLUMN user_id INTEGER REFERENCES users(id)");
            createResultIndexes(database);
            return;
        }
        throw new IllegalStateException(
                "Unsupported database migration from " + oldVersion + " to " + newVersion);
    }

    private static void createUsers(SQLiteDatabase database) {
        database.execSQL(CREATE_USERS);
        database.execSQL(CREATE_USERNAME_INDEX);
    }

    private static void createResultIndexes(SQLiteDatabase database) {
        database.execSQL(CREATE_PLAYED_AT_INDEX);
        database.execSQL(CREATE_SCORE_INDEX);
        database.execSQL(CREATE_USER_PLAYED_AT_INDEX);
    }
}
