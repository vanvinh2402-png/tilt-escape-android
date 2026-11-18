package com.vanvinh2402.tiltescape.results;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Objects;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class GameResultsCoordinator implements AchievementsLoader, LeaderboardLoader, AutoCloseable {
    private static final String TAG = "GameResults";
    private static final int RECENT_LIMIT = 10;

    public interface Callback {
        void onLoaded(AchievementsData data);

        void onError();
    }

    private final GameResultRepository repository;
    private final ExecutorService databaseExecutor;
    private final Consumer<Runnable> mainDispatcher;
    private final AtomicBoolean closed = new AtomicBoolean();

    GameResultsCoordinator(GameResultRepository repository,
            ExecutorService databaseExecutor,
            Consumer<Runnable> mainDispatcher) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.databaseExecutor = Objects.requireNonNull(databaseExecutor, "databaseExecutor");
        this.mainDispatcher = Objects.requireNonNull(mainDispatcher, "mainDispatcher");
    }

    public static GameResultsCoordinator create(GameResultRepository repository) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        return new GameResultsCoordinator(
                repository,
                Executors.newSingleThreadExecutor(),
                mainHandler::post);
    }

    public void save(long userId, GameResult result) {
        if (userId <= 0L) throw new IllegalArgumentException("userId must be positive");
        Objects.requireNonNull(result, "result");
        submit(() -> {
            try {
                repository.insert(userId, result);
            } catch (RuntimeException error) {
                Log.e(TAG, "Unable to save game result", error);
            }
        });
    }

    public void saveAndReportNewHighScore(long userId, GameResult result,
            Consumer<Boolean> callback) {
        if (userId <= 0L) throw new IllegalArgumentException("userId must be positive");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(callback, "callback");
        submit(() -> {
            try {
                int previousHighScore = repository.getHighScore(userId);
                repository.insert(userId, result);
                boolean isNewHighScore = result.getScore() > previousHighScore;
                dispatch(() -> callback.accept(isNewHighScore));
            } catch (RuntimeException error) {
                Log.e(TAG, "Unable to save game result", error);
                dispatch(() -> callback.accept(false));
            }
        });
    }

    @Override
    public void loadAchievements(long userId, Callback callback) {
        if (userId <= 0L) throw new IllegalArgumentException("userId must be positive");
        Objects.requireNonNull(callback, "callback");
        submit(() -> {
            try {
                AchievementsData data = new AchievementsData(
                        repository.getHighScore(userId),
                        repository.getRecent(userId, RECENT_LIMIT));
                dispatch(() -> callback.onLoaded(data));
            } catch (RuntimeException error) {
                Log.e(TAG, "Unable to load achievements", error);
                dispatch(callback::onError);
            }
        });
    }

    @Override
    public void loadLeaderboard(int limit, LeaderboardLoader.Callback callback) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
        Objects.requireNonNull(callback, "callback");
        submit(() -> {
            try {
                List<LeaderboardEntry> entries = repository.getLeaderboard(limit);
                dispatch(() -> callback.onLoaded(entries));
            } catch (RuntimeException error) {
                Log.e(TAG, "Unable to load leaderboard", error);
                dispatch(callback::onError);
            }
        });
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            databaseExecutor.execute(() -> {
                try {
                    repository.close();
                } catch (RuntimeException error) {
                    Log.e(TAG, "Unable to close game results", error);
                }
            });
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to queue game result close", error);
        } finally {
            databaseExecutor.shutdown();
        }
    }

    private void submit(Runnable task) {
        if (closed.get()) return;
        try {
            databaseExecutor.execute(task);
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to queue game result work", error);
        }
    }

    private void dispatch(Runnable callback) {
        try {
            mainDispatcher.accept(() -> {
                if (!closed.get()) callback.run();
            });
        } catch (RuntimeException error) {
            Log.e(TAG, "Unable to dispatch game result callback", error);
        }
    }
}
