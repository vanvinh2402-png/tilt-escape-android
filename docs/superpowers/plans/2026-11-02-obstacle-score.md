# Obstacle-Pass Scoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace time-derived score with one point for every obstacle that safely leaves the bottom of the playfield.

**Architecture:** `ObstacleField` reports how many obstacles it removed after a complete pass during each update. `GameEngine` owns a run-scoped integer score, adds the reported count only in PLAYING, and continues using capped survival time solely for difficulty.

**Tech Stack:** Java 17, Android Views/Canvas, JUnit 4, Robolectric 4.17, Gradle 8.11.1.

**Spec:** `docs/superpowers/specs/2026-11-02-obstacle-score-design.md`

## Global Constraints

- Award exactly `1` point per obstacle whose top reaches or passes normalized Y `1.0f` without collision.
- Difficulty remains based on capped simulated survival time; its constants do not change.
- `GameEngine` owns score and `ObstacleField` owns movement/removal.
- Reset, invalid delta, non-PLAYING state, collision, and lifecycle changes never create points.
- Preserve Java 17, min SDK 24, target/compile SDK 35, and the current Canvas/Choreographer architecture.
- Every behavior change follows RED → GREEN and each task ends in a focused commit.

## Review Focus

- Two obstacles leaving in the same field update must return `2`, not collapse into one event; Task 1 adds this test.
- A passed obstacle must be counted exactly once on later updates; Task 1 adds this test.
- Invalid field deltas must return zero without movement, spawn, or score events; Task 1 adds this test.
- A collision must end the run with no dodge point for the colliding obstacle; Task 2 adds this test.
- Oversized engine delta must remain capped and cannot skip ahead to award a burst of points; Task 2 adds this test.

---

### Task 1: Report safely passed obstacles from the field

**Files:**
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/game/ObstacleField.java`
- Modify: `app/src/test/java/com/vanvinh2402/tiltescape/game/ObstacleFieldTest.java`

**Interfaces:**
- Consumes: `Obstacle.isBelowWorld()` and existing spawn/removal rules.
- Produces: `int ObstacleField.update(float deltaSeconds, float speed, float spawnInterval, int maximumActive)` returning the number removed below the world in that call.

- [ ] **Step 1: Write failing field-event tests**

Add `updateReturnsEveryObstaclePassedInThatCall()`: reset a field, call `update(0.30f, 1.0f, 0.10f, 2)` to create the second active obstacle, then assert `update(2.0f, 1.0f, 100.0f, 2)` returns `2` and the next non-spawning update returns `0`. Add `invalidDeltaReturnsZeroWithoutChangingField()` for `NaN`, infinity, zero, and negative delta. Existing call sites may ignore the new return value where irrelevant.

- [ ] **Step 2: Run Task 1 tests and verify RED**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest --tests "*.ObstacleFieldTest"`

Expected: compilation failure because `update(...)` still returns `void`.

- [ ] **Step 3: Implement the passed-count return value**

Change `ObstacleField.update(...)` to return `int`. Return `0` for invalid delta, increment a local count only when `isBelowWorld()` causes iterator removal, preserve spawn behavior, and return the count after the update.

- [ ] **Step 4: Run Task 1 tests and full regression suite**

Run the Step 2 command, then `.\gradlew.bat --no-daemon testDebugUnitTest`.

Expected: all tests pass and existing obstacle behavior is unchanged.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vanvinh2402/tiltescape/game/ObstacleField.java app/src/test/java/com/vanvinh2402/tiltescape/game/ObstacleFieldTest.java
git commit -m "feat: report passed obstacles"
```

### Task 2: Store dodge score in the game engine

**Files:**
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/game/GameEngine.java`
- Modify: `app/src/test/java/com/vanvinh2402/tiltescape/game/GameEngineTest.java`
- Modify: `app/src/test/java/com/vanvinh2402/tiltescape/ui/GameplayUiControllerTest.java`

**Interfaces:**
- Consumes: Task 1 `ObstacleField.update(...): int`.
- Produces: `GameSnapshot.getScore()` as the stored number of safely passed obstacles; public snapshot API is unchanged.

- [ ] **Step 1: Replace time-score tests with failing dodge-score tests**

Replace `newRunStartsAtZeroAndAccumulatesSurvivalScore()` with `scoreIncrementsOnlyAfterACompleteObstaclePass()`: use lane `0`, assert a new run starts at `0`, run 71 updates of `0.05f` and assert score remains `0`, then run at most two more updates and assert score becomes `1` while survival time continues independently. Add a collision test using center lane `2` and assert final score stays `0`. Extend the delta test so one `1.0f` input advances only `0.05f` and awards no point.

- [ ] **Step 2: Strengthen reset and frozen-state assertions**

In existing PAUSED/READY/GAME_OVER and restart tests, create or retain a nonzero dodge score first where practical, then assert Pause preserves it while Restart, Play Again, and Main Menu reset it to `0`. In `GameplayUiControllerTest`, replace the old assumption that two `0.05f` updates produce score `1`; use survival time to prove a repeated hidden Restart/Play Again click does not reset an already active run.

- [ ] **Step 3: Run Task 2 tests and verify RED**

Run: `.\gradlew.bat --no-daemon testDebugUnitTest --tests "*.GameEngineTest" --tests "*.GameplayUiControllerTest"`

Expected: assertions fail because snapshots still derive score from `survivalSeconds * 10`.

- [ ] **Step 4: Implement run-scoped score**

Add `private int score` to `GameEngine`. Reset it in `startNewRun()` and `returnToReady()`. In a valid PLAYING update, add the integer returned by `obstacleField.update(...)`. Pass `score` directly into `GameSnapshot`; keep `survivalSeconds` and difficulty-tier calculation unchanged.

- [ ] **Step 5: Run Task 2 tests and full regression suite**

Run the Step 3 command, then `.\gradlew.bat --no-daemon testDebugUnitTest`.

Expected: all score, controls, HUD, and regression tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/vanvinh2402/tiltescape/game/GameEngine.java app/src/test/java/com/vanvinh2402/tiltescape/game/GameEngineTest.java app/src/test/java/com/vanvinh2402/tiltescape/ui/GameplayUiControllerTest.java
git commit -m "feat: score completed obstacle dodges"
```

### Task 3: Documentation and final verification

**Files:**
- Modify: `README.md`
- Modify only files needed by Critical/Important review findings, each through RED → GREEN.

**Interfaces:**
- Consumes: completed Tasks 1–2.
- Produces: documented, verified APK with obstacle-pass scoring.

- [ ] **Step 1: Update score documentation**

Replace README statements that score increases with survival time. State that each safely passed obstacle awards one point and difficulty still increases every 15 seconds.

- [ ] **Step 2: Run clean verification**

Run: `.\gradlew.bat --stop`, then `.\gradlew.bat --no-daemon clean testDebugUnitTest assembleDebug lintDebug`.

Expected: `BUILD SUCCESSFUL`, no test/lint failures, and a non-empty `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Verify Git hygiene and request whole-feature review**

Run `git diff --check`; confirm build output/APK is untracked and `gradlew` remains mode `100755`. Review against the design and this plan; fix every Critical/Important finding through RED → GREEN.

- [ ] **Step 4: Emulator smoke test**

Install the APK on Pixel 7, start a run, avoid one obstacle, and verify HUD changes from `0` to `1` only after the obstacle leaves the bottom. Verify Pause does not change score and Restart returns it to `0`.

- [ ] **Step 5: Commit and push after verification**

```bash
git add README.md <review-fix-files-if-any>
git commit -m "docs: verify obstacle-pass scoring"
git push origin main
```
