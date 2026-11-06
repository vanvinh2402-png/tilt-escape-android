package com.vanvinh2402.tiltescape.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class SharedPreferencesSessionStoreTest {
    private static final String PREFERENCES_NAME = "tilt_escape_session";
    private static final String ACTIVE_USER_ID = "active_user_id";
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        preferences().edit().clear().commit();
    }

    @Test
    public void absentSessionReturnsNull() {
        assertNull(new SharedPreferencesSessionStore(context).getActiveUserId());
    }

    @Test
    public void savedSessionSurvivesNewStoreInstanceAndCanBeCleared() {
        new SharedPreferencesSessionStore(context).saveActiveUserId(42L);

        SharedPreferencesSessionStore reopened = new SharedPreferencesSessionStore(context);
        assertEquals(Long.valueOf(42L), reopened.getActiveUserId());

        reopened.clear();
        assertNull(new SharedPreferencesSessionStore(context).getActiveUserId());
    }

    @Test
    public void nonPositiveIdsAreRejected() {
        assertIllegalArgument(() ->
                new SharedPreferencesSessionStore(context).saveActiveUserId(0L));
        assertIllegalArgument(() ->
                new SharedPreferencesSessionStore(context).saveActiveUserId(-1L));
    }

    @Test
    public void malformedOrStaleStoredValuesAreClearedAndTreatedAsAbsent() {
        preferences().edit().putString(ACTIVE_USER_ID, "not-a-number").commit();
        assertNull(new SharedPreferencesSessionStore(context).getActiveUserId());
        assertFalse(preferences().contains(ACTIVE_USER_ID));

        preferences().edit().putLong(ACTIVE_USER_ID, -5L).commit();
        assertNull(new SharedPreferencesSessionStore(context).getActiveUserId());
        assertFalse(preferences().contains(ACTIVE_USER_ID));
    }

    private SharedPreferences preferences() {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    private static void assertIllegalArgument(Runnable action) {
        try {
            action.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected public boundary.
        }
    }
}
