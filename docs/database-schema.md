# SQLite database schema

Tilt Escape stores local accounts and completed runs in Android app-private SQLite storage.

## Database identity

- File: `tilt_escape.db`
- Schema version: `2`
- Helper: `com.vanvinh2402.tiltescape.data.TiltEscapeDatabaseHelper`
- Foreign-key enforcement: enabled in `onConfigure`
- Retention: until app data is cleared or the app is uninstalled

## Users

```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    username_normalized TEXT NOT NULL,
    password_hash BLOB NOT NULL,
    password_salt BLOB NOT NULL,
    password_algorithm TEXT NOT NULL,
    password_iterations INTEGER NOT NULL CHECK(password_iterations > 0),
    created_at_epoch_millis INTEGER NOT NULL CHECK(created_at_epoch_millis >= 0)
);

CREATE UNIQUE INDEX idx_users_username_normalized
ON users(username_normalized);
```

`username_normalized` is the `Locale.ROOT` lowercase lookup key, so case variants cannot create duplicate accounts. Password metadata records the PBKDF2 algorithm, iteration count, random salt and derived bytes; plaintext passwords are never stored.

## Game results

```sql
CREATE TABLE game_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    score INTEGER NOT NULL CHECK(score >= 0),
    survival_millis INTEGER NOT NULL CHECK(survival_millis >= 0),
    played_at_epoch_millis INTEGER NOT NULL CHECK(played_at_epoch_millis >= 0),
    user_id INTEGER REFERENCES users(id)
);

CREATE INDEX idx_game_results_played_at
ON game_results(played_at_epoch_millis DESC, id DESC);

CREATE INDEX idx_game_results_score
ON game_results(score DESC);

CREATE INDEX idx_game_results_user_played_at
ON game_results(user_id, played_at_epoch_millis DESC, id DESC);
```

`user_id` is required for all new application writes. It remains nullable only so version-1 rows can survive as legacy Guest history.

Account high score:

```sql
SELECT COALESCE(MAX(score), 0) AS high_score
FROM game_results
WHERE user_id = ?;
```

Account recent history:

```sql
SELECT id, score, survival_millis, played_at_epoch_millis
FROM game_results
WHERE user_id = ?
ORDER BY played_at_epoch_millis DESC, id DESC
LIMIT ?;
```

These queries exclude other accounts and all legacy Guest rows.

## Version 1 to version 2 migration

The upgrade is executed inside the transaction managed by `SQLiteOpenHelper`:

1. Create `users` and its unique normalized-username index.
2. Add nullable `game_results.user_id` referencing `users(id)`.
3. Create `idx_game_results_user_played_at`.
4. Preserve every existing result unchanged with `user_id = NULL`.

The migration never drops or recreates `game_results`. Automated Robolectric migration tests construct a real version-1 database, insert representative rows and verify values, indexes, foreign keys and null legacy ownership after opening version 2.

## Session storage

The active session is separate from SQLite credentials. Private SharedPreferences file `tilt_escape_session` stores only the positive numeric `active_user_id`. Logout removes this value without deleting the account or results.
