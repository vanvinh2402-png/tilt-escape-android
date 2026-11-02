# SQLite Achievements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist every completed Tilt Escape run in SQLite and provide an approved achievements screen showing the all-time high score and ten most recent runs.

**Architecture:** A repository backed by `SQLiteOpenHelper` owns the explicit version-1 schema. A pure `GameResultRecorder` detects one PLAYING-to-GAME_OVER transition, while a single-threaded `GameResultsCoordinator` serializes writes/reads off the UI thread and posts lifecycle-safe results to the UI. A dedicated achievements controller renders the approved overlay without coupling `GameEngine` to Android storage.

**Tech Stack:** Java 17, Android SDK 35/minSdk 24, Android `SQLiteOpenHelper`, Android `Executor`/`Handler`, JUnit 4, Robolectric 4.17, Gradle 8.11.1.

**Spec:** `docs/superpowers/specs/2026-11-02-sqlite-achievements-design.md`

## Global Constraints

- Keep Android application code in Java and add no database or ORM dependency.
- Database file is exactly `tilt_escape.db`; schema version is exactly `1`.
- Persist one row only for each observed `PLAYING` to `GAME_OVER` run, including zero-point runs.
- Never write on score changes, normal frames, Pause/Resume, READY, or achievements navigation.
- Store `score`, `survival_millis`, and `played_at_epoch_millis` as non-negative integers.
- Keep SQLite calls off the UI/game-loop thread and serialize them through one executor.
- Database failure must not crash, block, or alter gameplay.
- High score covers every retained result; recent history is newest-first and limited to ten.
- Timestamps are stored as epoch milliseconds and formatted in the device timezone for display.
- All completed results remain until app data is cleared or the app is uninstalled.

## Review Focus

- Repeated GAME_OVER snapshots or button refreshes must create exactly one row; pinned by Task 2's repeated-observation test.
- Opening achievements immediately after Game Over must include the queued insert; pinned by Task 3's single-executor ordering test.
- Destroying the Activity while a database request is pending must suppress its UI callback; pinned by Task 3's close-before-dispatch test.
- Multiple results with the same timestamp must retain deterministic newest order by `id DESC`; pinned by Task 1's tied-timestamp query test.
- SQLite insert/query exceptions must leave gameplay usable and render a recoverable achievements error; pinned by Tasks 3 and 4 failure tests.

---

### Task 1: SQLite result repository

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/results/GameResult.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/results/GameResultRepository.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/results/TiltEscapeDatabaseHelper.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/results/SQLiteGameResultRepository.java`
- Create: `app/src/test/java/com/vanvinh2402/tiltescape/results/SQLiteGameResultRepositoryTest.java`

**Interfaces:**
- Consumes: Android `Context`, `SQLiteOpenHelper`, `SQLiteDatabase`.
- Produces: immutable `GameResult(long id, int score, long survivalMillis, long playedAtEpochMillis)` with getters; `GameResultRepository.insert(GameResult): long`, `getHighScore(): int`, `getRecent(int): List<GameResult>`, and `close(): void`.

- [ ] **Step 1: Write failing repository tests**

Add tests named:

- `createsVersionOneTableAndIndexes()` asserting database version `1`, columns `id`, `score`, `survival_millis`, `played_at_epoch_millis`, and both named indexes.
- `insertAndReopenPreservesEveryField()` asserting `8`, `12_500L`, and `1_793_610_000_000L` round-trip after close/reopen.
- `highScoreUsesAllRowsWhileRecentReturnsOnlyTenNewest()` inserting 12 rows and asserting high score can belong to an older row while recent size is 10.
- `sameTimestampOrdersHigherIdFirst()` inserting two equal timestamps and asserting the second inserted row is first.
- `zeroScoreIsStoredAndNegativeFieldsAreRejected()` asserting zero round-trips and negative score/duration throws `IllegalArgumentException`.
- `nonPositiveRecentLimitIsRejected()` asserting `0` and `-1` throw `IllegalArgumentException`.

- [ ] **Step 2: Run the repository tests to verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest --tests '*.SQLiteGameResultRepositoryTest'
```

Expected: compilation failure because the results classes do not exist.

- [ ] **Step 3: Add the immutable result and repository contract**

Implement the exact constructor/getter types from Interfaces. `GameResultRepository` extends `AutoCloseable`; `close()` must not declare a checked exception.

- [ ] **Step 4: Implement schema version 1**

`TiltEscapeDatabaseHelper` creates `game_results` with non-negative `CHECK` constraints and indexes `idx_game_results_played_at` on `(played_at_epoch_millis DESC, id DESC)` and `idx_game_results_score` on `(score DESC)`. `onUpgrade` accepts only equal versions in this release and throws for an unsupported migration rather than dropping data.

- [ ] **Step 5: Implement repository inserts and queries**

Use `ContentValues`, parameterized queries, try-with-resources cursors, immutable returned lists, `COALESCE(MAX(score), 0)`, recent ordering `played_at_epoch_millis DESC, id DESC`, and caller-supplied `LIMIT`. Validate all non-negative fields and positive limits before touching SQLite.

- [ ] **Step 6: Run repository and full tests to verify GREEN**

Run:

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest --tests '*.SQLiteGameResultRepositoryTest'
.\gradlew.bat --no-daemon testDebugUnitTest
```

Expected: all tests pass.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/vanvinh2402/tiltescape/results app/src/test/java/com/vanvinh2402/tiltescape/results/SQLiteGameResultRepositoryTest.java
git commit -m "feat: add SQLite game result repository"
```

---

### Task 2: Record each completed run exactly once

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/game/GameResultRecorder.java`
- Create: `app/src/test/java/com/vanvinh2402/tiltescape/game/GameResultRecorderTest.java`

**Interfaces:**
- Consumes: `GameSnapshot`, `Consumer<GameResult>`, and `LongSupplier` clock.
- Produces: `GameResultRecorder(Consumer<GameResult>, LongSupplier)` and `observe(GameSnapshot): void`.

- [ ] **Step 1: Write failing transition tests**

Construct package-visible `GameSnapshot` fixtures and add tests named:

- `playingThenGameOverSavesOneCompleteResult()` asserting score `3`, survival `4_250L` milliseconds, and injected timestamp `1_793_610_000_000L`.
- `repeatedGameOverSnapshotsSaveOnlyOnce()` observing GAME_OVER three times and asserting sink size `1`.
- `pauseResumeReadyAndGameOverWithoutPlayingDoNotSave()` asserting sink remains empty.
- `secondPlayingRunRearmsAndSavesSecondResult()` asserting two distinct completed runs produce two rows.
- `nonFiniteOrNegativeSurvivalIsClampedToZero()` covering `NaN`, infinity, and negative duration so invalid engine-facing values cannot reach SQLite.

- [ ] **Step 2: Run recorder tests to verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest --tests '*.GameResultRecorderTest'
```

Expected: compilation failure because `GameResultRecorder` does not exist.

- [ ] **Step 3: Implement the recorder state machine**

Arm after observing `PLAYING`; keep the arm through `PAUSED`; create a result only on the first later `GAME_OVER`; disarm after submission. Convert finite positive survival seconds with `Math.round(seconds * 1000.0f)` and clamp invalid/negative values to `0L`. Copy values before invoking the sink.

- [ ] **Step 4: Run recorder and full tests to verify GREEN**

Run:

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest --tests '*.GameResultRecorderTest'
.\gradlew.bat --no-daemon testDebugUnitTest
```

Expected: all tests pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/vanvinh2402/tiltescape/game/GameResultRecorder.java app/src/test/java/com/vanvinh2402/tiltescape/game/GameResultRecorderTest.java
git commit -m "feat: record completed game runs once"
```

---

### Task 3: Serialize background saves and achievements queries

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/results/AchievementsData.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/results/GameResultsCoordinator.java`
- Create: `app/src/test/java/com/vanvinh2402/tiltescape/results/GameResultsCoordinatorTest.java`

**Interfaces:**
- Consumes: `GameResultRepository`, `ExecutorService`, and a `Consumer<Runnable>` main-thread dispatcher.
- Produces: immutable `AchievementsData(int highScore, List<GameResult> recent)`; `GameResultsCoordinator.save(GameResult): void`, `loadAchievements(Callback): void`, `close(): void`; callback methods `onLoaded(AchievementsData)` and `onError()`.

- [ ] **Step 1: Write failing coordinator tests**

Use deterministic queued executors/dispatchers and a fake repository. Add tests named:

- `saveRunsOnDatabaseExecutorNotCaller()` asserting insert is pending until the executor runs.
- `saveThenLoadPreservesQueueOrder()` enqueueing save then load and asserting returned data includes the saved result.
- `insertFailureDoesNotEscapeOrPreventLaterLoad()` making insert throw and asserting caller does not throw and a later query still succeeds.
- `queryFailurePostsOnError()` asserting exactly one error callback.
- `closeBeforeMainDispatchSuppressesCallback()` completing the database query, closing, then draining main callbacks and asserting neither callback fires.
- `closeRejectsNewWorkAndClosesRepositoryOnce()` asserting no executor work is added after close.

- [ ] **Step 2: Run coordinator tests to verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest --tests '*.GameResultsCoordinatorTest'
```

Expected: compilation failure because coordinator/data classes do not exist.

- [ ] **Step 3: Implement immutable achievements data**

Defensively copy recent results and expose an unmodifiable list. Reject negative high scores.

- [ ] **Step 4: Implement ordered asynchronous coordination**

Submit all repository operations to the same `ExecutorService`. Catch `RuntimeException` at task boundaries and log without rethrowing to gameplay callers. For successful/failed loads, post a closure through the dispatcher that checks an atomic closed flag before invoking the callback. `close()` atomically rejects new work, queues repository close behind existing operations, then calls `shutdown()` so accepted work and close finish in order; repeated close calls do nothing.

- [ ] **Step 5: Add the Android production factory**

Add `GameResultsCoordinator.create(GameResultRepository): GameResultsCoordinator`, using `Executors.newSingleThreadExecutor()` and `new Handler(Looper.getMainLooper())::post` while retaining the injectable constructor at package visibility for tests.

- [ ] **Step 6: Run coordinator and full tests to verify GREEN**

Run:

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest --tests '*.GameResultsCoordinatorTest'
.\gradlew.bat --no-daemon testDebugUnitTest
```

Expected: all tests pass.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/vanvinh2402/tiltescape/results/AchievementsData.java app/src/main/java/com/vanvinh2402/tiltescape/results/GameResultsCoordinator.java app/src/test/java/com/vanvinh2402/tiltescape/results/GameResultsCoordinatorTest.java
git commit -m "feat: coordinate background game results"
```

---

### Task 4: Add and wire the achievements screen

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/ui/AchievementsUiController.java`
- Create: `app/src/main/res/layout/item_game_result.xml`
- Create: `app/src/test/java/com/vanvinh2402/tiltescape/ui/AchievementsUiControllerTest.java`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/ui/GameplayUiController.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/MainActivity.java`
- Modify: `app/src/test/java/com/vanvinh2402/tiltescape/ui/GameplayUiControllerTest.java`
- Modify: `app/src/test/java/com/vanvinh2402/tiltescape/MainActivityTest.java`

**Interfaces:**
- Consumes: Tasks 1–3, `GameEngine`, `GameView`, `GameResultsCoordinator`, Android views/resources.
- Produces: `AchievementsUiController(Activity, GameEngine, GameView, GameResultsCoordinator)`, `bind(): void`, `open(): void`, `close(): void`, `isVisible(): boolean`, `handleBack(): boolean`; `GameplayUiController(Activity, GameEngine, GameView, GameResultRecorder, BooleanSupplier)` for production/test injection; production wiring from snapshots to `GameResultRecorder.observe(GameSnapshot)`.

- [ ] **Step 1: Add failing UI tests for navigation and states**

Add tests named:

- `readyAchievementsButtonOpensWithoutStartingGame()` asserting engine remains READY, loop is stopped, READY panel hides, and achievements panel shows.
- `loadingThenEmptyShowsZeroAndEmptyMessage()` asserting loading precedes callback and empty rendering shows high score `0`.
- `populatedDataShowsHighScoreAndTenNewestRows()` asserting title/value and exactly ten row views in supplied order.
- `queryFailureShowsRecoverableErrorAndBackRemainsEnabled()`.
- `backButtonAndSystemBackReturnToReady()` asserting `handleBack()` consumes only while visible.
- `staleLoadCallbackAfterCloseDoesNotRender()` opening, closing, then delivering a callback and asserting READY remains visible.

Extend `GameplayUiControllerTest` with `repeatedGameOverRefreshPersistsOnlyOnce()` using a collecting recorder sink, and extend `MainActivityTest` to assert required achievements view IDs exist.

- [ ] **Step 2: Run UI tests to verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest --tests '*.AchievementsUiControllerTest' --tests '*.GameplayUiControllerTest' --tests '*.MainActivityTest'
```

Expected: resource/class compilation failures for the new screen.

- [ ] **Step 3: Add approved layout and localized strings**

Add `achievements_button` to READY and `achievements_panel` to the existing overlay. The panel contains `achievements_high_score`, `achievements_loading`, `achievements_empty`, `achievements_error`, a scrollable `achievements_results_container`, and `achievements_back_button`. Add `item_game_result.xml` with position, localized date/time, and score. Use the existing dark palette, turquoise high-score emphasis, red score rows, and screen-sized scrolling behavior from the approved mockup.

- [ ] **Step 4: Implement achievements rendering and stale-request protection**

`open()` is valid only while engine state is READY; it increments a request generation, shows loading, hides READY, and calls the coordinator. Each callback captures its generation and renders only if it still matches and the panel remains visible. `close()` invalidates the generation, hides the panel, and refreshes the gameplay snapshot. Format dates with the device locale/timezone using APIs compatible with minSdk 24.

- [ ] **Step 5: Forward snapshots without duplicate persistence**

Add `GameResultRecorder` and `BooleanSupplier achievementsVisible` dependencies to the injectable `GameplayUiController` constructor and call `observe(snapshot)` once at the start of `refresh`. Keep the existing three-argument constructor for isolated tests; it delegates with a recorder whose sink is a no-op and a supplier returning `false`, never null. READY visibility must additionally require `!achievementsVisible.getAsBoolean()`.

- [ ] **Step 6: Wire production objects and Back handling**

In `MainActivity.onCreate`, create `SQLiteGameResultRepository`, `GameResultsCoordinator`, `GameResultRecorder(coordinator::save, System::currentTimeMillis)`, then create `AchievementsUiController` before `GameplayUiController` so `achievementsUiController::isVisible` can be supplied. Register an `OnBackPressedCallback` that first delegates to `AchievementsUiController.handleBack()`; when not consumed, temporarily disable itself before redispatching to preserve default Back behavior without recursion. In `onDestroy`, close the coordinator exactly once after stopping UI work.

- [ ] **Step 7: Run target and full tests to verify GREEN**

Run:

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest --tests '*.AchievementsUiControllerTest' --tests '*.GameplayUiControllerTest' --tests '*.MainActivityTest'
.\gradlew.bat --no-daemon testDebugUnitTest
```

Expected: all tests pass.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/java/com/vanvinh2402/tiltescape/MainActivity.java app/src/main/java/com/vanvinh2402/tiltescape/ui app/src/main/res/layout app/src/main/res/values/strings.xml app/src/test/java/com/vanvinh2402/tiltescape/MainActivityTest.java app/src/test/java/com/vanvinh2402/tiltescape/ui
git commit -m "feat: add local achievements screen"
```

---

### Task 5: Handoff documentation and final verification

**Files:**
- Create: `docs/database-schema.md`
- Modify: `README.md`
- Modify only files required by Critical/Important review findings, each through RED → GREEN.

**Interfaces:**
- Consumes: completed Tasks 1–4.
- Produces: documented SQLite schema, verified persistent achievements APK, final review evidence.

- [ ] **Step 1: Document database and delivered behavior**

`docs/database-schema.md` must include database filename/version, exact `CREATE TABLE` and index SQL, column meanings, high-score/recent queries, timestamp/duration units, retention, and migration rule. Update README to list SQLite achievements as completed and remove SQLite/high score from the missing-feature list.

- [ ] **Step 2: Run clean build verification**

Run:

```powershell
.\gradlew.bat --stop
.\gradlew.bat --no-daemon clean testDebugUnitTest assembleDebug lintDebug
```

Expected: `BUILD SUCCESSFUL`, no test/lint failures, and non-empty `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Verify repository hygiene**

Run `git diff --check`; verify no database, APK, build output, local mockup/session file, or Android Studio file is tracked; verify `gradlew` remains mode `100755`.

- [ ] **Step 4: Request whole-feature review**

Review from the pre-feature base through HEAD against the design and this plan. Fix every Critical/Important finding through a new failing test followed by GREEN. Record Minor findings without broadening scope.

- [ ] **Step 5: Run Pixel 7 persistence smoke test**

Install the fresh APK. Complete two runs and verify exactly two recent rows, newest first, with the correct high score. Force-stop and relaunch; verify both rows persist. Open achievements immediately after Game Over and verify the just-completed run appears. Reopen the screen, Pause/Resume, and Play Again without completing a run; verify no duplicate/extra rows. Verify system Back and `QUAY LẠI` return to READY.

- [ ] **Step 6: Commit and push after verification**

```powershell
git add README.md docs/database-schema.md <review-fix-files-if-any>
git commit -m "docs: document SQLite achievements"
git push origin main
```
