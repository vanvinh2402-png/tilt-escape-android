package com.vanvinh2402.tiltescape;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ScrollView;

import com.vanvinh2402.tiltescape.ui.GameView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowSensor;
import org.robolectric.shadows.ShadowSensorManager;

@RunWith(RobolectricTestRunner.class)
public final class MainActivityTest {
    @Test
    public void appBackupIsDisabledForInstallationLocalAccounts() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            int flags = controller.get().getApplicationInfo().flags;
            assertEquals(0, flags & ApplicationInfo.FLAG_ALLOW_BACKUP);
        }
    }

    @Test
    public void achievementsViewsExistInTheMainLayout() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertNotNull(activity.findViewById(R.id.achievements_button));
            assertNotNull(activity.findViewById(R.id.achievements_panel));
            assertNotNull(activity.findViewById(R.id.achievements_high_score));
            assertNotNull(activity.findViewById(R.id.achievements_results_container));
            assertNotNull(activity.findViewById(R.id.achievements_back_button));
            assertNotNull(activity.findViewById(R.id.login_panel));
            assertNotNull(activity.findViewById(R.id.login_username));
            assertNotNull(activity.findViewById(R.id.login_password));
            assertNotNull(activity.findViewById(R.id.login_error));
            assertNotNull(activity.findViewById(R.id.login_loading));
            assertNotNull(activity.findViewById(R.id.register_panel));
            assertNotNull(activity.findViewById(R.id.register_confirmation));
            assertNotNull(activity.findViewById(R.id.current_username));
            assertNotNull(activity.findViewById(R.id.logout_button));
            assertNotNull(activity.findViewById(R.id.main_menu_sensor_status));
        }
    }

    @Test
    public void missingAccelerometerShowsUnavailableMessageAndSafeShell() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            TextView status = activity.findViewById(R.id.main_menu_sensor_status);
            ViewGroup root = activity.findViewById(R.id.game_root);
            View hud = activity.findViewById(R.id.hud_container);
            View gameContainer = activity.findViewById(R.id.game_container);

            assertEquals(activity.getString(R.string.sensor_unavailable), status.getText().toString());
            assertEquals(View.VISIBLE, status.getVisibility());
            assertNull(activity.findViewById(R.id.game_view));
            assertEquals(View.VISIBLE, activity.findViewById(R.id.login_panel).getVisibility());
            assertEquals(View.GONE, activity.findViewById(R.id.ready_panel).getVisibility());
            assertTrue(!activity.findViewById(R.id.start_button).isEnabled());
            assertSame(root, hud.getParent());
            assertSame(root, gameContainer.getParent());
            assertTrue(root.indexOfChild(hud) < root.indexOfChild(gameContainer));

        }
    }

    @Test
    public void availableAccelerometerCreatesPlayableViewAcrossLifecycle() {
        SensorManager sensorManager = (SensorManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.SENSOR_SERVICE);
        ShadowSensorManager shadowSensorManager = shadowOf(sensorManager);
        shadowSensorManager.addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER));

        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).create().start().resume()) {
            MainActivity activity = controller.get();
            GameView gameView = activity.findViewById(R.id.game_view);
            assertNotNull(gameView);
            controller.pause().stop().restart().start().resume().pause().stop().destroy();
        }
    }

    @Test
    public void scoreHudUsesLocalizedFormat() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            activity.updateScore(120);
            TextView score = activity.findViewById(R.id.score_text);
            assertEquals(activity.getString(R.string.score_format, 120), score.getText().toString());
        }
    }

    @Test
    public void unauthenticatedUserCannotStartGame() {
        addAccelerometer();
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).create().start().resume()) {
            MainActivity activity = controller.get();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.login_panel).getVisibility());
            assertEquals(View.GONE, activity.findViewById(R.id.ready_panel).getVisibility());
            activity.<Button>findViewById(R.id.start_button).performClick();
            assertEquals(View.GONE, activity.findViewById(R.id.pause_button).getVisibility());
            assertEquals(View.GONE, activity.findViewById(R.id.ready_panel).getVisibility());
        }
    }

    @Test
    public void loginRemainsVisibleAcrossActivityLifecycle() {
        addAccelerometer();
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).create().start().resume()) {
            MainActivity activity = controller.get();
            controller.pause().stop().restart().start().resume();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.login_panel).getVisibility());
            assertEquals(View.GONE, activity.findViewById(R.id.ready_panel).getVisibility());
        }
    }

    @Test
    public void authenticationFieldsUseExplicitNeonColors() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            EditText[] fields = {
                    activity.findViewById(R.id.login_username),
                    activity.findViewById(R.id.login_password),
                    activity.findViewById(R.id.register_username),
                    activity.findViewById(R.id.register_password),
                    activity.findViewById(R.id.register_confirmation)
            };

            for (EditText field : fields) {
                assertEquals(activity.getColor(R.color.on_background),
                        field.getCurrentTextColor());
                assertEquals(activity.getColor(R.color.text_secondary),
                        field.getHintTextColors().getDefaultColor());
                assertEquals(R.drawable.bg_neon_input,
                        shadowOf(field.getBackground()).getCreatedFromResId());
            }
        }
    }

    @Test
    public void registrationRemainsInsideScrollablePanel() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            int cardId = activity.getResources().getIdentifier(
                    "register_card", "id", activity.getPackageName());
            assertTrue("register_card must exist", cardId != 0);

            View panel = activity.findViewById(R.id.register_panel);
            View card = activity.findViewById(cardId);
            View lastAction = activity.findViewById(R.id.register_back_button);
            assertTrue(isDescendantOf(card, panel));
            assertTrue(isDescendantOf(lastAction, panel));
        }
    }

    @Test
    public void neonLayoutKeepsHudBeforeGameContainer() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            ViewGroup root = activity.findViewById(R.id.game_root);
            View hud = activity.findViewById(R.id.hud_container);
            View gameContainer = activity.findViewById(R.id.game_container);

            assertSame(root, hud.getParent());
            assertSame(root, gameContainer.getParent());
            assertTrue(root.indexOfChild(hud) < root.indexOfChild(gameContainer));
            assertEquals(R.drawable.bg_app_gradient,
                    shadowOf(root.getBackground()).getCreatedFromResId());
        }
    }

    @Test
    public void neonActionsKeepAccessibleTouchTargets() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            int minimum = activity.getResources().getDimensionPixelSize(
                    R.dimen.touch_target_min);
            Button[] actions = {
                    activity.findViewById(R.id.start_button),
                    activity.findViewById(R.id.pause_button),
                    activity.findViewById(R.id.resume_button),
                    activity.findViewById(R.id.restart_button),
                    activity.findViewById(R.id.pause_main_menu_button),
                    activity.findViewById(R.id.play_again_button),
                    activity.findViewById(R.id.game_over_main_menu_button)
            };

            for (Button action : actions) {
                assertTrue(action.getMinHeight() >= minimum);
                assertEquals(activity.getColor(R.color.on_background),
                        action.getCurrentTextColor());
            }
        }
    }

    @Test
    public void achievementsUseReadableNeonSurface() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            View panel = activity.findViewById(R.id.achievements_panel);
            TextView highScore = activity.findViewById(R.id.achievements_high_score);
            TextView loading = activity.findViewById(R.id.achievements_loading);
            TextView empty = activity.findViewById(R.id.achievements_empty);
            TextView error = activity.findViewById(R.id.achievements_error);
            View results = activity.findViewById(R.id.achievements_results_container);
            Button back = activity.findViewById(R.id.achievements_back_button);
            View resultRow = activity.getLayoutInflater().inflate(
                    R.layout.item_game_result, null, false);

            assertEquals(R.drawable.bg_app_gradient,
                    shadowOf(panel.getBackground()).getCreatedFromResId());
            assertEquals(activity.getColor(R.color.accent),
                    highScore.getCurrentTextColor());
            assertEquals(activity.getColor(R.color.on_background),
                    loading.getCurrentTextColor());
            assertEquals(activity.getColor(R.color.on_background),
                    empty.getCurrentTextColor());
            assertEquals(activity.getColor(R.color.score_accent),
                    error.getCurrentTextColor());
            assertTrue(isDescendantOf(results, panel));
            assertTrue(back.getMinHeight() >= activity.getResources()
                    .getDimensionPixelSize(R.dimen.touch_target_min));
            assertEquals(R.drawable.bg_button_secondary,
                    shadowOf(back.getBackground()).getCreatedFromResId());
            assertEquals(R.drawable.bg_result_item,
                    shadowOf(resultRow.getBackground()).getCreatedFromResId());
        }
    }

    @Test
    public void howToPlayPanelKeepsActionsReachable() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            int panelId = activity.getResources().getIdentifier(
                    "how_to_play_panel", "id", activity.getPackageName());
            int menuButtonId = activity.getResources().getIdentifier(
                    "how_to_play_button", "id", activity.getPackageName());
            int startButtonId = activity.getResources().getIdentifier(
                    "how_to_play_start_button", "id", activity.getPackageName());
            int backButtonId = activity.getResources().getIdentifier(
                    "how_to_play_back_button", "id", activity.getPackageName());

            assertTrue("how_to_play_panel must exist", panelId != 0);
            assertTrue("how_to_play_button must exist", menuButtonId != 0);
            assertTrue("how_to_play_start_button must exist", startButtonId != 0);
            assertTrue("how_to_play_back_button must exist", backButtonId != 0);

            View panel = activity.findViewById(panelId);
            View menuButton = activity.findViewById(menuButtonId);
            View startButton = activity.findViewById(startButtonId);
            View backButton = activity.findViewById(backButtonId);
            assertNotNull(panel);
            assertNotNull(menuButton);
            assertNotNull(startButton);
            assertNotNull(backButton);
            assertTrue(panel instanceof ScrollView);
            assertTrue(isDescendantOf(startButton, panel));
            assertTrue(isDescendantOf(backButton, panel));
        }
    }

    private static boolean isDescendantOf(View child, View ancestor) {
        ViewParent parent = child.getParent();
        while (parent != null) {
            if (parent == ancestor) return true;
            parent = parent.getParent();
        }
        return false;
    }

    private static void addAccelerometer() {
        SensorManager sensorManager = (SensorManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.SENSOR_SERVICE);
        shadowOf(sensorManager).addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER));
    }
}
