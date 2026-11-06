package com.vanvinh2402.tiltescape.auth;

public interface AuthService extends AutoCloseable {
    interface Callback {
        void onComplete(AuthOutcome outcome);
    }

    void register(UsernameValidation username, char[] password, Callback callback);

    void login(UsernameValidation username, char[] password, Callback callback);

    void restore(long userId, Callback callback);

    @Override
    void close();
}
