# Neon Focus UI verification results

Date: 2026-11-06 (Asia/Bangkok)

## Automated clean verification

Commands:

```powershell
.\gradlew.bat --stop
.\gradlew.bat --no-daemon --max-workers=1 clean testDebugUnitTest assembleDebug lintDebug
```

Result: **BUILD SUCCESSFUL** in 50 seconds; 50 actionable tasks executed.

- Unit tests: 138
- Failures: 0
- Errors: 0
- Android Lint: 0 errors, 19 warnings
- Lint report: `app/build/reports/lint-results-debug.html`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- APK size: 3,238,372 bytes

## Covered Neon Focus behavior

Automated tests verify:

- Approved palette, 48dp minimum touch target, 52dp controls and all Neon drawables load.
- Primary, secondary and ghost buttons expose a visible keyboard-focus state for API 24+.
- The root layout uses the larger of system-bar and IME bottom insets, keeping forms above the soft keyboard.
- Authentication fields use explicit readable text/hint colors and the focused/default Neon input selector.
- Login/Register cards remain inside full-height ScrollViews and retain every controller-facing view ID.
- HUD remains before the game container and the root uses the approved gradient.
- Start, Pause, Resume, Restart, Replay and menu actions meet the touch/color contract.
- Game canvas uses the approved background, player and obstacle colors without changing engine behavior.
- Achievements uses the Neon background, readable loading/empty/error colors, scrollable results, styled rows and Back action.
- Existing authentication, gameplay, lifecycle, SQLite and account-scoped achievement tests remain green.

## Pixel 7 emulator end-to-end

Device: `emulator-5554`. APK installation with `adb install -r` succeeded and preserved app data.

Verified manually:

- App launches without crash and respects status/navigation insets.
- Login and Register cards render with the Neon surface, readable Vietnamese text, visible hints and gradient primary action.
- Local account `e2e1106a` can register, log in, log out and log in again.
- The Ready, Running, Pause, Resume, Restart, Game Over and main-menu transitions work.
- Emulator accelerometer input moves the player horizontally.
- Passing one obstacle changes the score from 0 to 1; a later collision records final score 1.
- Achievements shows high score 1 and the two recorded SQLite results (scores 1 and 0).
- Force-stop and relaunch restore the authenticated session and local data.
- With the soft keyboard open, the game area resizes, Register becomes scrollable and both final actions remain above the keyboard.
- Process log inspection found no fatal exception, ANR or SQLite exception.

## Compatibility ruling

`android:windowLightNavigationBar` is defined only in `values-v27/themes.xml`. API 24–26 use the dark base theme and explicit dark navigation-bar color, while API 27+ explicitly keeps light navigation icons.

## Scope preserved

No account, SQLite, score, tilt, collision or game-state logic changed. No external UI library or image asset was added.
