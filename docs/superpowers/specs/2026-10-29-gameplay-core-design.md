# Gameplay Core Design

**Date:** 2026-10-29

## Intent

Build the first playable vertical slice of TILT ESCAPE and validate its highest-risk behavior: a player controlled by device tilt inside a stable, lifecycle-aware Android game loop. The result should feel responsive and natural on a real phone while keeping the game logic deterministic and testable as plain Java.

## Goals

- Render a player and one falling obstacle with a custom Android `View` and `Canvas`.
- Advance game state from elapsed time (`deltaTime`), not frame count.
- Read the accelerometer only while the activity is active.
- Convert tilt into horizontal target velocity with noise filtering, a dead zone, speed limits, and light acceleration/deceleration.
- Keep the player inside the playfield.
- Detect player-obstacle overlap and transition to `GAME_OVER` exactly once.
- Pause updates when the activity is backgrounded and resume safely without a large time jump.
- Unit-test motion, boundaries, state transitions, and collision without Android framework objects.

## Non-goals

- Login, registration, SQLite, settings UI, leaderboard, score persistence, difficulty progression, art, audio, vibration, menus, or polished HUD.
- Multiple simultaneous obstacles or object pooling.
- Release signing or store-ready packaging.
- A general-purpose game engine.

## Constraints

- Java source and XML Views on the existing Android project.
- `minSdk 24`, portrait orientation, one `app` module.
- No third-party game or sensor library.
- Production rendering remains on the UI thread for this small vertical slice.
- Game logic uses normalized world coordinates so behavior is independent of screen pixels.

## Approaches considered

### Selected: custom `View + Canvas + Choreographer`

`GameView` renders with `Canvas`; `Choreographer.FrameCallback` supplies display-paced callbacks. This is the smallest Android-native approach, keeps lifecycle ownership visible, and is sufficient for a player plus a small obstacle set.

### Rejected for now: `SurfaceView` with a dedicated game thread

It offers stronger control for heavier scenes but introduces thread lifecycle, synchronization, and surface ownership complexity before measurements justify it. It remains a migration option if profiling later shows the UI-thread renderer cannot meet the frame target.

### Rejected: external game engine

It would add dependency and project complexity, obscure the Android sensor/lifecycle learning goals, and exceed the current game scope.

## Architecture

### Pure Java game model

- `GameEngine`: owns `GameState`, the player, obstacle, collision, and time-based updates.
- `GameState`: `READY`, `PLAYING`, `PAUSED`, `GAME_OVER`.
- `Player`: normalized center position, width, height, and horizontal velocity.
- `Obstacle`: normalized rectangle and vertical velocity.
- `RectF` from Android is not used in the model; collision uses a small plain-Java rectangle/value type.

The engine exposes immutable snapshots for rendering. `GameView` must not mutate model fields directly.

### Input boundary

- `TiltInput`: interface returning the latest normalized horizontal input in `[-1, 1]`.
- `AccelerometerTiltInput`: Android adapter implementing `SensorEventListener`.
- Tests use a deterministic fake `TiltInput`.

The adapter reads accelerometer X, corrects direction for the locked portrait orientation, divides by gravitational acceleration, clamps the useful range, applies a time-aware low-pass filter, and then applies the dead zone.

### Rendering and frame scheduling

- `GameView` owns a `Choreographer.FrameCallback`.
- The first callback establishes a timestamp and does not advance the model.
- Later callbacks compute seconds elapsed and cap a single update to `0.05 s` to prevent a background/resume jump.
- When `PLAYING`, the view reads tilt, updates the engine, invalidates, and schedules the next callback.
- When `READY`, `PAUSED`, or `GAME_OVER`, it renders the state but performs no gameplay update.
- `onDetachedFromWindow()` cancels callbacks.

### Activity lifecycle

- `MainActivity.onResume()` registers the sensor listener and resumes frame scheduling.
- `MainActivity.onPause()` unregisters the listener, pauses the engine, cancels scheduling, and clears the last frame timestamp.
- If no accelerometer exists, the activity shows an unsupported-device message and does not start gameplay.

For this vertical slice, tapping the game view starts or restarts a run. Menus will replace this temporary entry action later.

## Control model

The control is velocity-based with light inertia rather than direct position mapping or full physical sliding.

1. Normalize the horizontal accelerometer component by gravity and clamp the useful magnitude.
2. Apply a low-pass filter whose coefficient derives from elapsed sensor time and a short time constant, so behavior does not depend on sensor sampling frequency.
3. Apply a small dead zone; input inside it becomes zero.
4. Rescale input outside the dead zone back to `[0, 1]` while preserving sign.
5. Compute `targetVelocity = input * maxHorizontalSpeed`.
6. Move current velocity toward target velocity using acceleration; use stronger deceleration when input returns to zero.
7. Integrate `position += velocity * deltaTime` and clamp to the playfield, zeroing velocity when a boundary is hit.

Initial tuning values are builder calls and must be named constants. Suggested starting points are a dead zone near `0.08 g`, useful full tilt near `0.60 g`, maximum traversal near one playfield width per second, and faster braking than acceleration. These values are tuned on a real device after the deterministic behavior is implemented.

## Game flow

1. Initial state is `READY`, player centered near the bottom, obstacle above the visible playfield.
2. Tap transitions to `PLAYING` and resets player and obstacle.
3. Each update moves the player horizontally and obstacle vertically using `deltaTime`.
4. An obstacle leaving the bottom is reset above the top at a deterministic horizontal position for this slice.
5. Rectangle overlap transitions once to `GAME_OVER`; movement stops.
6. Tap from `GAME_OVER` resets and starts a new run.
7. Backgrounding while playing transitions to `PAUSED`; returning to the app renders paused state and requires a tap to continue, preventing an unexpected immediate collision.

## Error and edge handling

- Missing accelerometer: explicit message, no crash, no gameplay start.
- Invalid sensor values (`NaN`/infinite): ignored; last valid filtered input remains.
- Invalid or negative `deltaTime`: no update.
- Large `deltaTime`: capped by the frame owner before engine update.
- Multiple start/restart taps: state transition remains idempotent for the current state.
- Collision is processed only in `PLAYING` and cannot fire repeatedly in `GAME_OVER`.

## Testing

Use TDD for all pure Java behavior:

- Neutral input leaves a centered player still.
- Input magnitude controls target direction and speed.
- Returning to neutral brakes the player to rest.
- Player position clamps at both boundaries and velocity does not keep pushing past them.
- Movement is equivalent for comparable elapsed time split across different frame sizes within tolerance.
- Obstacle movement is time-based and resets after leaving the playfield.
- Non-overlapping rectangles remain `PLAYING`; overlap transitions to `GAME_OVER` once.
- `PAUSED` and `GAME_OVER` do not update positions.
- Restart restores initial positions and velocity.

Android integration is verified with a debug build and a device/emulator smoke test. Real-device tuning validates direction, dead zone, filtering, and responsiveness; those subjective tuning values are not asserted in local unit tests.

## Success criteria

- Clean unit test suite and debug APK build.
- On a real portrait Android device, tilting left/right moves the player in the expected direction, neutral posture settles it quickly, and hand tremor does not cause visible drift.
- The player stays on screen, obstacle motion remains consistent, collision stops the run, and backgrounding never advances the game or leaks sensor registration.
