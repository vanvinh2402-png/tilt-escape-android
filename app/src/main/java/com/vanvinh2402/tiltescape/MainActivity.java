package com.vanvinh2402.tiltescape;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.input.AccelerometerTiltInput;
import com.vanvinh2402.tiltescape.input.TiltSignalProcessor;
import com.vanvinh2402.tiltescape.ui.GameView;
import com.vanvinh2402.tiltescape.ui.SystemBarInsets;

public final class MainActivity extends AppCompatActivity {
    private GameSessionController sessionController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SystemBarInsets.applyToPadding(findViewById(R.id.game_root));

        TextView statusText = findViewById(R.id.status_text);
        AccelerometerTiltInput tiltInput = new AccelerometerTiltInput(
                this, new TiltSignalProcessor());
        if (!tiltInput.isAvailable()) {
            statusText.setText(R.string.sensor_unavailable);
            statusText.setVisibility(View.VISIBLE);
            return;
        }

        GameEngine engine = new GameEngine();
        GameView gameView = new GameView(this, engine, tiltInput);
        gameView.setId(R.id.game_view);
        FrameLayout gameContainer = findViewById(R.id.game_container);
        gameContainer.addView(gameView, 0, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        statusText.setText(R.string.tilt_instructions);
        findViewById(R.id.state_overlay).bringToFront();
        sessionController = new GameSessionController(engine, gameView, tiltInput);
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
        }
        super.onPause();
    }
}
