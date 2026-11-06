package com.vanvinh2402.tiltescape.auth;

import static org.junit.Assert.assertEquals;

import android.database.sqlite.SQLiteConstraintException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public final class AccountCoordinatorTest {
    @Test
    public void registerRunsOffCallerAndUsesClampedInjectedClock() {
        Harness harness = new Harness(-10L);
        RecordingCallback callback = new RecordingCallback();
        char[] password = "secret1".toCharArray();

        harness.coordinator.register(validUsername("Player"), password, callback);
        password[0] = 'X';

        assertEquals(0, harness.repository.records.size());
        harness.worker.runAll();
        assertEquals(1, harness.repository.records.size());
        assertEquals(0L, harness.repository.records.get(0).getCreatedAtEpochMillis());
        assertEquals(0, callback.count);
        harness.main.runAll();
        assertEquals(AuthOutcome.Status.REGISTERED, callback.outcome.getStatus());
    }

    @Test
    public void duplicateAndFailuresMapToStableOutcomes() {
        Harness duplicate = new Harness(1L);
        duplicate.repository.failDuplicate = true;
        assertOutcome(duplicate, callback ->
                duplicate.coordinator.register(validUsername("Player"),
                        "secret1".toCharArray(), callback),
                AuthOutcome.Status.DUPLICATE_USERNAME);

        Harness failure = new Harness(1L);
        failure.repository.failAll = true;
        assertOutcome(failure, callback ->
                failure.coordinator.login(validUsername("Player"),
                        "secret1".toCharArray(), callback),
                AuthOutcome.Status.ERROR);
    }

    @Test
    public void loginUsesGenericInvalidCredentialsAndAuthenticatesCorrectPassword() {
        Harness harness = new Harness(5L);
        assertOutcome(harness, callback ->
                harness.coordinator.login(validUsername("Missing"),
                        "secret1".toCharArray(), callback),
                AuthOutcome.Status.INVALID_CREDENTIALS);

        RecordingCallback registered = new RecordingCallback();
        harness.coordinator.register(validUsername("Player"), "secret1".toCharArray(), registered);
        harness.runAll();

        assertOutcome(harness, callback ->
                harness.coordinator.login(validUsername("Player"),
                        "wrong12".toCharArray(), callback),
                AuthOutcome.Status.INVALID_CREDENTIALS);

        RecordingCallback authenticated = new RecordingCallback();
        harness.coordinator.login(validUsername("PLAYER"), "secret1".toCharArray(), authenticated);
        harness.runAll();
        assertEquals(AuthOutcome.Status.AUTHENTICATED, authenticated.outcome.getStatus());
        assertEquals("Player", authenticated.outcome.getUser().getUsername());
    }

    @Test
    public void restoreAndCloseHandleMissingAndLateWork() {
        Harness harness = new Harness(1L);
        assertOutcome(harness, callback -> harness.coordinator.restore(99L, callback),
                AuthOutcome.Status.NO_SESSION);

        RecordingCallback late = new RecordingCallback();
        harness.coordinator.restore(99L, late);
        harness.worker.runAll();
        harness.coordinator.close();
        harness.main.runAll();
        harness.worker.runAll();
        assertEquals(0, late.count);
        assertEquals(1, harness.repository.closeCount);

        harness.coordinator.restore(99L, late);
        harness.worker.runAll();
        harness.main.runAll();
        assertEquals(0, late.count);
    }

    @Test
    public void unexpectedExecutorRejectionCompletesWithError() {
        FakeRepository repository = new FakeRepository();
        QueuedDispatcher main = new QueuedDispatcher();
        AccountCoordinator coordinator = new AccountCoordinator(
                repository,
                new PasswordHasher(new PredictableRandom(), 26, 1_000),
                new RejectingExecutor(),
                main::post,
                () -> 1L);
        RecordingCallback callback = new RecordingCallback();

        coordinator.register(
                validUsername("Player"), "secret1".toCharArray(), callback);
        main.runAll();

        assertEquals(1, callback.count);
        assertEquals(AuthOutcome.Status.ERROR, callback.outcome.getStatus());
    }

    private static void assertOutcome(
            Harness harness, Operation operation, AuthOutcome.Status expected) {
        RecordingCallback callback = new RecordingCallback();
        operation.run(callback);
        harness.runAll();
        assertEquals(expected, callback.outcome.getStatus());
    }

    private static UsernameValidation validUsername(String username) {
        return new UsernamePolicy().validate(username);
    }

    private interface Operation {
        void run(AuthService.Callback callback);
    }

    private static final class RecordingCallback implements AuthService.Callback {
        int count;
        AuthOutcome outcome;

        @Override
        public void onComplete(AuthOutcome outcome) {
            count++;
            this.outcome = outcome;
        }
    }

    private static final class Harness {
        final FakeRepository repository = new FakeRepository();
        final QueuedExecutor worker = new QueuedExecutor();
        final QueuedDispatcher main = new QueuedDispatcher();
        final AccountCoordinator coordinator;

        Harness(long clock) {
            coordinator = new AccountCoordinator(
                    repository,
                    new PasswordHasher(new PredictableRandom(), 26, 1_000),
                    worker,
                    main::post,
                    () -> clock);
        }

        void runAll() {
            worker.runAll();
            main.runAll();
        }
    }

    private static final class FakeRepository implements AccountRepository {
        final List<AccountRecord> records = new ArrayList<>();
        boolean failDuplicate;
        boolean failAll;
        int closeCount;

        @Override
        public long insert(String username, String normalized, PasswordHash hash, long createdAt) {
            if (failAll) throw new IllegalStateException("storage failed");
            if (failDuplicate) throw new SQLiteConstraintException();
            long id = records.size() + 1L;
            records.add(new AccountRecord(id, username, normalized, hash, createdAt));
            return id;
        }

        @Override
        public AccountRecord findByNormalizedUsername(String normalized) {
            if (failAll) throw new IllegalStateException("storage failed");
            for (AccountRecord record : records) {
                if (record.getNormalizedUsername().equals(normalized)) return record;
            }
            return null;
        }

        @Override
        public AccountRecord findById(long id) {
            if (failAll) throw new IllegalStateException("storage failed");
            for (AccountRecord record : records) if (record.getId() == id) return record;
            return null;
        }

        @Override
        public void close() {
            closeCount++;
        }
    }

    private static final class PredictableRandom extends SecureRandom {
        private int value = 1;

        @Override
        public void nextBytes(byte[] bytes) {
            for (int index = 0; index < bytes.length; index++) bytes[index] = (byte) value++;
        }
    }

    private static final class QueuedDispatcher {
        final Queue<Runnable> tasks = new ArrayDeque<>();
        void post(Runnable task) { tasks.add(task); }
        void runAll() { while (!tasks.isEmpty()) tasks.remove().run(); }
    }

    private static final class QueuedExecutor extends AbstractExecutorService {
        final Queue<Runnable> tasks = new ArrayDeque<>();
        boolean shutdown;
        @Override public void execute(Runnable command) {
            if (shutdown) throw new IllegalStateException("shutdown");
            tasks.add(command);
        }
        void runAll() { while (!tasks.isEmpty()) tasks.remove().run(); }
        @Override public void shutdown() { shutdown = true; }
        @Override public List<Runnable> shutdownNow() {
            shutdown = true;
            List<Runnable> left = new ArrayList<>(tasks);
            tasks.clear();
            return left;
        }
        @Override public boolean isShutdown() { return shutdown; }
        @Override public boolean isTerminated() { return shutdown && tasks.isEmpty(); }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) {
            return isTerminated();
        }
    }

    private static final class RejectingExecutor extends AbstractExecutorService {
        @Override public void execute(Runnable command) {
            throw new IllegalStateException("rejected");
        }
        @Override public void shutdown() { }
        @Override public List<Runnable> shutdownNow() { return new ArrayList<>(); }
        @Override public boolean isShutdown() { return false; }
        @Override public boolean isTerminated() { return false; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return false; }
    }
}
