# Gameplay Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a playable Android vertical slice in which tilt moves a bounded player, one obstacle falls, collision ends the run, and app lifecycle safely pauses sensor and frame processing.

**Architecture:** Keep deterministic gameplay, sensor filtering, frame timing, and lifecycle orchestration in plain Java classes with no Android framework dependency. Thin Android adapters connect `SensorManager`, `Choreographer`, `Canvas`, and `MainActivity` to those tested interfaces.

**Tech Stack:** Java 17 source, Android XML Views, custom `View`/`Canvas`, `Choreographer`, Android Sensor API, JUnit 4, Gradle 8.11.1, AGP 8.7.2.

**Spec:** `docs/superpowers/specs/2026-10-29-gameplay-core-design.md`

## Global Constraints

- Preserve package/application ID `com.vanvinh2402.tiltescape`, minSdk 24, compile/target SDK 35, and portrait orientation.
- No third-party game or sensor library.
- Use normalized world coordinates; pixels exist only inside the renderer.
- All gameplay movement is based on elapsed seconds, never frame count.
- Production Android adapters remain thin; behavior belongs in plain Java and is developed test-first.
- This slice includes one player, one obstacle, temporary tap start/resume/restart, and no account/database/menu/score/difficulty work.

## Review Focus

- Invalid sensor samples (`NaN`/infinite) must not poison future input; Task 3 tests retaining the last valid filtered value.
- The first frame after attach/resume must advance zero time and later large gaps must cap at `0.05 s`; Task 4 tests both cases.
- Repeated taps/start calls must not reset an active run; Task 2 tests idempotent state transitions.
- Boundary contact must clamp position and clear outward velocity; Task 1 tests both left and right edges.
- Missing accelerometer must show the unsupported state and prevent registration/start; Task 5 tests the plain-Java session controller and verifies the Android branch by build/manual smoke test.

---

### Task 1: Player motion and normalized geometry

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/game/WorldRect.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/game/PlayerMotion.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/game/WorldRectTest.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/game/PlayerMotionTest.java`

**Interfaces:**
- Consumes: normalized input `[-1, 1]` and elapsed seconds.
- Produces: `WorldRect.overlaps(WorldRect)`, plus `PlayerMotion.reset()`, `update(float input, float deltaSeconds)`, `getCenterX()`, `getCenterY()`, `getVelocityX()`, and `getBounds()`.

- [ ] **Step 1: Write failing geometry tests**

Create tests named `overlapRequiresPositiveArea`, `touchingEdgesDoNotOverlap`, and `separatedRectanglesDoNotOverlap`, asserting rectangle intersection semantics used by collision.

- [ ] **Step 2: Run geometry tests and verify RED**

Run: `.\gradlew.bat testDebugUnitTest --tests "*.WorldRectTest"`

Expected: compilation failure because `WorldRect` does not exist.

- [ ] **Step 3: Implement immutable `WorldRect`**

Provide `left`, `top`, `right`, `bottom` getters and `boolean overlaps(WorldRect other)`. Reject no inputs; callers own normalized geometry validity.

- [ ] **Step 4: Run geometry tests and verify GREEN**

Run the Step 2 command; expected all `WorldRectTest` tests pass.

- [ ] **Step 5: Write failing player-motion tests**

Test neutral rest, positive/negative direction, stronger braking at neutral, comparable displacement across `10 × 0.01 s` and `5 × 0.02 s`, left/right boundary clamp with outward velocity cleared, and invalid/non-positive delta producing no change.

- [ ] **Step 6: Run player-motion tests and verify RED**

Run: `.\gradlew.bat testDebugUnitTest --tests "*.PlayerMotionTest"`

Expected: compilation failure because `PlayerMotion` does not exist.

- [ ] **Step 7: Implement `PlayerMotion`**

Use named constants for normalized player size, initial center, maximum speed, acceleration, and braking. Clamp input to `[-1, 1]`; move velocity toward target velocity; integrate position; clamp bounds and clear only outward velocity at an edge.

- [ ] **Step 8: Run Task 1 tests and commit**

Run: `.\gradlew.bat testDebugUnitTest --tests "*.WorldRectTest" --tests "*.PlayerMotionTest"`

Expected: all Task 1 tests pass.

Commit: `feat: add time-based player motion`

### Task 2: Game state, obstacle, and collision

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/game/GameState.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/game/Obstacle.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/game/GameSnapshot.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/game/GameEngine.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/game/GameEngineTest.java`

**Interfaces:**
- Consumes: `PlayerMotion` and `WorldRect` from Task 1.
- Produces: `GameEngine.getState()`, `startOrRestart()`, `pause()`, `resume()`, `update(float horizontalInput, float deltaSeconds)`, and immutable `snapshot()` containing state/player/obstacle geometry.

- [ ] **Step 1: Write failing engine state tests**

Assert initial `READY`; start transitions to `PLAYING`; repeated start while playing preserves current positions; pause only affects `PLAYING`; resume only affects `PAUSED`; restart from `GAME_OVER` resets player velocity and geometry.

- [ ] **Step 2: Write failing obstacle/collision tests**

Assert obstacle displacement is time-based; leaving the bottom resets it above the top at a deterministic X; separated rectangles stay `PLAYING`; overlap transitions once to `GAME_OVER`; `PAUSED` and `GAME_OVER` updates do not move entities.

- [ ] **Step 3: Run tests and verify RED**

Run: `.\gradlew.bat testDebugUnitTest --tests "*.GameEngineTest"`

Expected: compilation failure because engine types do not exist.

- [ ] **Step 4: Implement state and engine types**

Keep `Obstacle` and `PlayerMotion` mutable only inside the engine; return immutable snapshot values. Collision is evaluated only after a valid `PLAYING` update and changes state once.

- [ ] **Step 5: Run tests and commit**

Run the Step 3 command; expected all `GameEngineTest` tests pass.

Commit: `feat: add gameplay state and collision`

### Task 3: Time-aware tilt signal processing

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/input/TiltInput.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/input/TiltSignalProcessor.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/input/TiltSignalProcessorTest.java`

**Interfaces:**
- Consumes: raw portrait accelerometer X in `m/s²` and sensor timestamp nanoseconds.
- Produces: `TiltInput.getHorizontalInput()` and processor methods `onSample(float rawX, long timestampNanos)`, `reset()`, `getHorizontalInput()`.

- [ ] **Step 1: Write failing processor tests**

Assert first valid sample initializes without a time jump; sign is corrected so physical left/right matches the documented direction; output stays in `[-1, 1]`; values inside `0.08 g` dead zone become zero; `0.60 g` reaches full magnitude; equal elapsed time at different sample rates gives comparable filtered output; invalid samples/timestamps preserve the last valid output; reset returns neutral.

- [ ] **Step 2: Run tests and verify RED**

Run: `.\gradlew.bat testDebugUnitTest --tests "*.TiltSignalProcessorTest"`

Expected: compilation failure because processor types do not exist.

- [ ] **Step 3: Implement the processor**

Use gravity `9.80665f`, dead zone `0.08 g`, full tilt `0.60 g`, and a named low-pass time constant. Compute filter alpha from elapsed sensor seconds, ignore non-finite values/non-increasing timestamps, dead-zone and rescale after filtering, then clamp.

- [ ] **Step 4: Run tests and commit**

Run the Step 2 command; expected all processor tests pass.

Commit: `feat: add filtered tilt input`

### Task 4: Frame timing and lifecycle orchestration

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/game/FrameClock.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/game/GameLoop.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/input/SensorRegistration.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/GameSessionController.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/game/FrameClockTest.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/GameSessionControllerTest.java`

**Interfaces:**
- Consumes: engine state plus abstract `GameLoop.start()/stop()` and `SensorRegistration.isAvailable()/register()/unregister()`.
- Produces: `FrameClock.nextDeltaSeconds(long frameTimeNanos)`, `reset()`, and controller methods `onResume()`, `onPause()`, `canPlay()`.

- [ ] **Step 1: Write failing frame-clock tests**

Assert first frame is `0`, normal nanosecond differences convert to seconds, non-increasing time returns `0`, a long gap caps at `0.05f`, and reset makes the next frame `0` again.

- [ ] **Step 2: Write failing session-controller tests**

Using hand-written fakes, assert available sensor registers and starts loop on resume; pause stops loop, unregisters once, pauses a playing engine, and is idempotent; missing sensor never registers/starts and `canPlay()` is false.

- [ ] **Step 3: Run tests and verify RED**

Run: `.\gradlew.bat testDebugUnitTest --tests "*.FrameClockTest" --tests "*.GameSessionControllerTest"`

Expected: compilation failure because timing/controller types do not exist.

- [ ] **Step 4: Implement clock, ports, and controller**

Keep controller free of Android classes. It owns no rendering or sensor math; it coordinates the provided interfaces and `GameEngine` lifecycle only.

- [ ] **Step 5: Run tests and commit**

Run the Step 3 command; expected all Task 4 tests pass.

Commit: `feat: coordinate game lifecycle`

### Task 5: Android sensor, Canvas view, and activity integration

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/input/AccelerometerTiltInput.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/ui/GameView.java`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/MainActivity.java`
- Replace: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/ui/GameViewTest.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/MainActivityTest.java`

**Interfaces:**
- Consumes: Tasks 2–4 interfaces and snapshots.
- Produces: Android `SensorRegistration`/`TiltInput` adapter, a `GameLoop`/Canvas view, and activity lifecycle wiring.

- [ ] **Step 1: Configure Robolectric and write failing Android behavior tests**

Enable Android resources for local unit tests and add `org.robolectric:robolectric:4.17`. In `GameViewTest`, construct a real engine plus constant fake `TiltInput` and assert `performClick()` transitions `READY → PLAYING`, `PAUSED → PLAYING`, and `GAME_OVER → PLAYING` with reset geometry. In `MainActivityTest`, use Robolectric's sensor shadow to assert a missing accelerometer renders `sensor_unavailable` and does not expose a playable view; with an accelerometer present, assert the activity creates a `GameView`, resumes, pauses, and destroys without duplicate-registration or lifecycle exceptions.

- [ ] **Step 2: Run Android behavior tests and verify RED**

Run: `.\gradlew.bat testDebugUnitTest --tests "*.GameViewTest" --tests "*.MainActivityTest"`.

Expected: compilation failure because `GameView` and integrated `MainActivity` behavior do not exist.

- [ ] **Step 3: Implement `AccelerometerTiltInput`**

Wrap `SensorManager`, expose availability, register with `SENSOR_DELAY_GAME`, unregister idempotently, forward X/timestamps into `TiltSignalProcessor`, and implement `TiltInput` plus `SensorRegistration`.

- [ ] **Step 4: Implement `GameView`**

Implement `Choreographer.FrameCallback` and `GameLoop`. Use `FrameClock`, feed latest tilt into `GameEngine`, render snapshots with reusable `Paint` objects, cancel callbacks when stopped/detached, and map taps by state: start, resume, or restart. Render explicit labels for `READY`, `PAUSED`, and `GAME_OVER`.

- [ ] **Step 5: Wire `MainActivity` and resources**

Construct engine, processor, sensor adapter, view, and controller; delegate `onResume/onPause`; show `sensor_unavailable` and disable start when unavailable. Keep temporary tap instructions in string resources.

- [ ] **Step 6: Run tests and debug build**

Run: `.\gradlew.bat clean testDebugUnitTest assembleDebug`

Expected: all unit tests pass and `app/build/outputs/apk/debug/app-debug.apk` is non-empty.

- [ ] **Step 7: Perform device smoke test when a device is available**

Verify portrait launch, correct left/right direction, neutral settling, boundary clamp, obstacle motion, collision/Game Over, tap restart, background pause, and no duplicate sensor/frame registrations. If no device is connected, record this as an explicit unverified manual criterion rather than claiming it passed.

- [ ] **Step 8: Update README and commit**

Document temporary controls and verification commands. Also narrow the base README JDK statement to supported JDK 17 or 21.

Commit: `feat: integrate tilt gameplay prototype`

### Task 6: Whole-feature verification and delivery

**Files:**
- Modify only files required by Critical/Important review findings, each through a new failing test.

**Interfaces:**
- Consumes: completed Tasks 1–5.
- Produces: reviewed, pushed gameplay-core commits on the approved integration branch.

- [ ] **Step 1: Run full verification**

Run: `.\gradlew.bat clean testDebugUnitTest assembleDebug`

Expected: `BUILD SUCCESSFUL`, all tests pass, APK non-empty, and `git diff --check` is clean.

- [ ] **Step 2: Request independent whole-branch review**

Review against the design, this plan, the Review Focus list, and all ledger rulings. Fix Critical/Important findings through RED→GREEN; defer Minor findings explicitly.

- [ ] **Step 3: Verify Git hygiene and push**

Confirm no `local.properties`, `.gradle`, `.idea`, `build`, or APK output is tracked; confirm `gradlew` remains mode `100755`; push only after the final clean verification.

