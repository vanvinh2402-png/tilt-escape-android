# Settings Sensitivity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn thành Frame 8 / R60–R61 bằng màn Settings chọn, lưu và áp dụng độ nhạy nghiêng ngay lập tức.

**Architecture:** `TiltSensitivity` giữ ba hệ số cố định; `TiltSettingsStore` quản lý SharedPreferences. `TiltSignalProcessor` nhận hệ số runtime. `SettingsUiController` quản lý overlay/menu, lưu lựa chọn rồi phát callback áp dụng vào processor; `MainActivity` tạo/wire tất cả thành phần.

**Tech Stack:** Android Java, XML Views, SharedPreferences, Robolectric, JUnit 4, Gradle 8.11.1.

**Spec:** `docs/superpowers/specs/2026-11-18-settings-sensitivity-design.md`

## Global Constraints

- Làm trực tiếp trên `main` theo lựa chọn đã có của người dùng.
- Ba mức duy nhất: LOW 0.75, MEDIUM 1.00, HIGH 1.25; MEDIUM là default an toàn.
- Không sửa schema SQLite, dữ liệu tài khoản, điểm, va chạm, difficulty hoặc sensor lifecycle.
- Settings chỉ mở tại `READY` khi Main Menu được phép hiển thị.
- Không có accelerometer thì ẩn action Settings.
- Dùng resource tiếng Việt, Neon Focus và ScrollView hiện có; không thêm thư viện.

## Review Focus

- Preference thiếu/sai kiểu/giá trị không xác định phải tự về MEDIUM, không crash; Task 1 test.
- Đổi mức phải thay đổi input theo đúng hướng, nhưng clamp vẫn giới hạn [-1, 1]; Task 1 test.
- Radio callback khi restore không được lưu/apply sai; Task 2 test.
- Settings không được che login, achievements, game đang Playing/Pause/Game Over; Task 2 test gate.
- Thiết bị không cảm biến phải không có action Settings chết; Task 2 MainActivity test.

---

### Task 1: Sensitivity model, persistence and processor

**Files:**
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/input/TiltSensitivity.java`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/input/TiltSettingsStore.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/input/TiltSignalProcessor.java`
- Create: `app/src/test/java/com/vanvinh2402/tiltescape/input/TiltSettingsStoreTest.java`
- Modify: `app/src/test/java/com/vanvinh2402/tiltescape/input/TiltSignalProcessorTest.java`

**Interfaces:**
- `TiltSensitivity` exposes `getMultiplier()` and `fromStorageValue(String)`.
- `TiltSettingsStore(Context)` exposes `getSensitivity()` and `saveSensitivity(TiltSensitivity)`.
- `TiltSignalProcessor.setSensitivityMultiplier(float)` applies finite positive values; invalid values reset to 1.0.

- [ ] **Step 1: Write failing tests**

Add store tests for default/corrupt persistence and save/reopen persistence. Add processor tests showing high response is greater than medium, low response is lower than medium, and output remains within [-1, 1].

- [ ] **Step 2: Run RED tests**

Run: `./gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.input.TiltSettingsStoreTest" --tests "com.vanvinh2402.tiltescape.input.TiltSignalProcessorTest"`

Expected: FAIL because no model/store/processor sensitivity API exists.

- [ ] **Step 3: Implement minimal model and input behavior**

Implement the enum/store. Keep filtering intact; multiply `filteredTiltG` by the selected factor when deriving horizontal input. Recompute input after a runtime sensitivity change.

- [ ] **Step 4: Run GREEN tests**

Run the same targeted command. Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/vanvinh2402/tiltescape/input app/src/test/java/com/vanvinh2402/tiltescape/input
git commit -m "feat: persist tilt sensitivity"
```

### Task 2: Settings overlay and application wiring

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/java/com/vanvinh2402/tiltescape/ui/SettingsUiController.java`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/MainActivity.java`
- Modify: `app/src/test/java/com/vanvinh2402/tiltescape/MainActivityTest.java`
- Create: `app/src/test/java/com/vanvinh2402/tiltescape/ui/SettingsUiControllerTest.java`

**Interfaces:**
- `SettingsUiController(Activity, GameEngine, BooleanSupplier, TiltSettingsStore, Consumer<TiltSensitivity>)` exposes `bind()`, `open()`, `close()`, `isVisible()`, `handleBack()`.
- MainActivity passes `processor::setSensitivityMultiplier` after mapping from enum multiplier.

- [ ] **Step 1: Write failing tests**

Add layout test that panel/action IDs exist, panel is scrollable and controls descend from it. Add controller tests for READY/menu gate, selection persistence + callback, reopening selected radio, Back/close restore. Extend unavailable-sensor Activity test to require Settings hidden.

- [ ] **Step 2: Run RED tests**

Run: `./gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.MainActivityTest" --tests "com.vanvinh2402.tiltescape.ui.SettingsUiControllerTest"`

Expected: FAIL because Settings resource IDs and controller do not exist.

- [ ] **Step 3: Implement overlay and wiring**

Add Main Menu action and hidden scrollable card containing radio controls and Back. Implement controller guards, restore selection before observing changes, save/apply a user selection, and close behavior. Create store/processor before availability check; hide Settings when unavailable; bind controller and route Back ahead of help/achievements/auth.

- [ ] **Step 4: Run GREEN regressions**

Run: `./gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.MainActivityTest" --tests "com.vanvinh2402.tiltescape.ui.SettingsUiControllerTest" --tests "com.vanvinh2402.tiltescape.ui.HowToPlayUiControllerTest" --tests "com.vanvinh2402.tiltescape.ui.GameplayUiControllerTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main app/src/test
git commit -m "feat: add tilt sensitivity settings"
```

### Task 3: Verification and handover

**Files:**
- Modify: `docs/handover.md`
- Create: `docs/test-results-settings-sensitivity.md`

- [ ] **Step 1: Run complete verification**

Run: `./gradlew.bat --no-daemon clean testDebugUnitTest assembleDebug lintDebug assembleRelease`.

Expected: BUILD SUCCESSFUL, zero test failures/errors, lint without errors and both APKs exist.

- [ ] **Step 2: Pixel 7 smoke test**

If ADB device is available, install signed release, select Low/High, relaunch and verify selected state persists plus tilt feels less/more responsive. Otherwise record this as pending.

- [ ] **Step 3: Document and commit**

Update Frame 8 / R60–R61 status and exact verification evidence.

```powershell
git add docs/handover.md docs/test-results-settings-sensitivity.md
git commit -m "docs: verify tilt sensitivity settings"
```
