# Gameplay v2 Design

**Date:** 2026-10-30
**Status:** Proposed
**Scope:** Complete the playable offline gameplay loop before account and SQLite work.

## Intent

Turn the current one-obstacle technical prototype into a coherent game loop that can be tested on the Pixel 7 emulator and a physical Android device. This iteration fixes the visible Android 15 layout defect, makes obstacle patterns vary, adds score and progressive difficulty, and supplies real pause and game-over controls.

## Goals

- Keep all content below Android system bars on API 24–35.
- Spawn obstacles at varied, valid horizontal positions.
- Prevent obviously unavoidable spawn patterns through bounded density and lane rules.
- Score survival time consistently without counting paused or background time.
- Increase difficulty gradually while keeping hard caps on speed and density.
- Provide usable HUD, Pause, Resume, Restart, Main Menu, Game Over, and Play Again actions.
- Preserve lifecycle safety: no duplicate sensor listeners or frame callbacks.
- Cover deterministic game rules with JVM tests and Android wiring with Robolectric tests.

## Non-goals

- Login, registration, session management, SQLite, high-score persistence, and leaderboard.
- Final art, animation, sound, vibration, skins, maps, or online services.
- A production Main Menu matching the complete nine-screen SRS. In this iteration, Main Menu means the local READY screen for entering a run; the full navigation screen comes later.

## Constraints

- Android Java, XML Views, Canvas, and the existing `GameEngine` architecture remain in use.
- Portrait orientation, min SDK 24, compile/target SDK 35.
- Gameplay calculations remain normalized and delta-time based.
- Random behavior must be injectable so tests are deterministic.
- Physical tilt direction still requires a real-device smoke test; emulator tests use Virtual Sensors.

## Approaches considered

### 1. Incrementally extend the existing engine — selected

Add score, difficulty, deterministic spawning, and obstacle collections to the existing pure-Java model. Keep Android rendering and controls as adapters around snapshots.

**Trade-off:** Some existing single-obstacle APIs must evolve, but core behavior remains testable and the current code is reused.

### 2. Build UI overlays first and leave the one-obstacle engine intact

This would quickly improve the screenshot, but score, difficulty, and obstacle variation would remain disconnected from the model. It would create throwaway UI logic and does not address the user's observed gameplay limitation.

### 3. Replace the loop with a game framework or large Android rewrite

This could provide richer scene management, but it adds dependencies and migration risk without helping the required simple 2D game. It conflicts with the incremental Java base already verified.

## Design

### 1. System-bar-safe layout

`MainActivity` applies `WindowInsetsCompat.Type.systemBars()` to the root gameplay container. The original XML padding is retained and system-bar insets are added, rather than hard-coding status-bar sizes. The instruction/HUD row therefore starts below the status bar on Android 15 while remaining correct on older supported APIs.

The top row becomes a compact HUD containing score and a Pause button. `GameView` is laid out below this HUD rather than drawing behind it, so normalized world coordinates describe only the visible playfield. READY instructions move into the centered state overlay so they do not compete with system icons.

### 2. Obstacle spawning

Replace the engine's single fixed obstacle with an `ObstacleField` that owns a bounded list of active obstacles. A small `RandomSource` interface supplies deterministic values in tests and a seeded/platform implementation in production.

Horizontal positions use five logical lanes inside the playable bounds. A new spawn:

- stays fully inside the world;
- avoids repeating the immediately previous lane when alternatives exist;
- never creates more than the current difficulty cap;
- respects a minimum spawn interval and vertical separation;
- recycles/removes obstacles after they leave the bottom.

These rules avoid obviously unfair bursts without pretending to mathematically solve every future player path. Speed, interval, and maximum count are capped so a navigable gap remains.

Production randomness continues across Restart and Play Again, so a new run does not replay the same pattern. Tests inject a fake sequence and explicitly control it; resetting gameplay does not reset the `RandomSource`.

### 3. Score and difficulty

The engine tracks accumulated simulation time only while state is `PLAYING`. It uses the same frame delta capped by `FrameClock` at 50 ms, so a foreground stall does not award free score while gameplay is frozen. Score is `floor(survivalSeconds * 10)`, begins at zero, and is included in `GameSnapshot`.

Difficulty advances every 15 seconds:

- obstacle fall speed increases by a small fixed step;
- spawn interval decreases;
- maximum simultaneous obstacles increases at defined tiers;
- each value has a hard cap.

The initial tuning table is a builder-owned balancing baseline:

| Value | Rule |
|---|---|
| Tier | `floor(survivalSeconds / 15)` |
| Fall speed | `min(0.30 + tier * 0.05, 0.65)` world heights/second |
| Spawn interval | `max(1.40 - tier * 0.12, 0.65)` seconds |
| Maximum active obstacles | 1 for tiers 0–1, 2 for tiers 2–3, 3 from tier 4 onward |
| Logical lanes | 5 evenly spaced lanes within obstacle half-width margins |
| Minimum vertical separation | `0.22` world heights at spawn time |

These constants live in one difficulty policy class so later device testing can tune them without rewriting engine logic.

Pause, backgrounding, READY, and GAME_OVER do not advance elapsed time, score, spawning, or movement. The final score is frozen when collision changes state to GAME_OVER.

### 4. Gameplay states and controls

The existing states remain `READY`, `PLAYING`, `PAUSED`, and `GAME_OVER`.

- READY overlay: title, short tilt instruction, and Start Game.
- PLAYING HUD: current score and Pause.
- PAUSED overlay: Resume, Restart, Main Menu.
- GAME_OVER overlay: final score, Play Again, Main Menu.

`Restart` always creates a zero-score run. `Main Menu` resets to READY. `Resume` continues the same engine snapshot. `Play Again` resets the run. The UI controller observes state changes after frames and user actions, then updates overlay visibility and labels.

The frame loop schedules callbacks only in PLAYING. Starting, resuming, or replaying restarts it; pausing, collision, backgrounding, or detachment stops it.

### 5. Rendering and snapshots

`GameSnapshot` exposes an immutable obstacle-bounds collection, state, player bounds, score, elapsed time, and difficulty tier. `GameView` iterates the snapshot collection with reusable `Paint` objects and does not mutate engine internals.

State text and button labels live in string resources. The overlay supplies accessible Android controls; the Canvas itself receives a concise content description for the play field.

### 6. Testing

Pure JVM tests cover:

- lane selection with a fake random sequence;
- world-bound and separation rules;
- obstacle recycling and bounded collection size;
- score progression only during PLAYING;
- difficulty tier thresholds and caps;
- pause/resume, restart, Main Menu, and frozen Game Over score;
- collision against every active obstacle.

Robolectric tests cover:

- API 35 system-bar insets applied to the HUD;
- correct overlay/buttons for all four states;
- Pause/Resume/Restart/Main Menu/Play Again transitions;
- no tilt reads or repeated frame scheduling outside PLAYING.

Final verification runs `clean testDebugUnitTest assembleDebug`, checks APK output and Git hygiene, then performs emulator smoke testing. Physical tilt direction and feel remain explicitly unverified until tested on a real device.

## Delivery sequence

1. Fix system-bar insets and HUD structure.
2. Add deterministic varied obstacle spawning and safe bounds.
3. Add score and HUD updates.
4. Add capped progressive difficulty.
5. Add Pause controls and overlay.
6. Add complete Game Over controls and local READY/Main Menu behavior.
7. Run full automated verification and emulator smoke tests.

## Risks and mitigations

- **Unfair obstacle patterns:** use discrete lanes, separation, interval limits, and density caps; add deterministic scenario tests.
- **Frame/lifecycle duplication:** retain idempotent loop and sensor registration guards and test repeated lifecycle transitions.
- **State/UI divergence:** derive visible controls from the engine snapshot after every action instead of maintaining a second independent UI state.
- **Scope creep into persistence:** display only the current/final score in this iteration; high-score persistence waits for the SQLite phase.
