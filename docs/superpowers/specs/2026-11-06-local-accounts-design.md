# Local Accounts and Account-Scoped Results Design

## Intent

Tilt Escape currently opens directly into a local game and stores completed runs without an account. Add local registration, login, persisted session, logout, and account-scoped achievements so the project satisfies the account foundation in R01-R10 and R33-R35 while remaining fully offline.

This feature is the prerequisite for the later local Leaderboard. It does not add networking or an online identity system.

## Goals

- Provide Login and Register panels inside the existing single-Activity application.
- Validate usernames and passwords and show clear Vietnamese errors.
- Store accounts in the existing app-private SQLite database.
- Never store plaintext passwords.
- Preserve the logged-in account across app restarts until Logout.
- Require a valid account before entering the Main Menu or starting gameplay.
- Associate every new completed run with the account that played it.
- Scope high score and recent-run history to the current account.
- Preserve existing version-1 results as legacy Guest data without assigning them to an arbitrary account.
- Keep authentication and database work off the UI/game frame path.
- Preserve current gameplay, sensor, pause, lifecycle, and duplicate-result behavior.

## Non-goals

- Online accounts, password recovery, email verification, social login, or cross-device synchronization.
- A Leaderboard screen or ranking query; that is a later feature built on this account foundation.
- How to Play and Settings screens.
- Importing legacy Guest results into a newly registered account.
- Encrypting the entire SQLite database.
- Allowing account deletion or password changes in this version.

## Anti-goals

- Do not silently assign old Guest history to the first registered user.
- Do not store, log, or persist plaintext passwords.
- Do not let an invalid/stale session bypass Login.
- Do not allow UI code to execute SQL or password hashing directly.
- Do not block gameplay frames with authentication or database work.
- Do not drop existing result history during the version-1 to version-2 migration.

## Constraints and Approved Decisions

- Android application code remains Java.
- UI remains one `MainActivity` with panels/controllers; no Navigation dependency is added.
- Continue using `SQLiteOpenHelper`; database version advances from 1 to 2.
- The database remains local to one Android installation.
- Username: after trimming, 3-20 ASCII characters containing only `A-Z`, `a-z`, `0-9`, and underscore. Vietnamese letters with diacritics are rejected with a clear validation message.
- Username uniqueness is case-insensitive through a normalized lowercase value using `Locale.ROOT`.
- Password: minimum 6 characters; confirmation must match.
- Session survives app restart and ends only on Logout or when the stored account no longer exists.
- Legacy version-1 rows remain with `user_id = NULL`, representing Guest data. They are excluded from account achievements and future account ranking.
- Registration returns to Login with the new username prefilled and a success message; it does not silently log in.

## Approaches Considered

### Selected: upgrade the existing SQLite database

Add accounts to `tilt_escape.db`, migrate `game_results`, use repository/coordinator boundaries, and store only the active numeric user ID in private session preferences. This preserves the explicit SQLite handoff, avoids a new dependency, and keeps account/result relationships queryable in one database.

### Rejected: separate account database

A second database would isolate account code but make account-result relationships, migrations, cleanup, and transaction reasoning harder with no benefit for this local application.

### Rejected: replace SQLiteOpenHelper with Room

Room would provide generated mappings but adds a dependency and requires rewriting the already tested result repository. The current explicit schema is also easier to document for the project handoff.

## Data Model

Database: `tilt_escape.db`  
Schema version: `2`

### Table: `users`

| Column | SQLite type | Rules | Meaning |
| --- | --- | --- | --- |
| `id` | `INTEGER` | primary key, autoincrement | Local account identifier |
| `username` | `TEXT` | not null | Trimmed display name |
| `username_normalized` | `TEXT` | not null, unique | `Locale.ROOT` lowercase lookup key |
| `password_hash` | `BLOB` | not null | PBKDF2-derived bytes |
| `password_salt` | `BLOB` | not null | Per-account random salt |
| `password_algorithm` | `TEXT` | not null | Stored algorithm name for future verification |
| `password_iterations` | `INTEGER` | not null, positive | Work factor used for this hash |
| `created_at_epoch_millis` | `INTEGER` | not null, non-negative | Account creation time |

Indexes:

- Unique index on `username_normalized`.

### Updated table: `game_results`

Add nullable column:

| Column | SQLite type | Rules | Meaning |
| --- | --- | --- | --- |
| `user_id` | `INTEGER` | nullable foreign key to `users(id)` | Owner; null means legacy Guest |

Add index `idx_game_results_user_played_at` on
`(user_id, played_at_epoch_millis DESC, id DESC)`.

The high-score query and recent-results query require a non-null current `user_id`. Legacy null-owner rows remain stored but are not included.

## Migration: Version 1 to Version 2

Run the migration transactionally:

1. Create `users` and its unique index.
2. Add nullable `user_id` to `game_results`.
3. Create the account/result index.
4. Leave every existing row's `user_id` as null.
5. Enable SQLite foreign-key enforcement in helper configuration.
6. Set database version to 2 only after successful migration.

Migration must never drop or recreate `game_results`. Tests will construct a real version-1 database, insert representative rows, upgrade it, and verify that all values survive with null ownership.

## Password Handling

`PasswordHasher` owns password derivation and verification:

- Generate a 16-byte salt with `SecureRandom`.
- Use PBKDF2 with a stored algorithm and iteration count.
- On API 26+, use `PBKDF2WithHmacSHA256`.
- On API 24-25, use the platform-supported `PBKDF2WithHmacSHA1`.
- Derive 32 bytes and store hash, salt, algorithm, and iteration count.
- Compare derived and stored hashes with a constant-time byte comparison.
- Clear temporary password character arrays when practical.
- Never write passwords or derived values to Logcat.

The iteration count is a named constant and must be covered by a device/emulator responsiveness check. Storing algorithm and work factor per row permits a future rehash policy without invalidating existing accounts.

This is local application authentication, not a substitute for server authentication. The active session stores no password or reusable credential.

## Session Handling

`SessionManager` stores only `active_user_id` in an app-private preferences file using `MODE_PRIVATE`.

On application start:

1. Read the stored ID.
2. Query the account repository on the database executor.
3. If the user exists, expose an authenticated session and show Main Menu.
4. If missing or invalid, clear the preference and show Login.

On successful Login, persist the user ID before showing Main Menu. On Logout, clear only the session preference; do not delete the account or results.

## Components and Responsibilities

### `UserAccount`

Immutable model containing ID, display username, normalized username, password metadata, and creation time. Password bytes are not exposed to UI controllers.

### `UsernamePolicy`

Trims, validates the ASCII-only length/character rules, and produces the normalized lookup key. It returns structured validation outcomes so UI messages do not duplicate rules.

### `PasswordHasher`

Creates and verifies versioned password hashes. It has no Android UI dependency and accepts injectable randomness/configuration for deterministic tests.

### `AccountRepository` and `SQLiteAccountRepository`

Provide:

- `createAccount(...)`
- `findByNormalizedUsername(...)`
- `findById(...)`
- `verifyCredentials(...)`

Duplicate usernames are rejected by both validation and the database unique constraint. Repository APIs return explicit outcomes for duplicate username, invalid credentials, storage failure, and success.

### `AccountCoordinator`

Serializes account queries and password hashing on a background executor and posts immutable outcomes to the main thread. It owns lifecycle-safe callback suppression like `GameResultsCoordinator`.

### `SessionManager`

Persists and clears the active user ID, validates restored sessions through the coordinator, and never stores password material.

### `AuthUiController`

Owns Login/Register panel views and form behavior:

- Login validation, loading state, failure message, and success navigation.
- Register validation, password confirmation, duplicate-name message, loading state, and success return to Login.
- Prevent repeated submission while a request is running.
- Clear password fields after submission or navigation.

### Main Menu integration

The existing READY panel becomes the authenticated Main Menu:

- Display current username.
- Keep Start Game and Thành tích.
- Add Logout.
- Do not show or enable gameplay controls before session restoration succeeds.

`MainActivity` remains the composition root and coordinates top-level panel state. Existing gameplay/pause/game-over controllers remain responsible for game state.

### Account-scoped game results

`GameResult` gains nullable owner identity or the repository insert API accepts an owner ID explicitly. The chosen implementation must snapshot the authenticated user ID for the run and save it with the Game Over result.

`GameResultRepository` queries become account-scoped:

- `getHighScore(long userId)`
- `getRecent(long userId, int limit)`

`GameResultsCoordinator` and `AchievementsUiController` load data only for the active user. A missing session rejects starting a run or opening account achievements and returns to Login.

## UI Flow

### Cold start without a valid session

`Login → Register → Login → Main Menu`

- Login includes username, password, Login, and Register link/button.
- Register includes username, password, confirmation, Create Account, and Back to Login.

### Cold start with a valid session

`Session validation → Main Menu`

The Main Menu displays the active username.

### Authenticated flow

`Main Menu → Gameplay/Pause/Game Over` remains unchanged.

`Main Menu → Thành tích` shows only the active account's high score and recent runs.

`Main Menu → Logout → Login` clears the session but preserves all database rows.

Android Back:

- Register returns to Login.
- Thành tích returns to Main Menu.
- Existing Pause/Game Over behavior remains unchanged.
- Back from Login or Main Menu follows normal Activity exit behavior.

## Error Handling

- Empty/invalid fields produce inline or panel-level Vietnamese messages without database access.
- Duplicate username produces a specific message and keeps the Register form usable.
- Invalid credentials use one generic message so the UI does not distinguish missing username from wrong password.
- Database or hashing failures show a recoverable generic error, re-enable the submit button, and are logged without sensitive values.
- A stale session is cleared and routed to Login.
- Account callbacks are ignored after Activity/controller shutdown or after a newer request supersedes them.
- Logout is safe to repeat and never deletes data.

## Testing

### Pure unit tests

- Username trimming, ASCII-only accepted characters, rejection of Vietnamese diacritics, length boundaries, and case normalization.
- Password minimum length and confirmation mismatch.
- Password hashing uses unique salts, verifies the right password, rejects the wrong password, and compares safely.
- Session store save/read/clear behavior.
- UI-independent auth state transitions and stale-request handling.

### Robolectric SQLite tests

- Fresh version-2 schema contains users, constraints, and indexes.
- Version-1 to version-2 migration preserves every existing result with `user_id = NULL`.
- Username uniqueness is case-insensitive through normalization.
- Account insert/read preserves fields.
- Credential verification succeeds/fails correctly.
- Account-scoped high score and recent history never mix users or Guest rows.
- Logout/session clearing does not delete accounts or results.

### UI/controller tests

- No session shows Login and cannot start gameplay.
- Login/Register navigation and Android Back work.
- Validation and duplicate/invalid-credential/storage errors render correctly.
- Successful registration returns to Login with a success message.
- Successful login and restored session show Main Menu with username.
- Logout returns to Login and clears password fields.
- A logged-in run saves against the correct account.
- Switching accounts shows isolated achievements.
- Missing accelerometer still permits Login/Register/Logout and account achievements while gameplay remains unavailable.

### Verification

- Run clean unit tests, `assembleDebug`, and `lintDebug`.
- Install the upgraded APK over the current version and verify legacy Guest rows survive.
- Register two accounts differing clearly in name.
- Verify duplicate case variants are rejected.
- Play one completed run per account and confirm achievements remain isolated.
- Force-stop/reopen and confirm session restoration.
- Logout and confirm Login appears without deleting either account's data.

## Documentation and Handoff

- Update `docs/database-schema.md` for schema version 2, migration, account table, and scoped queries.
- Update README with Register/Login/Logout instructions and local-only limitations.
- Add a concise completed-function and known-limitations section.
- Keep the debug APK as a build artifact, not a committed binary.

## Builder Decisions Left Open

- Exact Java result types used for repository/coordinator outcomes.
- Exact panel styling and spacing, while preserving current visual language and mobile usability.
- Exact internal controller split, provided SQL, hashing, session persistence, and view ownership remain separated.
- Iteration-count tuning based on emulator/device measurements, stored as a named and persisted value.
