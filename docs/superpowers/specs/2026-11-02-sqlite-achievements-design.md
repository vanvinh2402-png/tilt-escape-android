# SQLite Achievements Design

## Intent

Tilt Escape currently keeps score only for the active run. Add local, durable game-result storage so a player can see an all-time high score and the ten most recent completed runs after closing and reopening the app. This also supplies the SQLite database and documented data structure required for project handoff.

The feature is for one local player on one Android device. It must not require an account, network connection, or external service.

## Goals

- Persist exactly one result whenever a run transitions from `PLAYING` to `GAME_OVER`.
- Store the final dodge score, survival duration, and completion time.
- Show the all-time high score and the ten most recently completed runs.
- Keep results across activity recreation, app restart, and device restart.
- Keep database work off the frame/UI path so saving a result does not interrupt gameplay rendering.
- Treat storage failures as recoverable: gameplay continues and the achievements screen explains when results cannot be loaded.
- Document the database file, schema, queries, and retention behavior for handoff.

## Non-goals

- Accounts, cloud sync, online leaderboards, sharing, export, or import.
- Ranking players against one another.
- Editing or deleting individual results in the UI.
- Bonuses, combos, difficulty-weighted scores, or changing gameplay scoring.
- Room, an ORM, or a third-party database library.

## Anti-goals

- Do not write to SQLite every frame or every time the score changes.
- Do not let repeated snapshot refreshes insert the same completed run more than once.
- Do not couple `GameEngine` to Android database APIs.
- Do not block or crash the game when SQLite cannot be opened or written.
- Do not call the ten recent runs a leaderboard; they are a local play history ordered by completion time.

## Constraints

- Android application code remains Java.
- The existing single-activity game and overlay structure remains in place.
- The database is device-local and is removed when the app is uninstalled or its data is cleared.
- All completed runs, including zero-point runs, are valid history entries.
- Timestamps are stored as epoch milliseconds and formatted in the device's local timezone only for display.
- Survival duration is stored as integer milliseconds to avoid floating-point values in the schema.

## Approaches Considered

### Selected: `SQLiteOpenHelper` plus a repository

Use Android's built-in `SQLiteOpenHelper` for schema creation and a small repository interface for inserts and queries. This adds no dependency, keeps the SQLite structure explicit for handoff, and fits the single-table feature. Database calls run through one background executor, while UI updates are posted to the main thread.

### Not selected: Room

Room provides compile-time query validation and convenient object mapping, but adds dependencies, annotation processing, and more generated/build configuration than this one-table Java project needs.

### Rejected: SharedPreferences

SharedPreferences could store one high-score integer but is not appropriate for ordered run history and would not satisfy the SQLite delivery requirement.

## User Experience

The approved achievements mockup is stored locally at `.superpowers/brainstorm/achievements-layout.html` as design reference only; it is not shipped in the APK.

The READY panel gains a `THÀNH TÍCH` button below `BẮT ĐẦU`. Selecting it does not start or alter a run. It replaces the READY panel with an achievements panel inside the existing state overlay.

The achievements panel contains:

1. Title `THÀNH TÍCH`.
2. A prominent `ĐIỂM CAO NHẤT` card.
3. A vertically scrollable `10 LƯỢT GẦN NHẤT` list.
4. A `QUAY LẠI` button.

Each recent row shows its 1-based position in recency order, local completion date/time, and score. The list is ordered newest first; the position is not a competitive rank. Survival duration is stored for future reporting and handoff completeness but is not shown in the first UI version.

When no results exist, high score displays `0` and the list displays `Chưa có lượt chơi nào`. While loading, the panel displays a loading state. If a query fails, it displays `Không thể tải thành tích` and keeps `QUAY LẠI` usable.

The Android system Back action and `QUAY LẠI` both return to READY. Starting a game is impossible while the achievements panel is open. The Game Over panel continues to show the current final score; it does not wait for database completion.

## Data Model

Database file: `tilt_escape.db`

Schema version: `1`

Table: `game_results`

| Column | SQLite type | Rules | Meaning |
| --- | --- | --- | --- |
| `id` | `INTEGER` | primary key, autoincrement | Local result identifier |
| `score` | `INTEGER` | not null, check `>= 0` | Obstacles safely passed |
| `survival_millis` | `INTEGER` | not null, check `>= 0` | Capped engine survival time converted to milliseconds |
| `played_at_epoch_millis` | `INTEGER` | not null | Completion time in UTC epoch milliseconds |

Indexes:

- `idx_game_results_played_at` on `(played_at_epoch_millis DESC, id DESC)` for recent results.
- `idx_game_results_score` on `(score DESC)` for the high score.

The database retains all completed runs. The screen query uses `LIMIT 10`; older results remain available to the high-score query. Schema creation uses `IF NOT EXISTS`. Version 1 has no upgrade migration; future schema changes must increment the version and add an explicit migration rather than dropping player history.

## Components and Responsibilities

### `GameResult`

A plain immutable Java value containing `id`, `score`, `survivalMillis`, and `playedAtEpochMillis`. Unsaved results may use a sentinel ID.

### `GameResultRepository`

A Java interface with synchronous storage operations intended to run on the database executor:

- `insert(GameResult result)`
- `getHighScore()`
- `getRecent(int limit)`
- `close()` where applicable

The interface keeps Android SQLite out of game/session logic and enables deterministic fakes in unit tests.

### `TiltEscapeDatabaseHelper`

Owns database name/version, creates the table and indexes, and contains future upgrade routing. It does not contain game-state decisions or UI formatting.

### `SQLiteGameResultRepository`

Maps `GameResult` values to `ContentValues`, executes parameterized queries, validates non-negative input, closes cursors with try-with-resources, and returns immutable result lists. Query ties are deterministic through `id DESC`.

### `GameResultRecorder`

Observes `GameSnapshot` values and detects a single transition into `GAME_OVER` for the current run. It arms when a new `PLAYING` run begins and records only when that armed run reaches `GAME_OVER`. Repeated GAME_OVER refreshes, Pause/Resume, opening achievements, returning to READY, and Activity pause callbacks do not produce extra inserts.

On a valid transition it copies score and survival duration immediately, adds the injected clock time, and submits the immutable result to a sink. The recorder has no Android database dependency. A new run rearms it only after observing `PLAYING`, so `PLAY_AGAIN` cannot reuse the prior completion.

### `GameResultsCoordinator`

Owns a single-thread executor for repository calls and a main-thread result dispatcher. It provides:

- a result sink used by `GameResultRecorder` to enqueue inserts;
- `loadAchievements(callback)` to query high score and recent results in order on the same executor;
- lifecycle-safe shutdown from `MainActivity.onDestroy()`.

Using one executor preserves insert/query ordering: opening achievements immediately after Game Over sees the just-enqueued result. Callbacks check that the Activity/controller is still active before touching views. Repository exceptions are converted into failure callbacks and logged; they never propagate into `GameView.doFrame()`.

### `AchievementsUiController`

Owns the achievements panel views, formats local date/time, renders loading/empty/data/error states, and returns to READY. It does not execute SQL. The existing `GameplayUiController` remains responsible for gameplay state panels and forwards snapshots to the recorder before rendering them.

`MainActivity` constructs the helper, repository, coordinator, recorder, and UI controllers. This remains manual dependency injection consistent with the current project.

## Event and Data Flow

1. `GameView` updates the engine and publishes a `GameSnapshot`.
2. `GameplayUiController.refresh()` forwards the snapshot to `GameResultRecorder`.
3. Normal PLAYING snapshots only arm/maintain recorder state.
4. On the first PLAYING-to-GAME_OVER observation, the recorder creates one immutable result.
5. `GameResultsCoordinator` queues the insert on its single database executor.
6. The Game Over UI renders immediately from the snapshot; it never waits on SQLite.
7. Opening achievements asks the coordinator to queue high-score and recent queries.
8. The coordinator posts an immutable view model or an error to the main thread.
9. `AchievementsUiController` renders the result if it is still active.

## Error Handling

- Invalid negative score/duration is rejected before insertion and logged.
- SQLite open, insert, and query exceptions are caught at the coordinator boundary.
- A failed insert does not retry automatically, avoiding accidental duplicate results; the run remains playable and restartable.
- A failed achievements query shows an error state and permits Back/`QUAY LẠI`.
- An empty database is a normal state, not an error.
- Date formatting falls back to the stored numeric timestamp only in diagnostics; malformed rows are not expected because repository writes are validated.

## Testing Strategy

### Pure unit tests

- `GameResultRecorder` saves once for PLAYING-to-GAME_OVER and ignores repeated GAME_OVER snapshots.
- Pause/Resume, READY, achievements navigation, and incomplete runs do not save.
- A second PLAYING run rearms the recorder and can save a second result.
- Score, survival milliseconds, and injected timestamp are copied correctly.
- Repository/coordinator errors do not escape into game callbacks.

### Robolectric SQLite tests

- Version 1 creates the expected table, columns, constraints, and indexes.
- Insert and read preserve score, duration, and timestamp.
- High score is the maximum across all stored runs, not only the recent ten.
- Recent query returns at most ten values newest first with deterministic ties.
- Zero-score runs are retained; negative inputs are rejected.
- Cursors and database resources can be closed and reopened without losing data.

### UI/controller tests

- READY shows the achievements button.
- Opening achievements does not change `GameEngine` state or start the loop.
- Loading, empty, populated, and error views render correctly.
- Ten rows display in newest-first order with localized text.
- `QUAY LẠI` and system Back return to READY.
- Repeated snapshot refreshes around Game Over do not create duplicate rows.

### Verification

- Run clean unit tests, `assembleDebug`, and `lintDebug`.
- Install the APK on Pixel 7.
- Complete two runs, including a zero-point run if practical; verify exactly two history rows.
- Force-stop and reopen the app; verify high score and recent history persist.
- Verify Play Again, Pause/Resume, and repeated opening of achievements do not add rows.

## Documentation and Handoff

Update `README.md` to describe local achievements and remove SQLite/high-score from the missing-feature list. Add `docs/database-schema.md` with the database filename, version, `CREATE TABLE`/index definitions, column meanings, ordering queries, retention policy, and upgrade rule. The debug APK remains a build artifact and is not committed.

## Builder Decisions Left Open

The builder may choose exact class/package names, small spacing and color adjustments that preserve the approved mockup, the immutable collection implementation, logging tags, and the specific Java date formatter compatible with the project's minimum SDK. These choices must not change storage semantics, navigation, or displayed information.
