package com.vanvinh2402.tiltescape.input;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public final class AccelerometerTiltInput
        implements SensorEventListener, TiltInput, SensorRegistration {
    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final TiltSignalProcessor processor;
    private boolean registered;

    public AccelerometerTiltInput(Context context, TiltSignalProcessor processor) {
        this((SensorManager) context.getSystemService(Context.SENSOR_SERVICE), processor);
    }

    AccelerometerTiltInput(SensorManager sensorManager, TiltSignalProcessor processor) {
        this.sensorManager = sensorManager;
        this.processor = processor;
        this.accelerometer = sensorManager == null
                ? null
                : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public boolean isAvailable() {
        return accelerometer != null;
    }

    @Override
    public void register() {
        if (registered || !isAvailable()) {
            return;
        }
        processor.reset();
        registered = sensorManager.registerListener(
                this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void unregister() {
        if (!registered) {
            return;
        }
        sensorManager.unregisterListener(this, accelerometer);
        registered = false;
        processor.reset();
    }

    @Override
    public float getHorizontalInput() {
        return processor.getHorizontalInput();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && event.values.length > 0) {
            processor.onSample(event.values[0], event.timestamp);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No accuracy-specific behavior is needed for horizontal tilt control.
    }
}
