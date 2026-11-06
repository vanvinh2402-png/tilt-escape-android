package com.vanvinh2402.tiltescape.ui;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.vanvinh2402.tiltescape.R;
import com.vanvinh2402.tiltescape.auth.ActiveSession;
import com.vanvinh2402.tiltescape.auth.AuthOutcome;
import com.vanvinh2402.tiltescape.auth.AuthService;
import com.vanvinh2402.tiltescape.auth.AuthenticatedUser;
import com.vanvinh2402.tiltescape.auth.PasswordPolicy;
import com.vanvinh2402.tiltescape.auth.PasswordValidation;
import com.vanvinh2402.tiltescape.auth.SessionStore;
import com.vanvinh2402.tiltescape.auth.UsernamePolicy;
import com.vanvinh2402.tiltescape.auth.UsernameValidation;

import java.util.Arrays;
import java.util.Objects;

public final class AuthUiController implements AutoCloseable {
    private final Activity activity;
    private final AuthService authService;
    private final SessionStore sessionStore;
    private final ActiveSession activeSession;
    private final UsernamePolicy usernamePolicy;
    private final PasswordPolicy passwordPolicy;
    private final View loginPanel;
    private final View registerPanel;
    private final View readyPanel;
    private final EditText loginUsername;
    private final EditText loginPassword;
    private final EditText registerUsername;
    private final EditText registerPassword;
    private final EditText registerConfirmation;
    private final TextView loginError;
    private final TextView registerError;
    private final TextView loginLoading;
    private final TextView registerLoading;
    private final TextView currentUsername;
    private final Button loginButton;
    private final Button registerButton;
    private boolean bound;
    private boolean pending;
    private boolean closed;
    private long generation;

    public AuthUiController(
            Activity activity,
            AuthService authService,
            SessionStore sessionStore,
            ActiveSession activeSession,
            UsernamePolicy usernamePolicy,
            PasswordPolicy passwordPolicy) {
        this.activity = Objects.requireNonNull(activity, "activity");
        this.authService = Objects.requireNonNull(authService, "authService");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.activeSession = Objects.requireNonNull(activeSession, "activeSession");
        this.usernamePolicy = Objects.requireNonNull(usernamePolicy, "usernamePolicy");
        this.passwordPolicy = Objects.requireNonNull(passwordPolicy, "passwordPolicy");
        loginPanel = activity.findViewById(R.id.login_panel);
        registerPanel = activity.findViewById(R.id.register_panel);
        readyPanel = activity.findViewById(R.id.ready_panel);
        loginUsername = activity.findViewById(R.id.login_username);
        loginPassword = activity.findViewById(R.id.login_password);
        registerUsername = activity.findViewById(R.id.register_username);
        registerPassword = activity.findViewById(R.id.register_password);
        registerConfirmation = activity.findViewById(R.id.register_confirmation);
        loginError = activity.findViewById(R.id.login_error);
        registerError = activity.findViewById(R.id.register_error);
        loginLoading = activity.findViewById(R.id.login_loading);
        registerLoading = activity.findViewById(R.id.register_loading);
        currentUsername = activity.findViewById(R.id.current_username);
        loginButton = activity.findViewById(R.id.login_button);
        registerButton = activity.findViewById(R.id.register_button);
    }

    public void bind() {
        if (bound) return;
        bound = true;
        activity.findViewById(R.id.show_register_button)
                .setOnClickListener(view -> showRegister());
        activity.findViewById(R.id.register_back_button)
                .setOnClickListener(view -> showLogin(null, null));
        loginButton.setOnClickListener(view -> submitLogin());
        registerButton.setOnClickListener(view -> submitRegistration());
        activity.findViewById(R.id.logout_button).setOnClickListener(view -> logout());
        showLogin(null, null);
    }

    public void restoreSession() {
        if (closed) return;
        Long userId = sessionStore.getActiveUserId();
        if (userId == null) {
            showLogin(null, null);
            return;
        }
        pending = true;
        setLoading(true, false);
        long request = ++generation;
        authService.restore(userId, outcome -> {
            if (!isCurrent(request)) return;
            pending = false;
            setLoading(false, false);
            if (outcome.getStatus() == AuthOutcome.Status.AUTHENTICATED
                    && outcome.getUser() != null) {
                authenticate(outcome.getUser());
            } else {
                activeSession.clear();
                if (outcome.getStatus() == AuthOutcome.Status.NO_SESSION) {
                    sessionStore.clear();
                }
                showLogin(null, outcome.getStatus() == AuthOutcome.Status.ERROR
                        ? activity.getString(R.string.auth_generic_error) : null);
            }
        });
    }

    public boolean handleBack() {
        if (registerPanel.getVisibility() != View.VISIBLE) return false;
        showLogin(null, null);
        return true;
    }

    public boolean isMainMenuAllowed() {
        return activeSession.isAuthenticated();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        generation++;
        authService.close();
    }

    private void submitLogin() {
        if (pending || closed) return;
        UsernameValidation username = usernamePolicy.validate(loginUsername.getText().toString());
        if (!username.isValid()) {
            showError(loginError, usernameMessage(username));
            return;
        }
        char[] password = loginPassword.getText().toString().toCharArray();
        PasswordValidation validation = passwordPolicy.validate(password, password);
        if (!validation.isValid()) {
            showError(loginError, passwordMessage(validation));
            Arrays.fill(password, '\0');
            return;
        }
        pending = true;
        showError(loginError, null);
        setLoading(true, false);
        loginPassword.setText("");
        long request = ++generation;
        try {
            authService.login(username, password, outcome -> {
                if (!isCurrent(request)) return;
                pending = false;
                setLoading(false, false);
                if (outcome.getStatus() == AuthOutcome.Status.AUTHENTICATED
                        && outcome.getUser() != null) {
                    authenticate(outcome.getUser());
                } else if (outcome.getStatus() == AuthOutcome.Status.INVALID_CREDENTIALS) {
                    showError(loginError,
                            activity.getString(R.string.auth_invalid_credentials));
                } else {
                    showError(loginError, activity.getString(R.string.auth_generic_error));
                }
            });
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void submitRegistration() {
        if (pending || closed) return;
        UsernameValidation username =
                usernamePolicy.validate(registerUsername.getText().toString());
        if (!username.isValid()) {
            showError(registerError, usernameMessage(username));
            return;
        }
        char[] password = registerPassword.getText().toString().toCharArray();
        char[] confirmation = registerConfirmation.getText().toString().toCharArray();
        PasswordValidation validation = passwordPolicy.validate(password, confirmation);
        if (!validation.isValid()) {
            showError(registerError, passwordMessage(validation));
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
            return;
        }
        pending = true;
        showError(registerError, null);
        setLoading(true, true);
        registerPassword.setText("");
        registerConfirmation.setText("");
        long request = ++generation;
        try {
            authService.register(username, password, outcome -> {
                if (!isCurrent(request)) return;
                pending = false;
                setLoading(false, true);
                if (outcome.getStatus() == AuthOutcome.Status.REGISTERED) {
                    showLogin(username.getDisplayUsername(),
                            activity.getString(R.string.auth_registered));
                } else if (outcome.getStatus() == AuthOutcome.Status.DUPLICATE_USERNAME) {
                    showError(registerError,
                            activity.getString(R.string.auth_duplicate_username));
                } else {
                    showError(registerError, activity.getString(R.string.auth_generic_error));
                }
            });
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }

    private void authenticate(AuthenticatedUser user) {
        activeSession.authenticate(user);
        sessionStore.saveActiveUserId(user.getId());
        generation++;
        loginPanel.setVisibility(View.GONE);
        registerPanel.setVisibility(View.GONE);
        readyPanel.setVisibility(View.VISIBLE);
        currentUsername.setText(
                activity.getString(R.string.current_user_format, user.getUsername()));
    }

    private void logout() {
        generation++;
        pending = false;
        sessionStore.clear();
        activeSession.clear();
        loginPassword.setText("");
        registerPassword.setText("");
        registerConfirmation.setText("");
        showLogin(null, null);
    }

    private void showRegister() {
        generation++;
        pending = false;
        clearPasswordFields();
        setLoading(false, true);
        showError(registerError, null);
        loginPanel.setVisibility(View.GONE);
        readyPanel.setVisibility(View.GONE);
        registerPanel.setVisibility(View.VISIBLE);
    }

    private void showLogin(String username, String message) {
        generation++;
        pending = false;
        clearPasswordFields();
        setLoading(false, false);
        if (username != null) loginUsername.setText(username);
        showError(loginError, message);
        registerPanel.setVisibility(View.GONE);
        readyPanel.setVisibility(View.GONE);
        loginPanel.setVisibility(View.VISIBLE);
    }

    private void clearPasswordFields() {
        loginPassword.setText("");
        registerPassword.setText("");
        registerConfirmation.setText("");
    }

    private boolean isCurrent(long request) {
        return !closed && request == generation;
    }

    private void setLoading(boolean loading, boolean registration) {
        if (registration) {
            registerLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            registerButton.setEnabled(!loading);
        } else {
            loginLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            loginButton.setEnabled(!loading);
        }
    }

    private void showError(TextView view, String message) {
        view.setText(message == null ? "" : message);
        view.setVisibility(message == null ? View.GONE : View.VISIBLE);
    }

    private String usernameMessage(UsernameValidation validation) {
        switch (validation.getError()) {
            case REQUIRED:
                return activity.getString(R.string.auth_username_required);
            case INVALID_LENGTH:
                return activity.getString(R.string.auth_username_length);
            default:
                return activity.getString(R.string.auth_username_characters);
        }
    }

    private String passwordMessage(PasswordValidation validation) {
        switch (validation.getError()) {
            case REQUIRED:
                return activity.getString(R.string.auth_password_required);
            case TOO_SHORT:
                return activity.getString(R.string.auth_password_length);
            default:
                return activity.getString(R.string.auth_password_mismatch);
        }
    }
}
