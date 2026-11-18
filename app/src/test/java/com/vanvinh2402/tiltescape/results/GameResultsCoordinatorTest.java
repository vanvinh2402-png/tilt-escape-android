package com.vanvinh2402.tiltescape.results;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public final class GameResultsCoordinatorTest {
    @Test
    public void saveRunsOnDatabaseExecutorNotCaller() {
        Harness harness = new Harness();
        GameResult result = result(4, 1_000L);

        harness.coordinator.save(7L, result);

        assertEquals(0, harness.repository.results.size());
        harness.databaseExecutor.runAll();
        assertEquals(1, harness.repository.results.size());
        assertEquals(4, harness.repository.results.get(0).getScore());
    }

    @Test
    public void saveThenLoadPreservesQueueOrder() {
        Harness harness = new Harness();
        RecordingCallback callback = new RecordingCallback();

        harness.coordinator.save(7L, result(9, 2_000L));
        harness.coordinator.loadAchievements(7L, callback);
        harness.databaseExecutor.runAll();

        assertEquals(0, callback.loadedCount);
        harness.mainDispatcher.runAll();
        assertEquals(1, callback.loadedCount);
        assertEquals(9, callback.data.getHighScore());
        assertEquals(1, callback.data.getRecent().size());
        assertEquals(9, callback.data.getRecent().get(0).getScore());
    }

    @Test
    public void differentUsersNeverMix() {
        Harness harness = new Harness();
        RecordingCallback first = new RecordingCallback();
        RecordingCallback second = new RecordingCallback();

        harness.coordinator.save(7L, result(7, 1_000L));
        harness.coordinator.save(9L, result(99, 2_000L));
        harness.coordinator.loadAchievements(7L, first);
        harness.coordinator.loadAchievements(9L, second);
        harness.databaseExecutor.runAll();
        harness.mainDispatcher.runAll();

        assertEquals(7, first.data.getHighScore());
        assertEquals(1, first.data.getRecent().size());
        assertEquals(99, second.data.getHighScore());
        assertEquals(1, second.data.getRecent().size());
    }

    @Test
    public void insertFailureDoesNotEscapeOrPreventLaterLoad() {
        Harness harness = new Harness();
        RecordingCallback callback = new RecordingCallback();
        harness.repository.failNextInsert = true;

        harness.coordinator.save(7L, result(3, 1_000L));
        harness.coordinator.loadAchievements(7L, callback);
        harness.databaseExecutor.runAll();
        harness.mainDispatcher.runAll();

        assertEquals(1, callback.loadedCount);
        assertEquals(0, callback.errorCount);
        assertEquals(0, callback.data.getHighScore());
    }

    @Test
    public void queryFailurePostsOnError() {
        Harness harness = new Harness();
        RecordingCallback callback = new RecordingCallback();
        harness.repository.failQueries = true;

        harness.coordinator.loadAchievements(7L, callback);
        harness.databaseExecutor.runAll();
        harness.mainDispatcher.runAll();

        assertEquals(0, callback.loadedCount);
        assertEquals(1, callback.errorCount);
    }

    @Test
    public void closeBeforeMainDispatchSuppressesCallback() {
        Harness harness = new Harness();
        RecordingCallback callback = new RecordingCallback();

        harness.coordinator.loadAchievements(7L, callback);
        harness.databaseExecutor.runAll();
        harness.coordinator.close();
        harness.mainDispatcher.runAll();
        harness.databaseExecutor.runAll();

        assertEquals(0, callback.loadedCount);
        assertEquals(0, callback.errorCount);
        assertEquals(1, harness.repository.closeCount);
    }

    @Test
    public void closeRejectsNewWorkAndClosesRepositoryOnce() {
        Harness harness = new Harness();
        RecordingCallback callback = new RecordingCallback();

        harness.coordinator.close();
        int queuedAfterClose = harness.databaseExecutor.queuedCount();
        harness.coordinator.save(7L, result(2, 1_000L));
        harness.coordinator.loadAchievements(7L, callback);
        harness.coordinator.close();

        assertEquals(queuedAfterClose, harness.databaseExecutor.queuedCount());
        harness.databaseExecutor.runAll();
        harness.mainDispatcher.runAll();
        assertEquals(1, harness.repository.closeCount);
        assertEquals(0, callback.loadedCount);
        assertEquals(0, callback.errorCount);
    }

    @Test
    public void achievementsDataDefensivelyCopiesRecentAndRejectsNegativeHighScore() {
        List<GameResult> mutable = new ArrayList<>();
        mutable.add(result(5, 1_000L));
        AchievementsData data = new AchievementsData(5, mutable);
        mutable.clear();

        assertEquals(1, data.getRecent().size());
        try {
            data.getRecent().clear();
            fail("Expected unmodifiable recent results");
        } catch (UnsupportedOperationException expected) {
            // Immutable view model.
        }
        try {
            new AchievementsData(-1, Collections.emptyList());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Invalid high score is rejected.
        }
    }

    private static GameResult result(int score, long playedAt) {
        return new GameResult(0L, score, score * 1_000L, playedAt);
    }

    private static final class Harness {
        final FakeRepository repository = new FakeRepository();
        final QueuedExecutorService databaseExecutor = new QueuedExecutorService();
        final QueuedDispatcher mainDispatcher = new QueuedDispatcher();
        final GameResultsCoordinator coordinator = new GameResultsCoordinator(
                repository, databaseExecutor, mainDispatcher::post);
    }

    private static final class RecordingCallback implements GameResultsCoordinator.Callback {
        int loadedCount;
        int errorCount;
        AchievementsData data;

        @Override
        public void onLoaded(AchievementsData loaded) {
            loadedCount++;
            data = loaded;
        }

        @Override
        public void onError() {
            errorCount++;
        }
    }

    private static final class FakeRepository implements GameResultRepository {
        final List<GameResult> results = new ArrayList<>();
        final List<Long> owners = new ArrayList<>();
        boolean failNextInsert;
        boolean failQueries;
        int closeCount;

        @Override
        public long insert(long userId, GameResult result) {
            if (failNextInsert) {
                failNextInsert = false;
                throw new IllegalStateException("insert failed");
            }
            long id = results.size() + 1L;
            results.add(new GameResult(id, result.getScore(), result.getSurvivalMillis(),
                    result.getPlayedAtEpochMillis()));
            owners.add(userId);
            return id;
        }

        @Override
        public int getHighScore(long userId) {
            if (failQueries) throw new IllegalStateException("query failed");
            int high = 0;
            for (int index = 0; index < results.size(); index++) {
                if (owners.get(index) == userId) {
                    high = Math.max(high, results.get(index).getScore());
                }
            }
            return high;
        }

        @Override
        public List<GameResult> getRecent(long userId, int limit) {
            if (failQueries) throw new IllegalStateException("query failed");
            List<GameResult> owned = new ArrayList<>();
            for (int index = 0; index < results.size() && owned.size() < limit; index++) {
                if (owners.get(index) == userId) owned.add(results.get(index));
            }
            return owned;
        }

        @Override
        public List<LeaderboardEntry> getLeaderboard(int limit) {
            return Collections.emptyList();
        }

        @Override
        public void close() {
            closeCount++;
        }
    }

    private static final class QueuedDispatcher {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        void post(Runnable task) {
            tasks.add(task);
        }

        void runAll() {
            while (!tasks.isEmpty()) tasks.remove().run();
        }
    }

    private static final class QueuedExecutorService extends AbstractExecutorService {
        private final Queue<Runnable> tasks = new ArrayDeque<>();
        private boolean shutdown;

        @Override
        public void execute(Runnable command) {
            if (shutdown) throw new IllegalStateException("executor is shut down");
            tasks.add(command);
        }

        void runAll() {
            while (!tasks.isEmpty()) tasks.remove().run();
        }

        int queuedCount() {
            return tasks.size();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            List<Runnable> remaining = new ArrayList<>(tasks);
            tasks.clear();
            return remaining;
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown && tasks.isEmpty();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return isTerminated();
        }
    }
}
