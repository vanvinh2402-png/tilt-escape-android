# Neon Focus UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Nâng cấp toàn bộ giao diện Tilt Escape sang phong cách Neon Focus dễ đọc, nhất quán và phù hợp Pixel 7 mà không thay đổi gameplay hoặc dữ liệu.

**Architecture:** Giữ nguyên Activity, controller và toàn bộ view ID đang được Java sử dụng. Tạo design system bằng Android XML resources (colors, dimensions, styles, selectors và shape drawables), sau đó áp dụng lần lượt cho màn xác thực và các trạng thái game; chỉ chỉnh `GameView` khi cần đồng bộ màu canvas.

**Tech Stack:** Android Java, AppCompat, XML layouts/drawables, Robolectric, JUnit 4, Gradle 8.11.1.

**Spec:** `docs/superpowers/specs/2026-11-06-neon-focus-ui-design.md`

## Global Constraints

- Không thêm thư viện UI hoặc tài nguyên ảnh bên ngoài.
- Giữ nguyên tất cả view ID mà controller và test hiện tại sử dụng.
- Không thay đổi logic tài khoản, SQLite, gameplay, điều khiển nghiêng hoặc tính điểm.
- Màu bắt buộc: background `#090B16`, surface `#131831`, elevated `#191F3D`, primary `#6D5DFC` → `#3D9BFF`, accent `#2AD9BA`, text `#F8FAFC`, secondary `#AEB8DC`, error `#FF667A`.
- Touch target tương tác tối thiểu 48dp; input và nút chuẩn cao 52dp.
- Form xác thực phải cuộn được khi bàn phím mở và không phụ thuộc màu mặc định Android.

## Review Focus

- Pixel 7 có thanh hệ thống/insets: HUD và form không được chạm status/navigation bar; kiểm thử `MainActivityTest.neonLayoutKeepsHudBeforeGameContainer` trong Task 2.
- Bàn phím ảo và màn hình thấp: form Đăng ký vẫn cuộn tới nút cuối; kiểm thử `MainActivityTest.registrationRemainsInsideScrollablePanel` trong Task 2.
- Chế độ Day/Night hoặc theme hệ thống: text/hint/input không được trở thành chữ tối trên nền tối; kiểm thử `MainActivityTest.authenticationFieldsUseExplicitNeonColors` trong Task 2.
- Trạng thái disabled/loading/thiếu cảm biến: nút vẫn đọc được và hành vi cũ không đổi; kiểm thử `MainActivityTest.neonActionsKeepAccessibleTouchTargets` và test gameplay gate hiện có trong Task 3.
- Danh sách thành tích dài/không có dữ liệu/lỗi tải: container phải cuộn và các trạng thái vẫn rõ; kiểm thử `MainActivityTest.achievementsUseReadableNeonSurface` cùng các test controller hiện có trong Task 4.

---

### Task 1: Neon resource system

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/dimens.xml`
- Create: `app/src/main/res/values/styles.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/bg_app_gradient.xml`
- Create: `app/src/main/res/drawable/bg_neon_card.xml`
- Create: `app/src/main/res/drawable/bg_neon_input.xml`
- Create: `app/src/main/res/drawable/bg_button_primary.xml`
- Create: `app/src/main/res/drawable/bg_button_secondary.xml`
- Create: `app/src/main/res/drawable/bg_button_ghost.xml`
- Create: `app/src/main/res/drawable/bg_result_item.xml`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/NeonUiResourcesTest.java`

**Interfaces:**
- Consumes: Android resource APIs and existing `Theme.TiltEscape`.
- Produces: colors `background`, `surface`, `surface_elevated`, `primary`, `primary_end`, `accent`, `on_background`, `text_secondary`, `score_accent`; dimensions `touch_target_min`, `control_height`, `card_radius`; styles `Widget.TiltEscape.Input`, `Widget.TiltEscape.Button.Primary`, `Widget.TiltEscape.Button.Secondary`, `Widget.TiltEscape.Button.Ghost`, `TextAppearance.TiltEscape.*`.

- [ ] **Step 1: Write failing resource tests**

Create `NeonUiResourcesTest` with `neonPaletteMatchesApprovedDesign`, `neonControlsMeetMinimumTouchTarget`, and `neonDrawablesAreLoadable`. Assert the exact ARGB values above, 48dp/52dp dimensions, and non-null drawable loading for every new background.

- [ ] **Step 2: Run the tests and verify RED**

Run: `./gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.NeonUiResourcesTest"`

Expected: FAIL because the new resources do not exist.

- [ ] **Step 3: Implement the resource system**

Add the exact palette/dimensions, XML shape/selector drawables, reusable widget/text styles, and update `Theme.TiltEscape` system bars/window background to `background`. Button selectors must include disabled colors; input selector must include focused and default strokes.

- [ ] **Step 4: Run the resource tests and verify GREEN**

Run the same targeted test command.

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/res app/src/test/java/com/vanvinh2402/tiltescape/NeonUiResourcesTest.java
git commit -m "feat: add Neon Focus design system"
```

### Task 2: Authentication screens

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/MainActivityTest.java`

**Interfaces:**
- Consumes: Task 1 widget/text styles and drawables; existing IDs `login_panel`, `register_panel`, all auth fields/buttons/errors/loading.
- Produces: centered scrollable auth card layout with new non-controller IDs `login_card` and `register_card`; all existing controller-facing IDs remain unchanged.

- [ ] **Step 1: Add failing authentication layout tests**

Add:
- `authenticationFieldsUseExplicitNeonColors`: assert current text `on_background`, hint `text_secondary`, non-null Neon input background.
- `registrationRemainsInsideScrollablePanel`: assert `register_card` is a descendant of `register_panel` and the bottom action remains inside the ScrollView content.
- `neonLayoutKeepsHudBeforeGameContainer`: assert existing HUD/game container order after the layout rewrite.

- [ ] **Step 2: Run the three tests and verify RED**

Run: `./gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.MainActivityTest.authenticationFieldsUseExplicitNeonColors" --tests "com.vanvinh2402.tiltescape.MainActivityTest.registrationRemainsInsideScrollablePanel" --tests "com.vanvinh2402.tiltescape.MainActivityTest.neonLayoutKeepsHudBeforeGameContainer"`

Expected: FAIL because card IDs/styles are absent and fields use theme defaults.

- [ ] **Step 3: Restyle Login and Register**

In `activity_main.xml`, keep each full-screen ScrollView and existing input/action IDs; introduce a centered card container, brand eyebrow, title, description, consistent 12dp field spacing, primary/secondary styles, explicit loading/error styles, and 24–28dp responsive padding. Update only user-facing copy required by the approved mockup.

- [ ] **Step 4: Run auth/controller and Activity tests**

Run: `./gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.ui.AuthUiControllerTest" --tests "com.vanvinh2402.tiltescape.MainActivityTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/res/layout/activity_main.xml app/src/main/res/values/strings.xml app/src/test/java/com/vanvinh2402/tiltescape/MainActivityTest.java
git commit -m "feat: redesign local authentication screens"
```

### Task 3: HUD and game-state cards

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/java/com/vanvinh2402/tiltescape/ui/GameView.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/MainActivityTest.java`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/ui/GameViewTest.java`

**Interfaces:**
- Consumes: Task 1 resource system and Task 2 layout hierarchy.
- Produces: Neon HUD, ready/pause/game-over cards and canvas palette; controller-facing IDs and visibility behavior unchanged.

- [ ] **Step 1: Add failing state-card tests**

Add `neonActionsKeepAccessibleTouchTargets` asserting Start, Pause, Resume, Replay and menu actions have minimum height at least 48dp and explicit readable text colors. Extend `GameViewTest` with `usesApprovedNeonPalette` through package-visible color constants `BACKGROUND_COLOR`, `PLAYER_COLOR`, and `OBSTACLE_COLOR`.

- [ ] **Step 2: Run targeted tests and verify RED**

Run: `./gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.MainActivityTest.neonActionsKeepAccessibleTouchTargets" --tests "com.vanvinh2402.tiltescape.ui.GameViewTest.usesApprovedNeonPalette"`

Expected: FAIL because default buttons/current private hard-coded palette do not expose the approved values.

- [ ] **Step 3: Apply Neon state styling**

Restyle HUD and the ready, pause and game-over panels with the Task 1 card/button/text styles. In `GameView`, define package-visible static color constants and initialize paints from the approved background/accent/error palette without changing geometry, collision or drawing order.

- [ ] **Step 4: Run gameplay UI and view tests**

Run: `./gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.MainActivityTest" --tests "com.vanvinh2402.tiltescape.ui.GameViewTest" --tests "com.vanvinh2402.tiltescape.GameSessionControllerTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/res/layout/activity_main.xml app/src/main/java/com/vanvinh2402/tiltescape/ui/GameView.java app/src/test/java/com/vanvinh2402/tiltescape
git commit -m "feat: style game states with Neon Focus"
```

### Task 4: Achievements, final verification and handoff

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/layout/item_game_result.xml`
- Modify: `README.md`
- Create: `docs/test-results-neon-focus-ui.md`
- Test: `app/src/test/java/com/vanvinh2402/tiltescape/MainActivityTest.java`

**Interfaces:**
- Consumes: Task 1 design resources and existing achievement IDs/data binding.
- Produces: Neon achievements screen/result cards and final verification evidence; no new Java API.

- [ ] **Step 1: Add the failing achievements surface test**

Add `achievementsUseReadableNeonSurface`: assert the panel uses the app background, high score uses accent, loading/empty states use readable text, error uses `score_accent`, results container remains inside the achievements ScrollView, and Back meets 48dp.

- [ ] **Step 2: Run the test and verify RED**

Run: `./gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.MainActivityTest.achievementsUseReadableNeonSurface"`

Expected: FAIL because current rows and controls lack the Neon backgrounds/styles.

- [ ] **Step 3: Restyle achievements and result rows**

Apply the approved palette, card rows, spacing and button styles to `achievements_panel` and `item_game_result.xml`. Preserve all IDs and the current empty/loading/error visibility behavior.

- [ ] **Step 4: Run controller and Activity regression tests**

Run: `./gradlew.bat --no-daemon testDebugUnitTest --tests "com.vanvinh2402.tiltescape.ui.AchievementsUiControllerTest" --tests "com.vanvinh2402.tiltescape.MainActivityTest"`

Expected: PASS.

- [ ] **Step 5: Run clean project verification**

Run:
```powershell
./gradlew.bat --stop
./gradlew.bat --no-daemon --max-workers=1 clean testDebugUnitTest assembleDebug lintDebug
```

Expected: `BUILD SUCCESSFUL`, zero test failures/errors, lint successful, and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 6: Perform Pixel 7 visual smoke check**

Verify Login, Register, Ready, Pause, Game Over and Achievements in portrait; confirm no clipped content with keyboard open, explicit readable input hints, correct insets, and unchanged tilt/game-state behavior. If no ADB device is available, record these checks as pending rather than passed.

- [ ] **Step 7: Document and commit**

Record exact test count, APK size, lint result and manual smoke status in `docs/test-results-neon-focus-ui.md`; update README screenshots/style notes only if accurate.

```powershell
git add app/src/main/res/layout README.md docs/test-results-neon-focus-ui.md app/src/test/java/com/vanvinh2402/tiltescape/MainActivityTest.java
git commit -m "docs: verify Neon Focus interface"
```

