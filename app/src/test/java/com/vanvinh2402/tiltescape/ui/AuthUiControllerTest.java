package com.vanvinh2402.tiltescape.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.vanvinh2402.tiltescape.MainActivity;
import com.vanvinh2402.tiltescape.R;
import com.vanvinh2402.tiltescape.auth.ActiveSession;
import com.vanvinh2402.tiltescape.auth.AuthOutcome;
import com.vanvinh2402.tiltescape.auth.AuthService;
import com.vanvinh2402.tiltescape.auth.PasswordPolicy;
import com.vanvinh2402.tiltescape.auth.SessionStore;
import com.vanvinh2402.tiltescape.auth.UsernamePolicy;
import com.vanvinh2402.tiltescape.auth.UsernameValidation;
import com.vanvinh2402.tiltescape.auth.AuthenticatedUser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public final class AuthUiControllerTest {
    @Test
    public void bindShowsLoginAndNavigationHandlesBackFromRegister() {
        try (ActivityController<MainActivity> owner =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            Harness harness = new Harness(owner.get());
            harness.controller.bind();

            assertEquals(View.VISIBLE, owner.get().findViewById(R.id.login_panel).getVisibility());
            assertEquals(View.GONE, owner.get().findViewById(R.id.ready_panel).getVisibility());
            owner.get().<Button>findViewById(R.id.show_register_button).performClick();
            assertEquals(View.VISIBLE,
                    owner.get().findViewById(R.id.register_panel).getVisibility());
            assertTrue(harness.controller.handleBack());
            assertEquals(View.VISIBLE, owner.get().findViewById(R.id.login_panel).getVisibility());
        }
    }

    @Test
    public void registrationValidatesAndIgnoresDoubleSubmitWhilePending() {
        try (ActivityController<MainActivity> owner =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            Harness harness = new Harness(owner.get());
            harness.controller.bind();
            owner.get().<Button>findViewById(R.id.show_register_button).performClick();
            owner.get().<Button>findViewById(R.id.register_button).performClick();
            assertEquals(0, harness.service.registerCalls);

            setText(owner.get(), R.id.register_username, "Player");
            setText(owner.get(), R.id.register_password, "secret1");
            setText(owner.get(), R.id.register_confirmation, "secret1");
            Button register = owner.get().findViewById(R.id.register_button);
            register.performClick();
            register.performClick();
            assertEquals(1, harness.service.registerCalls);

            harness.service.complete(AuthOutcome.of(AuthOutcome.Status.REGISTERED));
            assertEquals(View.VISIBLE, owner.get().findViewById(R.id.login_panel).getVisibility());
            assertEquals("Player", text(owner.get(), R.id.login_username));
        }
    }

    @Test
    public void loginPersistsAuthenticatedUserAndLogoutClearsBothSessions() {
        try (ActivityController<MainActivity> owner =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            Harness harness = new Harness(owner.get());
            harness.controller.bind();
            setText(owner.get(), R.id.login_username, "Player");
            setText(owner.get(), R.id.login_password, "secret1");
            owner.get().<Button>findViewById(R.id.login_button).performClick();
            harness.service.complete(AuthOutcome.authenticated(
                    new AuthenticatedUser(7L, "Player")));

            assertTrue(harness.controller.isMainMenuAllowed());
            assertEquals(Long.valueOf(7L), harness.store.id);
            assertEquals(View.VISIBLE, owner.get().findViewById(R.id.ready_panel).getVisibility());

            owner.get().<Button>findViewById(R.id.logout_button).performClick();
            assertFalse(harness.controller.isMainMenuAllowed());
            assertEquals(null, harness.store.id);
            assertEquals(View.VISIBLE, owner.get().findViewById(R.id.login_panel).getVisibility());
        }
    }

    @Test
    public void staleRestoredIdIsClearedAndReturnsToLogin() {
        try (ActivityController<MainActivity> owner =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            Harness harness = new Harness(owner.get());
            harness.store.id = 99L;
            harness.controller.bind();
            harness.controller.restoreSession();
            harness.service.complete(AuthOutcome.of(AuthOutcome.Status.NO_SESSION));

            assertEquals(null, harness.store.id);
            assertEquals(View.VISIBLE, owner.get().findViewById(R.id.login_panel).getVisibility());
        }
    }

    @Test
    public void restoreStorageErrorPreservesPersistedSessionForRetry() {
        try (ActivityController<MainActivity> owner =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            Harness harness = new Harness(owner.get());
            harness.store.id = 99L;
            harness.controller.bind();
            harness.controller.restoreSession();
            harness.service.complete(AuthOutcome.of(AuthOutcome.Status.ERROR));

            assertEquals(Long.valueOf(99L), harness.store.id);
            assertEquals(View.VISIBLE, owner.get().findViewById(R.id.login_panel).getVisibility());
        }
    }

    @Test
    public void navigationClearsAllPasswordFields() {
        try (ActivityController<MainActivity> owner =
                     Robolectric.buildActivity(MainActivity.class).setup()) {
            Harness harness = new Harness(owner.get());
            harness.controller.bind();
            setText(owner.get(), R.id.login_password, "secret1");
            owner.get().<Button>findViewById(R.id.show_register_button).performClick();
            assertEquals("", text(owner.get(), R.id.login_password));

            setText(owner.get(), R.id.register_password, "secret2");
            setText(owner.get(), R.id.register_confirmation, "secret2");
            assertTrue(harness.controller.handleBack());
            assertEquals("", text(owner.get(), R.id.register_password));
            assertEquals("", text(owner.get(), R.id.register_confirmation));
        }
    }

    private static void setText(MainActivity activity, int id, String value) {
        activity.<EditText>findViewById(id).setText(value);
    }

    private static String text(MainActivity activity, int id) {
        return activity.<EditText>findViewById(id).getText().toString();
    }

    private static final class Harness {
        final FakeAuthService service = new FakeAuthService();
        final FakeSessionStore store = new FakeSessionStore();
        final ActiveSession session = new ActiveSession();
        final AuthUiController controller;

        Harness(MainActivity activity) {
            controller = new AuthUiController(activity, service, store, session,
                    new UsernamePolicy(), new PasswordPolicy());
        }
    }

    private static final class FakeSessionStore implements SessionStore {
        Long id;
        @Override public Long getActiveUserId() { return id; }
        @Override public void saveActiveUserId(long userId) { id = userId; }
        @Override public void clear() { id = null; }
    }

    private static final class FakeAuthService implements AuthService {
        int registerCalls;
        Callback callback;
        @Override public void register(
                UsernameValidation username, char[] password, Callback callback) {
            registerCalls++;
            this.callback = callback;
        }
        @Override public void login(
                UsernameValidation username, char[] password, Callback callback) {
            this.callback = callback;
        }
        @Override public void restore(long userId, Callback callback) {
            this.callback = callback;
        }
        void complete(AuthOutcome outcome) { callback.onComplete(outcome); }
        @Override public void close() { }
    }
}
