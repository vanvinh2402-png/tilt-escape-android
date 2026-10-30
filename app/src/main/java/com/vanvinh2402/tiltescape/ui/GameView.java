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
    private GameSnapshotListener snapshotListener = snapshot -> { };
    private boolean running;

    public GameView(Context context, GameEngine engine, TiltInput tiltInput) {
        super(context);
        this.engine = engine;
        this.tiltInput = tiltInput;
        backgroundPaint.setColor(Color.rgb(16, 19, 31));
        playerPaint.setColor(Color.rgb(32, 214, 181));
        obstaclePaint.setColor(Color.rgb(255, 94, 109));
        setContentDescription(context.getString(com.vanvinh2402.tiltescape.R.string.playfield_description));
        setClickable(true);
        setFocusable(true);
    }

    public void setSnapshotListener(GameSnapshotListener listener) {
        snapshotListener = listener == null ? snapshot -> { } : listener;
    }

    public void refreshSnapshot() {
        snapshotListener.onSnapshotChanged(engine.snapshot());
        invalidate();
    }

    @Override public void start() {
        if (running) return;
        running = true;
        frameClock.reset();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override public void stop() {
        if (!running) return;
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
        frameClock.reset();
    }

    @Override public void doFrame(long frameTimeNanos) {
        if (!running) return;
        if (engine.getState() != GameState.PLAYING) { stop(); invalidate(); return; }
        engine.update(tiltInput.getHorizontalInput(), frameClock.nextDeltaSeconds(frameTimeNanos));
        snapshotListener.onSnapshotChanged(engine.snapshot());
        invalidate();
        if (running && engine.getState() == GameState.PLAYING) {
            Choreographer.getInstance().postFrameCallback(this);
        } else stop();
    }

    @Override public boolean performClick() {
        super.performClick();
        if (engine.getState() == GameState.PAUSED) { engine.resume(); start(); }
        else if (engine.getState() == GameState.READY || engine.getState() == GameState.GAME_OVER) {
            engine.startOrRestart(); start();
        }
        refreshSnapshot();
        return true;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
        GameSnapshot snapshot = engine.snapshot();
        for (WorldRect obstacle : snapshot.getObstacleBounds()) drawWorldRect(canvas, obstacle, obstaclePaint);
        drawWorldRect(canvas, snapshot.getPlayerBounds(), playerPaint);
    }

    @Override protected void onDetachedFromWindow() { stop(); super.onDetachedFromWindow(); }

    private void drawWorldRect(Canvas canvas, WorldRect rect, Paint paint) {
        canvas.drawRect(rect.getLeft()*getWidth(), rect.getTop()*getHeight(),
                rect.getRight()*getWidth(), rect.getBottom()*getHeight(), paint);
    }

}
