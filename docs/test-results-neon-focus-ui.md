# Neon Focus UI verification results

Date: 2026-11-06 (Asia/Bangkok)

## Automated clean verification

Commands:

```powershell
.\gradlew.bat --stop
.\gradlew.bat --no-daemon --max-workers=1 clean testDebugUnitTest assembleDebug lintDebug
```

Result: **BUILD SUCCESSFUL** in 53 seconds; 50 actionable tasks executed.

- Unit tests: 136
- Failures: 0
- Errors: 0
- Android Lint: 0 errors, 19 warnings
- Lint report: `app/build/reports/lint-results-debug.html`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- APK size: 3,238,076 bytes

## Covered Neon Focus behavior

Automated tests verify:

- Approved palette, 48dp minimum touch target, 52dp controls and all Neon drawables load.
- Authentication fields use explicit readable text/hint colors and the focused/default Neon input selector.
- Login/Register cards remain inside full-height ScrollViews and retain every controller-facing view ID.
- HUD remains before the game container and the root uses the approved gradient.
- Start, Pause, Resume, Restart, Replay and menu actions meet the touch/color contract.
- Game canvas uses the approved background, player and obstacle colors without changing engine behavior.
- Achievements uses the Neon background, readable loading/empty/error colors, scrollable results, styled rows and Back action.
- Existing authentication, gameplay, lifecycle, SQLite and account-scoped achievement tests remain green.

## Pixel 7 emulator smoke

Device: `emulator-5554`. APK installation with `adb install -r` succeeded and preserved app data.

Verified manually:

- App launches without crash and respects status/navigation insets.
- Login and Register cards render with the Neon surface, readable Vietnamese text, visible hints and gradient primary action.
- Register navigation works and all three fields remain visible.
- Focused input shows the purple focus stroke and teal cursor.
- Player/game background remain visible behind the overlay.

Pending manual checks:

- Soft keyboard resize/scroll was not confirmed because the emulator kept the hardware keyboard mode.
- Ready, Pause, Game Over and Achievements were not opened manually because no existing authenticated session was modified or test account created. Their layouts and state behavior are covered by Robolectric tests.

## Compatibility ruling

`android:windowLightNavigationBar` is defined only in `values-v27/themes.xml`. API 24–26 use the dark base theme and explicit dark navigation-bar color, while API 27+ explicitly keeps light navigation icons.

## Scope preserved

No account, SQLite, score, tilt, collision or game-state logic changed. No external UI library or image asset was added.
