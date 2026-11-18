package com.vanvinh2402.tiltescape.input;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class TiltSettingsStoreTest {
    @Test
    public void defaultsToMediumAndPersistsSelectionAcrossInstances() {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("tilt_escape_settings", Context.MODE_PRIVATE).edit().clear().commit();
        TiltSettingsStore first = new TiltSettingsStore(context);
        assertEquals(TiltSensitivity.MEDIUM, first.getSensitivity());

        first.saveSensitivity(TiltSensitivity.HIGH);
        assertEquals(TiltSensitivity.HIGH, new TiltSettingsStore(context).getSensitivity());
    }

    @Test
    public void invalidStoredValueFallsBackToMedium() {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("tilt_escape_settings", Context.MODE_PRIVATE)
                .edit().putString("tilt_sensitivity", "UNKNOWN").commit();

        assertEquals(TiltSensitivity.MEDIUM, new TiltSettingsStore(context).getSensitivity());
    }
}
