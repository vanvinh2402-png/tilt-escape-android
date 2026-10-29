package com.vanvinh2402.tiltescape;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.View;
import android.widget.TextView;

import com.vanvinh2402.tiltescape.ui.GameView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowSensor;
import org.robolectric.shadows.ShadowSensorManager;

import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public final class MainActivityTest {
    @Test
    public void missingAccelerometerShowsUnavailableMessageWithoutGameView() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            TextView status = activity.findViewById(R.id.status_text);

            assertEquals(activity.getString(R.string.sensor_unavailable), status.getText().toString());
            assertEquals(View.VISIBLE, status.getVisibility());
            assertNull(activity.findViewById(R.id.game_view));
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
}
