# Android Base Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a minimal Android Java project that syncs, tests, builds an APK, and opens to a TILT ESCAPE placeholder screen.

**Architecture:** Use a single `app` module with Android XML Views and Java. Keep the initial activity intentionally small; create empty feature packages only when real code is added later, so the base remains YAGNI-compliant.

**Tech Stack:** Android Gradle Plugin, Gradle Wrapper, Java 17 toolchain, Android SDK, AndroidX AppCompat, JUnit 4.

**Spec:** Approved conversation design: package `com.vanvinh2402.tiltescape`, Java, XML Views, minSdk 24, portrait orientation, one `app` module.

## Global Constraints

- Repository: `vanvinh2402-png/tilt-escape-android`, private.
- Application ID and namespace: `com.vanvinh2402.tiltescape`.
- Source language: Java.
- UI system: XML Views.
- Minimum Android API: 24.
- Screen orientation: portrait.
- No gameplay, authentication, database, sensor, or third-party feature libraries in this base.

## Review Focus

- A clean clone must build without a globally installed Gradle executable by using `gradlew`.
- The selected compile/target SDK must exist on the development machine.
- The launcher activity must be exported correctly for modern Android manifests.
- The app name must come from string resources and render as `TILT ESCAPE`.
- Generated build output and local SDK paths must remain untracked.

---

### Task 1: Scaffold the buildable Android project

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/MainActivity.java`
- Create: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/values/strings.xml`, `colors.xml`, `themes.xml`
- Create: `app/src/test/java/com/vanvinh2402/tiltescape/ExampleUnitTest.java`
- Modify: `.gitignore`
- Modify: `README.md`

**Interfaces:**
- Consumes: Installed Android SDK and Android Studio JDK.
- Produces: A launcher activity named `MainActivity` and the standard Gradle commands `testDebugUnitTest` and `assembleDebug`.

- [ ] **Step 1: Select compatible installed SDK and build-tool versions**

Inspect the local SDK and Android Studio version, then pin compatible Gradle and Android Gradle Plugin versions.

- [ ] **Step 2: Create configuration and generated scaffold files**

Create a single-module Android Java project with namespace/application ID `com.vanvinh2402.tiltescape`, minSdk 24, portrait launcher activity, XML placeholder screen, and JUnit dependency.

- [ ] **Step 3: Generate or obtain the Gradle Wrapper from the installed toolchain**

Ensure `gradlew.bat --version` runs without requiring a global Gradle installation.

- [ ] **Step 4: Run unit tests**

Run: `gradlew.bat testDebugUnitTest`

Expected: `BUILD SUCCESSFUL` and the sample unit test passes.

- [ ] **Step 5: Build the debug APK**

Run: `gradlew.bat assembleDebug`

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists and is non-empty.

- [ ] **Step 6: Review tracked files and commit**

Run `git status --short` and ensure no `local.properties`, `.gradle`, `.idea`, or `build` output is tracked. Commit as `chore: initialize Android Java project` and push `main`.

