package com.vanvinh2402.tiltescape.ui;

import static org.junit.Assert.assertEquals;

import android.view.View;

import com.vanvinh2402.tiltescape.MainActivity;
import com.vanvinh2402.tiltescape.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public final class NewRecordUiControllerTest {
    @Test
    public void newHighScoreShowsOnlyForTheCurrentGameOver() {
        try (ActivityController<MainActivity> activityController =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = activityController.get();
            NewRecordUiController controller = new NewRecordUiController(activity);

            int current = controller.beginPendingResult();
            controller.onRecordSaved(current, true);
            assertEquals(View.VISIBLE, activity.findViewById(R.id.new_record_text).getVisibility());

            controller.reset();
            assertEquals(View.GONE, activity.findViewById(R.id.new_record_text).getVisibility());
            controller.onRecordSaved(current, true);
            assertEquals(View.GONE, activity.findViewById(R.id.new_record_text).getVisibility());
        }
    }
}
