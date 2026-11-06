# Local accounts verification results

Date: 2026-11-06 (Asia/Bangkok)

## Automated clean verification

Command:

```powershell
.\gradlew.bat --stop
.\gradlew.bat --no-daemon clean testDebugUnitTest assembleDebug lintDebug
```

Result: **BUILD SUCCESSFUL** in 35 seconds; 50 actionable tasks executed.

- Unit tests: 123
- Failures: 0
- Errors: 0
- Android Lint: passed; report at `app/build/reports/lint-results-debug.html`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- APK size: 3,227,598 bytes

## Covered scenarios

Automated tests verify:

- Username trimming, ASCII rules, length boundaries and case normalization.
- Password validation, PBKDF2 algorithms, unique salts, correct/incorrect verification and defensive byte copies.
- SQLite version-1 to version-2 migration preserves legacy scores and leaves `user_id = NULL`.
- Fresh users schema, unique normalized usernames, foreign keys and account/result indexes.
- Account insert/read, duplicate handling and password metadata.
- Persisted session save/restore/clear and malformed/stale preference cleanup.
- Registration, login, session restore, logout, double-submit suppression and stale callback suppression.
- No-session gameplay gate and the no-accelerometer Login shell.
- Results and achievements are isolated between two account IDs and exclude Guest rows.
- A run retains the account captured on its first Playing snapshot even if the active account supplier changes before Game Over.

## Device and upgrade smoke status

`adb devices` returned no connected devices during this verification. Therefore the following manual checks remain pending and are not claimed as passed:

- Install an older APK, create Guest history, then install this APK with `adb install -r`.
- Register two named accounts on Pixel 7, reject a case-variant duplicate and complete one run per account.
- Confirm isolated scores, force-stop/relaunch session restoration and Logout data preservation.
- Check PBKDF2 responsiveness and tilt behavior on a physical Android device.

The migration, account isolation, session and no-sensor paths are covered by the automated tests above, but the physical/emulator smoke checklist should still be completed before final external delivery.

## Known limitations

- Accounts and results are local to one app installation.
- No password recovery, password change or account deletion.
- No online Leaderboard or multi-device synchronization.
- Clearing app data or uninstalling removes accounts and results.
