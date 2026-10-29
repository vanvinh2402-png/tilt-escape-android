# TILT ESCAPE

TILT ESCAPE is a portrait Android game in which the player tilts the device to dodge falling obstacles.

## Current status

This repository contains a buildable Android Java prototype with tilt-controlled player motion, a falling obstacle, collision/Game Over, pause/resume, and tap-to-restart behavior. Authentication, persistence, settings, and leaderboard features will be added incrementally.

## Requirements

- Android Studio with Android SDK 35
- JDK 17 or 21 (Android Studio's bundled JDK is supported)

## Open and run

1. Clone the repository.
2. Open the repository root in Android Studio.
3. Allow Gradle sync to complete.
4. Run the `app` configuration on an Android device or emulator running API 24 or newer.

## Command-line verification

On Windows:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Prototype controls

- Hold the device in portrait orientation.
- Tap the game area to start, resume, or restart after Game Over.
- Tilt the phone left or right to dodge the falling obstacle.
- The prototype requires an accelerometer; unsupported devices show an explicit message instead of starting the game.

## Project configuration

- Language: Java
- UI: XML Views
- Minimum SDK: 24
- Target/Compile SDK: 35
- Application ID: `com.vanvinh2402.tiltescape`
