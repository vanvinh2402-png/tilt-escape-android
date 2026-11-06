package com.vanvinh2402.tiltescape.auth;

import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class AccountCoordinator implements AuthService {
    private static final String TAG = "LocalAuth";

    private final AccountRepository repository;
    private final PasswordHasher hasher;
    private final ExecutorService executor;
    private final Consumer<Runnable> mainDispatcher;
    private final LongSupplier clock;
    private final AtomicBoolean closed = new AtomicBoolean();

    public AccountCoordinator(
            AccountRepository repository,
            PasswordHasher hasher,
            ExecutorService executor,
            Consumer<Runnable> mainDispatcher,
            LongSupplier clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.mainDispatcher = Objects.requireNonNull(mainDispatcher, "mainDispatcher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void register(
            UsernameValidation username, char[] password, Callback callback) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(callback, "callback");
        char[] workerPassword = Arrays.copyOf(password, password.length);
        if (!submit(() -> {
            try {
                PasswordHash hash = hasher.hash(workerPassword);
                long createdAt = Math.max(0L, clock.getAsLong());
                repository.insert(
                        username.getDisplayUsername(),
                        username.getNormalizedUsername(),
                        hash,
                        createdAt);
                dispatch(callback, AuthOutcome.of(AuthOutcome.Status.REGISTERED));
            } catch (SQLiteConstraintException duplicate) {
                dispatch(callback, AuthOutcome.of(AuthOutcome.Status.DUPLICATE_USERNAME));
            } catch (RuntimeException error) {
                Log.e(TAG, "Local registration failed", error);
                dispatch(callback, AuthOutcome.of(AuthOutcome.Status.ERROR));
            } finally {
                Arrays.fill(workerPassword, '\0');
            }
        })) {
            Arrays.fill(workerPassword, '\0');
        }
    }

    @Override
    public void login(UsernameValidation username, char[] password, Callback callback) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(callback, "callback");
        char[] workerPassword = Arrays.copyOf(password, password.length);
        if (!submit(() -> {
            try {
                AccountRecord account =
                        repository.findByNormalizedUsername(username.getNormalizedUsername());
                if (account == null
                        || !hasher.verify(workerPassword, account.getPasswordHash())) {
                    dispatch(callback,
                            AuthOutcome.of(AuthOutcome.Status.INVALID_CREDENTIALS));
                    return;
                }
                dispatch(callback, AuthOutcome.authenticated(
                        new AuthenticatedUser(account.getId(), account.getUsername())));
            } catch (RuntimeException error) {
                Log.e(TAG, "Local login failed", error);
                dispatch(callback, AuthOutcome.of(AuthOutcome.Status.ERROR));
            } finally {
                Arrays.fill(workerPassword, '\0');
            }
        })) {
            Arrays.fill(workerPassword, '\0');
        }
    }

    @Override
    public void restore(long userId, Callback callback) {
        Objects.requireNonNull(callback, "callback");
        submit(() -> {
            try {
                AccountRecord account = repository.findById(userId);
                if (account == null) {
                    dispatch(callback, AuthOutcome.of(AuthOutcome.Status.NO_SESSION));
                    return;
                }
                dispatch(callback, AuthOutcome.authenticated(
                        new AuthenticatedUser(account.getId(), account.getUsername())));
            } catch (RuntimeException error) {
                Log.e(TAG, "Local session restore failed", error);
                dispatch(callback, AuthOutcome.of(AuthOutcome.Status.ERROR));
            }
        });
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            executor.execute(() -> {
                try {
                    repository.close();
                } catch (RuntimeException error) {
                    Log.e(TAG, "Unable to close local account repository", error);
                }
            });
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to queue local account close", error);
        } finally {
            executor.shutdown();
        }
    }

    private boolean submit(Runnable task) {
        if (closed.get()) return false;
        try {
            executor.execute(task);
            return true;
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to queue local authentication work", error);
            return false;
        }
    }

    private void dispatch(Callback callback, AuthOutcome outcome) {
        try {
            mainDispatcher.accept(() -> {
                if (!closed.get()) callback.onComplete(outcome);
            });
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to dispatch local authentication callback", error);
        }
    }
}
