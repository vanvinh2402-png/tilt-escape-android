package com.vanvinh2402.tiltescape;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

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

    private static void addAccelerometer() {
        SensorManager sensorManager = (SensorManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.SENSOR_SERVICE);
        shadowOf(sensorManager).addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER));
    }
}
