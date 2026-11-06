package com.vanvinh2402.tiltescape;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameResultRecorder;
import com.vanvinh2402.tiltescape.input.AccelerometerTiltInput;
import com.vanvinh2402.tiltescape.input.TiltSignalProcessor;
import com.vanvinh2402.tiltescape.results.GameResultsCoordinator;
import com.vanvinh2402.tiltescape.results.SQLiteGameResultRepository;
import com.vanvinh2402.tiltescape.ui.AchievementsUiController;
import com.vanvinh2402.tiltescape.ui.AuthUiController;
import com.vanvinh2402.tiltescape.ui.GameView;
import com.vanvinh2402.tiltescape.ui.GameplayUiController;
import com.vanvinh2402.tiltescape.ui.SystemBarInsets;
import com.vanvinh2402.tiltescape.auth.AccountCoordinator;
import com.vanvinh2402.tiltescape.auth.ActiveSession;
import com.vanvinh2402.tiltescape.auth.PasswordHasher;
import com.vanvinh2402.tiltescape.auth.PasswordPolicy;
import com.vanvinh2402.tiltescape.auth.SQLiteAccountRepository;
import com.vanvinh2402.tiltescape.auth.SharedPreferencesSessionStore;
import com.vanvinh2402.tiltescape.auth.UsernamePolicy;

import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private GameSessionController sessionController;
    private GameplayUiController gameplayUiController;
    private AchievementsUiController achievementsUiController;
    private GameResultsCoordinator gameResultsCoordinator;
    private GameView gameView;
    private AuthUiController authUiController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SystemBarInsets.applyToPadding(findViewById(R.id.game_root));

        GameEngine engine = new GameEngine();
        ActiveSession activeSession = new ActiveSession();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        AccountCoordinator accountCoordinator = new AccountCoordinator(
                new SQLiteAccountRepository(this),
                new PasswordHasher(),
                Executors.newSingleThreadExecutor(),
                mainHandler::post,
                System::currentTimeMillis);
        authUiController = new AuthUiController(
                this,
                accountCoordinator,
                new SharedPreferencesSessionStore(this),
                activeSession,
                new UsernamePolicy(),
                new PasswordPolicy());
        authUiController.bind();
        authUiController.restoreSession();

        gameResultsCoordinator = GameResultsCoordinator.create(
                new SQLiteGameResultRepository(this));
        achievementsUiController = new AchievementsUiController(
                this, engine, gameResultsCoordinator, activeSession::requireUserId);
        achievementsUiController.bind();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (achievementsUiController.handleBack()) return;
                if (authUiController.handleBack()) return;
                setEnabled(false);
                try {
                    getOnBackPressedDispatcher().onBackPressed();
                } finally {
                    setEnabled(true);
                }
            }
        });

        AccelerometerTiltInput tiltInput = new AccelerometerTiltInput(
                this, new TiltSignalProcessor());
        if (!tiltInput.isAvailable()) {
            TextView sensorStatus = findViewById(R.id.main_menu_sensor_status);
            sensorStatus.setText(R.string.sensor_unavailable);
            sensorStatus.setVisibility(View.VISIBLE);
            findViewById(R.id.start_button).setEnabled(false);
            return;
        }

        gameView = new GameView(this, engine, tiltInput);
        gameView.setId(R.id.game_view);
        FrameLayout gameContainer = findViewById(R.id.game_container);
        gameContainer.addView(gameView, 0, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        GameResultRecorder resultRecorder = new GameResultRecorder(
                gameResultsCoordinator::save,
                activeSession::requireUserId,
                System::currentTimeMillis);
        gameplayUiController = new GameplayUiController(
                this, engine, gameView, resultRecorder,
                achievementsUiController::isVisible,
                authUiController::isMainMenuAllowed);
        gameplayUiController.bind();
        gameView.setSnapshotListener(gameplayUiController::refresh);
        gameView.refreshSnapshot();
        findViewById(R.id.state_overlay).bringToFront();
        sessionController = new GameSessionController(engine, gameView, tiltInput);
    }

    void updateScore(int score) {
        TextView scoreText = findViewById(R.id.score_text);
        scoreText.setText(getString(R.string.score_format, score));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionController != null) {
            sessionController.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (sessionController != null) {
            sessionController.onPause();
            gameView.refreshSnapshot();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (authUiController != null) {
            authUiController.close();
            authUiController = null;
        }
        if (gameResultsCoordinator != null) {
            gameResultsCoordinator.close();
            gameResultsCoordinator = null;
        }
        super.onDestroy();
    }
}
