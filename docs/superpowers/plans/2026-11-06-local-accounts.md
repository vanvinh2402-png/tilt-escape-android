# Local Accounts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add offline Register, Login, persisted session, Logout, and account-scoped achievements while preserving legacy Guest results.

**Architecture:** Upgrade the existing `SQLiteOpenHelper` database to version 2, add focused account/crypto/session components, and expose asynchronous account operations through a coordinator. Keep the existing single Activity and panel/controller style; gate Main Menu/gameplay behind a validated session and pass the active account ID into result persistence and achievements queries.

**Tech Stack:** Android Java 17, XML Views, `SQLiteOpenHelper`, PBKDF2/JCA, private `SharedPreferences`, JUnit 4, Robolectric 4.17, Gradle 8.11.1.

**Spec:** `docs/superpowers/specs/2026-11-06-local-accounts-design.md`

## Global Constraints

- Android code remains Java; min SDK 24, compile/target SDK 35.
- Add no Room, Navigation, DataStore, or online-service dependency.
- Keep one `MainActivity` and existing gameplay state machine.
- Username is trimmed, 3-20 ASCII characters, and matches `[A-Za-z0-9_]+`; uniqueness is case-insensitive with `Locale.ROOT`.
- Password is at least 6 characters and must match confirmation during registration.
- Never persist or log plaintext passwords.
- Existing version-1 results must survive migration with nullable `user_id = NULL`.
- New runs and achievements require a valid authenticated user.
- SQLite, account lookup, and password hashing stay off the UI/game frame thread.
- Session persists until Logout or failed account restoration.

## Review Focus

- A real version-1 database containing zero- and nonzero-score rows upgrades without loss and leaves every old `user_id` null — pinned in Task 2 migration tests.
- Usernames with whitespace, case variants, Unicode/diacritics, and boundary lengths cannot bypass normalization or uniqueness — pinned in Tasks 1 and 2.
- A stale or malformed persisted user ID is cleared before Main Menu/gameplay appears — pinned in Tasks 3, 4, and 5.
- Rapid repeated Register/Login clicks and late callbacks cannot create duplicate requests or overwrite a newer screen — pinned in Tasks 4 and 5.
- On a device without Accelerometer, authentication and achievements still work while Start is visibly disabled — pinned in Task 5.

---

### Task 1: Username and Password Domain

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/UsernameValidation.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/UsernamePolicy.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/PasswordValidation.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/PasswordPolicy.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/PasswordHash.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/PasswordHasher.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/AuthenticatedUser.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/auth/UsernamePolicyTest.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/auth/PasswordPolicyTest.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/auth/PasswordHasherTest.java`

**Interfaces:**
- Consumes: Java `SecureRandom`, `SecretKeyFactory`, `PBEKeySpec`; Android API level supplied as an integer for algorithm selection.
- Produces:
  - `UsernameValidation UsernamePolicy.validate(String rawUsername)`
  - `UsernameValidation.Error { NONE, REQUIRED, INVALID_LENGTH, INVALID_CHARACTERS }`
  - `PasswordValidation PasswordPolicy.validate(char[] password, char[] confirmation)`
  - `PasswordValidation.Error { NONE, REQUIRED, TOO_SHORT, CONFIRMATION_MISMATCH }`
  - `PasswordHash PasswordHasher.hash(char[] password)`
  - `boolean PasswordHasher.verify(char[] password, PasswordHash stored)`
  - `PasswordHasher(SecureRandom random, int apiLevel, int iterations)`
  - `AuthenticatedUser(long id, String username)`

- [ ] **Step 1: Write failing username-policy tests**

Add tests asserting trimming and normalization of `"  Player_01  "`, case-folding with `Locale.ROOT`, accepted lengths 3 and 20, and explicit rejection of empty, length 2/21, spaces, punctuation, and `"Mạnh_01"`.

- [ ] **Step 2: Run the username tests and verify RED**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.auth.UsernamePolicyTest"`  
Expected: FAIL because the policy classes do not exist.

- [ ] **Step 3: Implement username validation**

`UsernameValidation` is immutable and exposes `getError()`, `getDisplayUsername()`, `getNormalizedUsername()`, and `isValid()`. `UsernamePolicy.validate` trims first, checks required/length/ASCII regex in that order, then normalizes with `Locale.ROOT`.

- [ ] **Step 4: Write failing password-policy tests**

Test required password, 5/6-character boundary, matching confirmation, and mismatch without converting passwords to `String`.

- [ ] **Step 5: Run password-policy tests and verify RED**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.auth.PasswordPolicyTest"`  
Expected: FAIL because password-policy classes do not exist.

- [ ] **Step 6: Implement password validation**

Implement immutable `PasswordValidation` and `PasswordPolicy.validate` in required, minimum-length, confirmation order.

- [ ] **Step 7: Run password-policy tests and verify GREEN**

Run the PasswordPolicy test class.  
Expected: PASS.

- [ ] **Step 8: Write failing password-hasher tests**

Assert two hashes of the same password use different 16-byte salts; correct verification succeeds; wrong password fails; stored byte arrays are defensively copied; API 26 selects `PBKDF2WithHmacSHA256`; API 24 selects `PBKDF2WithHmacSHA1`; derived hashes are 32 bytes.

- [ ] **Step 9: Run password tests and verify RED**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.auth.PasswordHasherTest"`  
Expected: FAIL because hash classes do not exist.

- [ ] **Step 10: Implement versioned PBKDF2 hashing**

Use `DEFAULT_ITERATIONS = 120_000`, 16-byte `SecureRandom` salt, 256-bit `PBEKeySpec`, and constant-time comparison. `PasswordHash` stores algorithm, iterations, salt, and derived bytes with defensive copies. Do not log secrets.

- [ ] **Step 11: Run Task 1 tests**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.auth.*Test"`  
Expected: PASS.

- [ ] **Step 12: Run the full unit suite**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 13: Commit**

`git add app/src/main/java/com/vanvinh2402/tiltescape/auth app/src/test/java/com/vanvinh2402/tiltescape/auth && git commit -m "feat: add local account validation and hashing"`

---

### Task 2: SQLite Version 2 and Account Repository

**Files:**
- Move: `app/src/main/java/com/vanvinh2402/tiltescape/results/TiltEscapeDatabaseHelper.java` → `app/src/main/java/com/vanvinh2402/tiltescape/data/TiltEscapeDatabaseHelper.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/results/SQLiteGameResultRepository.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/AccountRecord.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/AccountRepository.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/SQLiteAccountRepository.java`
- Modify: `app/src/test/java/com/vanvinh2402/tiltescape/results/SQLiteGameResultRepositoryTest.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/data/TiltEscapeDatabaseMigrationTest.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/auth/SQLiteAccountRepositoryTest.java`

**Interfaces:**
- Consumes: `PasswordHash` from Task 1 and existing `GameResult`.
- Produces:
  - public `TiltEscapeDatabaseHelper(Context)`, database version 2, shared table/column constants
  - `long AccountRepository.insert(String username, String normalized, PasswordHash hash, long createdAt)`
  - `AccountRecord AccountRepository.findByNormalizedUsername(String normalized)`
  - `AccountRecord AccountRepository.findById(long id)`
  - `void AccountRepository.close()`
- Existing unscoped game-result APIs remain temporarily unchanged in this task so each commit builds; Task 6 replaces them atomically.

- [ ] **Step 1: Write a real version-1 migration test**

Create a database at version 1 using the exact old table/index DDL, insert rows including score 0 and tied timestamps, set version 1, close it, then open version-2 helper. Assert all original values remain, `user_id` exists and is null, `users` exists, version is 2, foreign keys are enabled, and new indexes exist.

- [ ] **Step 2: Run migration test and verify RED**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.data.TiltEscapeDatabaseMigrationTest"`  
Expected: FAIL because version 2/migration does not exist.

- [ ] **Step 3: Move and upgrade the database helper**

Implement `onConfigure` foreign keys, fresh version-2 creation, and transactional `onUpgrade(1,2)` that creates `users`, adds nullable `user_id`, creates indexes, and never drops `game_results`. Reject unsupported upgrade paths explicitly.

- [ ] **Step 4: Write failing repository tests**

Test insert/read by ID and normalized username, all password metadata, case-variant duplicate rejection through the unique index, null lookup, non-negative creation time, and cursor closure. Also update existing result-repository tests to import the moved helper and confirm old unscoped behavior still passes before Task 6.

- [ ] **Step 5: Run repository tests and verify RED**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.auth.SQLiteAccountRepositoryTest" --tests "com.vanvinh2402.tiltescape.results.SQLiteGameResultRepositoryTest"`  
Expected: FAIL because account repository classes do not exist.

- [ ] **Step 6: Implement account records and repository**

`AccountRecord` is immutable. Repository maps BLOBs with defensive copies, uses parameterized queries, returns null when absent, validates non-negative timestamps, and lets unique-constraint exceptions cross the repository boundary for coordinator classification.

- [ ] **Step 7: Run Task 2 tests**

Run the three Task 2 test classes.  
Expected: PASS.

- [ ] **Step 8: Run the full unit suite**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 9: Commit**

`git add app/src/main/java/com/vanvinh2402/tiltescape/data app/src/main/java/com/vanvinh2402/tiltescape/auth app/src/main/java/com/vanvinh2402/tiltescape/results app/src/test && git commit -m "feat: migrate SQLite for local accounts"`

---

### Task 3: Persisted and In-Memory Session

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/SessionStore.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/SharedPreferencesSessionStore.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/ActiveSession.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/auth/SharedPreferencesSessionStoreTest.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/auth/ActiveSessionTest.java`

**Interfaces:**
- Consumes: `AuthenticatedUser` from Task 1.
- Produces:
  - `Long SessionStore.getActiveUserId()`
  - `void SessionStore.saveActiveUserId(long userId)`
  - `void SessionStore.clear()`
  - `boolean ActiveSession.isAuthenticated()`
  - `AuthenticatedUser ActiveSession.getUser()`
  - `long ActiveSession.requireUserId()`
  - `void ActiveSession.authenticate(AuthenticatedUser user)`
  - `void ActiveSession.clear()`

- [ ] **Step 1: Write failing session tests**

Assert absent preference returns null, save survives a new store instance, clear removes it, zero/negative IDs are rejected, malformed/stale stored numeric data is treated as absent and cleared, and ActiveSession enforces authentication before `requireUserId`.

- [ ] **Step 2: Run tests and verify RED**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.auth.*Session*Test"`  
Expected: FAIL because session classes do not exist.

- [ ] **Step 3: Implement private session persistence**

Use one named `MODE_PRIVATE` preferences file and store only `active_user_id`. `ActiveSession` holds no password/hash data.

- [ ] **Step 4: Run Task 3 tests**

Run the targeted session classes.  
Expected: PASS.

- [ ] **Step 5: Run the full unit suite**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 6: Commit**

`git add app/src/main/java/com/vanvinh2402/tiltescape/auth app/src/test/java/com/vanvinh2402/tiltescape/auth && git commit -m "feat: persist active local session"`

---

### Task 4: Asynchronous Account Coordinator

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/AuthOutcome.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/AuthService.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/auth/AccountCoordinator.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/auth/AccountCoordinatorTest.java`

**Interfaces:**
- Consumes: `PasswordHasher`, `AccountRepository`, `AuthenticatedUser`.
- Produces:
  - `AuthOutcome.Status { REGISTERED, AUTHENTICATED, NO_SESSION, DUPLICATE_USERNAME, INVALID_CREDENTIALS, ERROR }`
  - `void AuthService.register(UsernameValidation username, char[] password, Callback callback)`
  - `void AuthService.login(UsernameValidation username, char[] password, Callback callback)`
  - `void AuthService.restore(long userId, Callback callback)`
  - `AuthService.Callback.onComplete(AuthOutcome outcome)`
  - `AccountCoordinator(AccountRepository repository, PasswordHasher hasher, ExecutorService executor, Consumer<Runnable> mainDispatcher, LongSupplier clock)`
  - `void AccountCoordinator.close()`

- [ ] **Step 1: Write deterministic failing coordinator tests**

Use a FIFO queued executor, queued main dispatcher, fake repository, deterministic hasher, injected clock, and recording callback. Test register off caller thread and uses the injected non-negative timestamp; duplicate constraint maps to `DUPLICATE_USERNAME`; login is generic `INVALID_CREDENTIALS` for missing user and wrong password; restore returns `NO_SESSION` for missing ID; storage/hash failure returns `ERROR`; rapid calls remain FIFO; close suppresses late callbacks/rejects new work; password arrays are copied synchronously before the service method returns and the worker copy is cleared after use.

- [ ] **Step 2: Run tests and verify RED**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.auth.AccountCoordinatorTest"`  
Expected: FAIL because coordinator/outcome classes do not exist.

- [ ] **Step 3: Implement coordinator and narrow service interface**

Mirror `GameResultsCoordinator` lifecycle/dispatch conventions. Inject `LongSupplier clock`; clamp a negative clock to zero. Perform repository access and PBKDF2 only on the single worker executor. Never include username/password/hash data in logs beyond a generic operation label.

- [ ] **Step 4: Run Task 4 tests**

Run the coordinator test.  
Expected: PASS.

- [ ] **Step 5: Run the full unit suite**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 6: Commit**

`git add app/src/main/java/com/vanvinh2402/tiltescape/auth app/src/test/java/com/vanvinh2402/tiltescape/auth && git commit -m "feat: coordinate local authentication"`

---

### Task 5: Login, Register, Main Menu, and Logout UI

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/ui/AuthUiController.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/ui/GameplayUiController.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/MainActivity.java`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Create: `app/src/test/java/com/vanvinh2402/tiltescape/ui/AuthUiControllerTest.java`
- Modify: `app/src/test/java/com/vanvinh2402/tiltescape/ui/GameplayUiControllerTest.java`
- Modify: `app/src/test/java/com/vanvinh2402/tiltescape/MainActivityTest.java`

**Interfaces:**
- Consumes: `AuthService`, `SessionStore`, `ActiveSession`, `UsernamePolicy`, `PasswordPolicy`.
- Produces:
  - `AuthUiController(Activity activity, AuthService authService, SessionStore sessionStore, ActiveSession activeSession, UsernamePolicy usernamePolicy, PasswordPolicy passwordPolicy)`
  - `void AuthUiController.bind()`
  - `void AuthUiController.restoreSession()`
  - `boolean AuthUiController.handleBack()`
  - `boolean AuthUiController.isMainMenuAllowed()`
  - `void AuthUiController.close()`
  - `GameplayUiController(..., BooleanSupplier achievementsVisible, BooleanSupplier mainMenuAllowed)`

- [ ] **Step 1: Add failing view-existence and initial-state tests**

Require IDs for Login/Register fields, errors, loading state, current username, Logout, and sensor message. Assert no session shows Login, hides Main Menu and gameplay controls, and does not start the engine.

- [ ] **Step 2: Run UI tests and verify RED**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.ui.AuthUiControllerTest" --tests "com.vanvinh2402.tiltescape.MainActivityTest"`  
Expected: FAIL because views/controller do not exist.

- [ ] **Step 3: Add Login/Register/Main Menu panels and strings**

Use `EditText` password input types, accessible hints/labels, Vietnamese validation/error copy, loading/disabled submit states, username display, Logout, and a Main Menu sensor-status element. Keep panels inside the existing overlay and preserve scrolling/usability on small screens.

- [ ] **Step 4: Add failing behavior tests**

Test Login↔Register navigation; Back from Register; username/password/confirmation validation; duplicate and invalid-credential messages; successful registration returns Login with username and success message; login saves session and shows username/Main Menu; restored session bypasses Login only after validation; stale ID clears and returns Login; Logout clears both session layers and password fields; double-click while pending submits once; stale callback after navigation is ignored.

- [ ] **Step 5: Implement AuthUiController**

Controller owns auth panel visibility and request generations. Copy password text into temporary `char[]`, call the service (which synchronously takes its own copy), clear the caller array in `finally`, and clear the visible field immediately after accepted submission. Persist user ID only on authenticated success.

- [ ] **Step 6: Gate gameplay and wire MainActivity**

Create account repository/coordinator/session/auth controller before sensor gating. `GameplayUiController.refresh` may show READY/Main Menu only when `mainMenuAllowed` is true. With no Accelerometer, keep Login/Register/Main Menu/Logout/achievements operational, disable Start, and display the sensor message. Back priority is achievements, then Register, then normal Activity exit.

- [ ] **Step 7: Run Task 5 tests**

Run the three UI/Activity classes plus existing gameplay tests.  
Expected: PASS.

- [ ] **Step 8: Run the full unit suite**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 9: Commit**

`git add app/src/main app/src/test && git commit -m "feat: add local login and registration UI"`

---

### Task 6: Account-Scoped Results and Achievements

**Files:**
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/results/GameResultRepository.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/results/SQLiteGameResultRepository.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/results/AchievementsLoader.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/results/GameResultsCoordinator.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/game/GameResultRecorder.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/ui/AchievementsUiController.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/MainActivity.java`
- Modify: corresponding repository/coordinator/achievements/gameplay tests

**Interfaces:**
- Consumes: `ActiveSession.requireUserId()` and existing `GameResultRecorder`.
- Produces:
  - `long GameResultRepository.insert(long userId, GameResult result)`
  - `int GameResultRepository.getHighScore(long userId)`
  - `List<GameResult> GameResultRepository.getRecent(long userId, int limit)`
  - `void GameResultsCoordinator.save(long userId, GameResult result)`
  - `void AchievementsLoader.loadAchievements(long userId, Callback callback)`
  - `AchievementsUiController(..., LongSupplier activeUserId)`
  - `GameResultRecorder(BiConsumer<Long, GameResult> sink, LongSupplier activeUserId, LongSupplier clock)`

- [ ] **Step 1: Write failing account-isolation repository tests**

Insert results for two users plus a direct legacy null-owner row. Assert each user's high score/recent list contains only their rows, retains newest-first/tie ordering, uses all owned rows for high score, and excludes Guest. Reject non-positive user IDs.

- [ ] **Step 2: Run repository tests and verify RED**

Run the SQLite result repository test.  
Expected: FAIL because APIs are still unscoped.

- [ ] **Step 3: Implement scoped SQL and interfaces**

Every insert writes `user_id`; every read includes `WHERE user_id = ?`. Replace old unscoped public methods rather than retaining a path that can leak mixed-account data.

- [ ] **Step 4: Write failing coordinator/UI integration tests**

Assert save then load preserves executor order for one user, different users never mix, achievements passes the current user ID, missing session cannot open achievements, recorder captures the user ID on the first PLAYING snapshot and still saves that owner if the supplier later changes before Game Over, and logout/second login changes subsequent-run ownership without duplicating prior results.

- [ ] **Step 5: Implement result ownership wiring**

`GameResultRecorder` captures `ActiveSession.requireUserId()` when a run first enters PLAYING, clears ownership on READY, and submits the captured ID at the single Game Over transition. Achievements obtains the active ID only when opening from authenticated Main Menu. Missing/invalid session routes through auth handling instead of querying global history.

- [ ] **Step 6: Run Task 6 tests**

Run result, coordinator, achievements, gameplay, and Activity tests.  
Expected: PASS.

- [ ] **Step 7: Run the full unit suite**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 8: Commit**

`git add app/src/main app/src/test && git commit -m "feat: scope achievements to local accounts"`

---

### Task 7: Handoff Documentation and Final Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/database-schema.md`
- Create: `docs/test-results-local-accounts.md`

**Interfaces:**
- Consumes: final schema/UI behavior from Tasks 1-6.
- Produces: build/run/use instructions, schema-v2/migration documentation, completed-function list, test scenarios/results, and known limitations required by SRS section 11.

- [ ] **Step 1: Update handoff documents**

Document users table, nullable legacy ownership, account indexes, version-1→2 migration, PBKDF2 metadata, private session behavior, Login/Register/Logout steps, local-only limitation, and absence of password recovery/Leaderboard.

- [ ] **Step 2: Run a clean verification build**

Run: `.\gradlew.bat --stop`  
Run: `.\gradlew.bat --no-daemon clean testDebugUnitTest assembleDebug lintDebug`  
Expected: `BUILD SUCCESSFUL`, zero test failures, APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Perform upgrade/device smoke checks**

Install the pre-feature APK and create legacy Guest history; install the new APK with `adb install -r`; register two accounts; reject a case-variant duplicate; complete one run per account; verify isolated achievements; force-stop/relaunch to verify session restoration; Logout to verify data preservation; repeat on no-sensor Robolectric path and Pixel 7 emulator.

- [ ] **Step 4: Record exact results**

Write command outputs, test count, device/API, migration result, account names used, isolated scores, session restoration, Logout result, APK path/size, and any known issue into `docs/test-results-local-accounts.md`.

- [ ] **Step 5: Commit**

`git add README.md docs/database-schema.md docs/test-results-local-accounts.md && git commit -m "docs: document local accounts and verification"`

- [ ] **Step 6: Request final review**

Review the complete diff against the design with focus on migration safety, secret handling, session gating, async lifecycle, cross-account isolation, and no-sensor navigation. Resolve every Critical/Important finding with one RED→GREEN fix pass, rerun the clean verification, and record any declined-to-judge ruling.
