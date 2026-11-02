# SQLite database schema

Tilt Escape stores completed local runs in Android's app-private SQLite storage.

## Database identity

- File: `tilt_escape.db`
- Schema version: `1`
- Owner: `TiltEscapeDatabaseHelper`
- Retention: all completed runs remain until the user clears app data or uninstalls the app.

Version 1 has no migration from an older application schema. Future changes must increment the database version and implement an explicit, non-destructive migration in `onUpgrade`; player history must not be dropped as an upgrade shortcut.

## Table and indexes

```sql
CREATE TABLE IF NOT EXISTS game_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    score INTEGER NOT NULL CHECK(score >= 0),
    survival_millis INTEGER NOT NULL CHECK(survival_millis >= 0),
    played_at_epoch_millis INTEGER NOT NULL CHECK(played_at_epoch_millis >= 0)
);

CREATE INDEX IF NOT EXISTS idx_game_results_played_at
ON game_results(played_at_epoch_millis DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_game_results_score
ON game_results(score DESC);
```

| Column | Unit | Meaning |
| --- | --- | --- |
| `id` | integer | Auto-incremented local identifier; also provides deterministic ordering when timestamps tie. |
| `score` | obstacle count | Number of obstacles safely passed during the completed run. |
| `survival_millis` | milliseconds | Engine survival duration when the run reached Game Over. |
| `played_at_epoch_millis` | UTC epoch milliseconds | Device time captured when Game Over was first observed. Display formatting uses the device locale and timezone. |

Zero-point runs are valid and are retained. A run is inserted exactly once on the first observed transition from Playing to Game Over; Pause, Resume, Ready, app backgrounding, repeated Game Over refreshes, and opening Thành tích do not insert rows.

## Read queries

All-time high score:

```sql
SELECT COALESCE(MAX(score), 0) AS high_score
FROM game_results;
```

Ten most recent runs:

```sql
SELECT id, score, survival_millis, played_at_epoch_millis
FROM game_results
ORDER BY played_at_epoch_millis DESC, id DESC
LIMIT 10;
```

SQLite operations are serialized on one background executor. This guarantees that opening Thành tích immediately after Game Over observes the insert queued immediately before its queries. Database failures are logged and converted into a recoverable UI error; they do not stop gameplay.
