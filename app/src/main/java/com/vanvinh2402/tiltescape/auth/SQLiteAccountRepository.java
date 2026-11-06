package com.vanvinh2402.tiltescape.auth;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vanvinh2402.tiltescape.data.TiltEscapeDatabaseHelper;

import java.util.Objects;

public final class SQLiteAccountRepository implements AccountRepository {
    private static final String[] ACCOUNT_COLUMNS = {
            "id",
            "username",
            "username_normalized",
            "password_hash",
            "password_salt",
            "password_algorithm",
            "password_iterations",
            "created_at_epoch_millis"
    };

    private final TiltEscapeDatabaseHelper databaseHelper;

    public SQLiteAccountRepository(Context context) {
        databaseHelper = new TiltEscapeDatabaseHelper(context);
    }

    @Override
    public long insert(
            String username,
            String normalizedUsername,
            PasswordHash passwordHash,
            long createdAtEpochMillis) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(normalizedUsername, "normalizedUsername");
        Objects.requireNonNull(passwordHash, "passwordHash");
        if (createdAtEpochMillis < 0L) {
            throw new IllegalArgumentException("createdAtEpochMillis must be non-negative");
        }

        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("username_normalized", normalizedUsername);
        values.put("password_hash", passwordHash.getHash());
        values.put("password_salt", passwordHash.getSalt());
        values.put("password_algorithm", passwordHash.getAlgorithm());
        values.put("password_iterations", passwordHash.getIterations());
        values.put("created_at_epoch_millis", createdAtEpochMillis);
        return databaseHelper.getWritableDatabase().insertOrThrow(
                TiltEscapeDatabaseHelper.TABLE_USERS, null, values);
    }

    @Override
    public AccountRecord findByNormalizedUsername(String normalizedUsername) {
        if (normalizedUsername == null) return null;
        return queryOne("username_normalized = ?", new String[] {normalizedUsername});
    }

    @Override
    public AccountRecord findById(long id) {
        return queryOne("id = ?", new String[] {Long.toString(id)});
    }

    @Override
    public void close() {
        databaseHelper.close();
    }

    private AccountRecord queryOne(String selection, String[] selectionArgs) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        try (Cursor cursor = database.query(
                TiltEscapeDatabaseHelper.TABLE_USERS,
                ACCOUNT_COLUMNS,
                selection,
                selectionArgs,
                null,
                null,
                null,
                "1")) {
            if (!cursor.moveToFirst()) return null;
            PasswordHash passwordHash = new PasswordHash(
                    cursor.getString(cursor.getColumnIndexOrThrow("password_algorithm")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("password_iterations")),
                    cursor.getBlob(cursor.getColumnIndexOrThrow("password_salt")),
                    cursor.getBlob(cursor.getColumnIndexOrThrow("password_hash")));
            return new AccountRecord(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cursor.getString(cursor.getColumnIndexOrThrow("username_normalized")),
                    passwordHash,
                    cursor.getLong(cursor.getColumnIndexOrThrow(
                            "created_at_epoch_millis")));
        }
    }
}
