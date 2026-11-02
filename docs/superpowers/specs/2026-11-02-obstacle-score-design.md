# Obstacle-Pass Scoring Design

**Date:** 2026-11-02
**Status:** Proposed
**Scope:** Replace survival-time scoring with one point for every obstacle safely passed.

## Intent

Make the displayed score represent successful dodges rather than elapsed time. This keeps scores understandable and prevents them from growing too quickly while preserving the existing endless-dodging gameplay.

## Goals

- Start every new run at score `0`.
- Award exactly `1` point when one obstacle moves completely below the playfield without colliding with the player.
- Award every passed obstacle when multiple obstacles leave during the same update.
- Keep the final score frozen in PAUSED, READY, and GAME_OVER.
- Keep difficulty progression based on capped simulated survival time.
- Preserve the current HUD, final-score panel, restart, play-again, and lifecycle behavior.

## Non-goals

- SQLite, personal high scores, accounts, and leaderboard persistence are not part of this change; they follow after the scoring rule is stable.
- No bonus, combo, multiplier, near-miss, or difficulty-based point values.
- No visual or audio dodge effect in this iteration.
- No change to obstacle spawning or difficulty constants.

## Constraints

- Java 17 and the current pure-Java game engine architecture remain unchanged.
- `GameEngine` remains the owner of run score.
- `ObstacleField` remains the owner of obstacle creation, movement, and removal.
- Resetting a field or starting a run must never generate points.
- Invalid, non-finite, zero, negative, or paused updates must never generate points.

## Approaches considered

### 1. Return the passed-obstacle count from `ObstacleField.update()` — selected

`ObstacleField` already knows exactly which obstacles are removed after crossing the lower boundary. Its update method returns that count, and `GameEngine` adds it to the current run score. This avoids duplicating removal rules and naturally handles several obstacles leaving in one update.

### 2. Compare obstacle snapshots in `GameEngine`

The engine could compare bounds before and after each update. This duplicates field logic and can miss an obstacle after the field removes it, so it was rejected.

### 3. Emit an obstacle-passed callback

A callback would support effects later, but adds stateful wiring and duplicate-callback risk for a simple counter. It was rejected for this iteration.

## Data flow

1. `GameEngine.update(input, delta)` validates and caps delta as it does today.
2. `ObstacleField.update(...)` advances obstacles, removes every obstacle whose top has reached or passed the lower playfield boundary, and returns the number removed for that reason.
3. `GameEngine` adds the returned count to a run-scoped integer score.
4. Collision detection runs against the remaining obstacles.
5. `GameSnapshot.getScore()` exposes the stored obstacle-pass count to the existing HUD and Game Over UI.
6. Survival seconds continue to advance independently and drive `DifficultyPolicy` only.

## State and edge cases

- READY: score is `0`; no updates award points.
- PLAYING: each fully passed obstacle awards one point exactly once.
- PAUSED: score and survival time remain unchanged.
- GAME_OVER: final score remains unchanged.
- Restart/Play Again: score, survival time, player, and obstacle field reset.
- Main Menu: returns to READY with score `0`.
- Two or more obstacles leaving in the same update award the matching number of points.
- An obstacle removed by reset is not a dodge and awards no point.

## Test strategy

- Field tests verify zero, one, and multiple passed-obstacle counts and confirm reset produces no score event.
- Engine tests verify score starts at zero, increments only after a complete pass, counts multiple passes, freezes in PAUSED/GAME_OVER, ignores invalid delta, and resets on a new run or Main Menu.
- Existing HUD and controls tests verify the new score is displayed without changing UI state behavior.
- The full unit-test, APK assembly, and lint suites run before integration.

## Follow-up

After this rule is implemented and verified, the next project phase adds SQLite tables and repositories for accounts and persisted personal high scores.
