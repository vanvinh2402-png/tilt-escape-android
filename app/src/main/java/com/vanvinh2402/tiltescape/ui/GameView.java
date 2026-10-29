package com.vanvinh2402.tiltescape.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Choreographer;
import android.view.View;

import com.vanvinh2402.tiltescape.game.FrameClock;
import com.vanvinh2402.tiltescape.game.GameEngine;
import com.vanvinh2402.tiltescape.game.GameLoop;
import com.vanvinh2402.tiltescape.game.GameSnapshot;
import com.vanvinh2402.tiltescape.game.GameState;
import com.vanvinh2402.tiltescape.game.WorldRect;
import com.vanvinh2402.tiltescape.input.TiltInput;

public final class GameView extends View implements Choreographer.FrameCallback, GameLoop {
    private final GameEngine engine;
    private final TiltInput tiltInput;
    private final FrameClock frameClock = new FrameClock();
    private final Paint backgroundPaint = new Paint();
    private final Paint playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint obstaclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean running;

    public GameView(Context context, GameEngine engine, TiltInput tiltInput) {
        super(context);
        this.engine = engine;
        this.tiltInput = tiltInput;
        backgroundPaint.setColor(Color.rgb(16, 19, 31));
        playerPaint.setColor(Color.rgb(32, 214, 181));
        obstaclePaint.setColor(Color.rgb(255, 94, 109));
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(48.0f);
        setClickable(true);
        setFocusable(true);
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        frameClock.reset();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
        frameClock.reset();
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) {
            return;
        }
        float deltaSeconds = frameClock.nextDeltaSeconds(frameTimeNanos);
        engine.update(tiltInput.getHorizontalInput(), deltaSeconds);
        invalidate();
        if (running) {
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        GameState state = engine.getState();
        if (state == GameState.PAUSED) {
            engine.resume();
        } else if (state == GameState.READY || state == GameState.GAME_OVER) {
            engine.startOrRestart();
        }
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), backgroundPaint);
        GameSnapshot snapshot = engine.snapshot();
        drawWorldRect(canvas, snapshot.getObstacleBounds(), obstaclePaint);
        drawWorldRect(canvas, snapshot.getPlayerBounds(), playerPaint);

        String label = labelFor(snapshot.getState());
        if (label != null) {
            canvas.drawText(label, getWidth() / 2.0f, getHeight() / 2.0f, labelPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    private void drawWorldRect(Canvas canvas, WorldRect rect, Paint paint) {
        canvas.drawRect(
                rect.getLeft() * getWidth(),
                rect.getTop() * getHeight(),
                rect.getRight() * getWidth(),
                rect.getBottom() * getHeight(),
                paint);
    }

    private static String labelFor(GameState state) {
        switch (state) {
            case READY:
                return "TAP TO START";
            case PAUSED:
                return "PAUSED - TAP TO RESUME";
            case GAME_OVER:
                return "GAME OVER - TAP TO RESTART";
            default:
                return null;
        }
    }
}
