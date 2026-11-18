package com.vanvinh2402.tiltescape.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.Button;

import com.vanvinh2402.tiltescape.MainActivity;
import com.vanvinh2402.tiltescape.game.GameEngine;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public final class HowToPlayUiControllerTest {
    @Test
    public void opensOnlyFromAllowedReadyMenuAndBackRestoresIt() throws Exception {
        try (ActivityController<MainActivity> activityController =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = activityController.get();
            GameEngine engine = new GameEngine();
            HowToPlayUiController controller = new HowToPlayUiController(
                    activity, engine, () -> true, () -> { });
            controller.bind();

            activity.findViewById(id(activity, "ready_panel")).setVisibility(View.VISIBLE);
            activity.<Button>findViewById(id(activity, "how_to_play_button")).performClick();
            assertEquals(View.VISIBLE, activity.findViewById(id(activity, "how_to_play_panel")).getVisibility());
            assertEquals(View.GONE, activity.findViewById(id(activity, "ready_panel")).getVisibility());
            assertTrue(controller.isVisible());

            assertTrue(controller.handleBack());
            assertEquals(View.GONE, activity.findViewById(id(activity, "how_to_play_panel")).getVisibility());
            assertEquals(View.VISIBLE, activity.findViewById(id(activity, "ready_panel")).getVisibility());
            assertFalse(controller.handleBack());
        }
    }

    @Test
    public void startInvokesCallbackOnlyWhenMenuIsAllowed() throws Exception {
        try (ActivityController<MainActivity> activityController =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = activityController.get();
            AtomicInteger starts = new AtomicInteger();
            HowToPlayUiController allowed = new HowToPlayUiController(
                    activity, new GameEngine(), () -> true, starts::incrementAndGet);
            allowed.bind();
            activity.findViewById(id(activity, "ready_panel")).setVisibility(View.VISIBLE);
            activity.<Button>findViewById(id(activity, "how_to_play_button")).performClick();
            activity.<Button>findViewById(id(activity, "how_to_play_start_button")).performClick();
            assertEquals(1, starts.get());
            assertEquals(View.GONE, activity.findViewById(id(activity, "how_to_play_panel")).getVisibility());

            HowToPlayUiController blocked = new HowToPlayUiController(
                    activity, new GameEngine(), () -> false, starts::incrementAndGet);
            blocked.bind();
            blocked.open();
            assertEquals(View.GONE, activity.findViewById(id(activity, "how_to_play_panel")).getVisibility());
            assertEquals(1, starts.get());
        }
    }

    private static int id(android.app.Activity activity, String name) {
        int id = activity.getResources().getIdentifier(name, "id", activity.getPackageName());
        assertTrue(name + " must exist", id != 0);
        return id;
    }
}
