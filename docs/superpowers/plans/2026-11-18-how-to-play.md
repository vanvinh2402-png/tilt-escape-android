# How to Play Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn thành Frame 4 / R59 với một màn Hướng dẫn chơi riêng, mở từ menu, có thể quay lại hoặc bắt đầu ván mới.

**Architecture:** Bổ sung `HowToPlayUiController` điều phối overlay hướng dẫn. Controller này nhận callback `Runnable` để dùng thao tác start hiện có thay vì sao chép logic gameplay. `MainActivity` quản lý Back theo thứ tự hướng dẫn, thành tích, xác thực. XML giữ toàn bộ ID hiện có và thêm các ID mới.

**Tech Stack:** Android Java, XML layout/resources, Robolectric, JUnit 4, Gradle 8.11.1.

**Spec:** `docs/superpowers/specs/2026-11-18-how-to-play-design.md`

## Global Constraints

- Làm trực tiếp trên `main` theo lựa chọn đã có của người dùng.
- Không thêm thư viện hoặc thay đổi cơ chế game, tài khoản, SQLite và tính điểm.
- Chỉ cho mở hướng dẫn khi engine là `READY` và menu được phép hiện.
- Card hướng dẫn phải cuộn được; action cuối luôn là descendant của `ScrollView`.
- Dùng string resources tiếng Việt và style Neon Focus hiện có.

## Review Focus

- Người chưa đăng nhập không được mở một màn che form xác thực; test controller phải kiểm tra gate menu.
- Back, nút Quay lại và Bắt đầu phải không làm game chuyển state sai hoặc để overlay còn hiển thị.
- Màn hình thấp/phông chữ lớn: action cuối nằm trong ScrollView; test hierarchy và kiểm tra thủ công Pixel 7.
- Luồng Thành tích/đăng xuất/Pause/Game Over giữ nguyên vì các ID và controller hiện hữu không đổi.

---

### Task 1: Help overlay and controller

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/ui/HowToPlayUiController.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/ui/GameplayUiController.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/MainActivity.java`
- Modify: `app/src/test/java/com/vanvinh2402/tiltescape/MainActivityTest.java`
- Create: `app/src/test/java/com/vanvinh2402/tiltescape/ui/HowToPlayUiControllerTest.java`

**Interfaces:**
- `HowToPlayUiController(Activity, GameEngine, BooleanSupplier, Runnable)` exposes `bind()`, `open()`, `close()`, `isVisible()` and `handleBack()`.
- `GameplayUiController` exposes `startFromExternalAction()` which has exactly the Start-button guard and behavior.

- [ ] **Step 1: Write failing tests**

Add an activity test proving the help panel, its three action IDs, and a scrollable final action exist. Add controller tests proving `open` hides Ready, `close` restores it, `handleBack` consumes only when visible, and `start` invokes its callback only when allowed.

- [ ] **Step 2: Run RED tests**

Run:
```powershell
.\gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.MainActivityTest.howToPlayPanelKeepsActionsReachable" --tests "com.vanvinh2402.tiltescape.ui.HowToPlayUiControllerTest"
```

Expected: FAIL because the panel, IDs and controller do not exist.

- [ ] **Step 3: Implement minimal help flow**

Add the string resources and a hidden scrollable `how_to_play_panel` with title, three explanations, `how_to_play_start_button` and `how_to_play_back_button`; add `how_to_play_button` to Ready. Implement the controller with READY/menu guards. Extract the existing private Gameplay start behavior into public `startFromExternalAction()` and use it for both start actions. Wire the controller and Back dispatch in `MainActivity`.

- [ ] **Step 4: Run GREEN tests and relevant regressions**

Run:
```powershell
.\gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.MainActivityTest" --tests "com.vanvinh2402.tiltescape.ui.HowToPlayUiControllerTest" --tests "com.vanvinh2402.tiltescape.ui.GameplayUiControllerTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main docs/superpowers/specs/2026-11-18-how-to-play-design.md docs/superpowers/plans/2026-11-18-how-to-play.md app/src/test
git commit -m "feat: add how to play screen"
```

### Task 2: Verification and handoff status

**Files:**
- Modify: `docs/handover.md`
- Create: `docs/test-results-how-to-play.md`

**Interfaces:**
- Consumes the completed help screen and Gradle tasks from Task 1.
- Produces accurate handover status for Frame 4 / R59 and current verification evidence.

- [ ] **Step 1: Run full project verification**

Run:
```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\CongQuyManh\AppData\Local\Android\Sdk'
.\gradlew.bat --no-daemon clean testDebugUnitTest assembleDebug lintDebug assembleRelease
```

Expected: `BUILD SUCCESSFUL`, zero test failures/errors, lint has no errors, and debug/release APK outputs exist.

- [ ] **Step 2: Smoke test Pixel 7 release APK**

Install the signed release with `adb install -r`, launch it, register/log in a local test account if necessary, then open Hướng dẫn chơi, use Quay lại, open it again and use Bắt đầu chơi. Record observed outcomes only.

- [ ] **Step 3: Document and commit**

Update handover to mark Frame 4 / R59 done and state what remains. Record command results, test count, APK path and manual result in the test report.

```powershell
git add docs/handover.md docs/test-results-how-to-play.md
git commit -m "docs: verify how to play screen"
```
