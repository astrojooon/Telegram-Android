package org.telegram.ui.xabarchi;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TiltGlareController implements SensorEventListener {
    public interface Listener {
        void onTiltUpdated(float tiltX, float tiltY);
    }

    private static final float LOW_PASS_ALPHA = 0.12f;
    private static final float EPSILON = 0.008f;

    private static volatile TiltGlareController instance;

    private final CopyOnWriteArrayList<WeakReference<Listener>> listeners = new CopyOnWriteArrayList<>();
    private final float[] rotationMatrix = new float[9];
    private final float[] orientation = new float[3];

    private SensorManager sensorManager;
    private Sensor activeSensor;
    private boolean registered = false;

    private float filteredX = 0f;
    private float filteredY = 0f;
    private float lastDispatchedX = 0f;
    private float lastDispatchedY = 0f;

    private TiltGlareController(Context context) {
        if (context != null) {
            sensorManager = (SensorManager) context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                activeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                if (activeSensor == null) {
                    activeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
                }
                if (activeSensor == null) {
                    activeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                }
            }
        }
    }

    public static TiltGlareController getInstance(Context context) {
        if (instance == null) {
            synchronized (TiltGlareController.class) {
                if (instance == null) {
                    instance = new TiltGlareController(context);
                }
            }
        }
        return instance;
    }

    public float getTiltX() {
        return LiquidGlassConfig.isTiltGlareEnabled() ? filteredX : 0f;
    }

    public float getTiltY() {
        return LiquidGlassConfig.isTiltGlareEnabled() ? filteredY : 0f;
    }

    public void addListener(Listener listener) {
        if (listener == null) {
            return;
        }
        removeListener(listener);
        listeners.add(new WeakReference<>(listener));
        updateSensorRegistration();
    }

    public void removeListener(Listener listener) {
        Iterator<WeakReference<Listener>> it = listeners.iterator();
        while (it.hasNext()) {
            WeakReference<Listener> ref = it.next();
            Listener l = ref.get();
            if (l == null || l == listener) {
                listeners.remove(ref);
            }
        }
        updateSensorRegistration();
    }

    public void refreshPreferenceState() {
        updateSensorRegistration();
        if (!LiquidGlassConfig.isTiltGlareEnabled()) {
            filteredX = 0f;
            filteredY = 0f;
            dispatchToListeners();
        }
    }

    private void updateSensorRegistration() {
        if (sensorManager == null || activeSensor == null) {
            return;
        }
        boolean shouldRun = LiquidGlassConfig.isTiltGlareEnabled() && !listeners.isEmpty();
        if (shouldRun && !registered) {
            try {
                sensorManager.registerListener(this, activeSensor, SensorManager.SENSOR_DELAY_UI);
                registered = true;
            } catch (Throwable ignored) {
                registered = false;
            }
        } else if (!shouldRun && registered) {
            try {
                sensorManager.unregisterListener(this);
            } catch (Throwable ignored) {
            }
            registered = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!LiquidGlassConfig.isTiltGlareEnabled() || event == null || event.values == null) {
            return;
        }
        float rawX;
        float rawY;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            rawX = clamp(-event.values[0] / 9.81f, -1f, 1f);
            rawY = clamp(event.values[1] / 9.81f, -1f, 1f);
        } else {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientation);
            rawX = clamp(orientation[2] / 0.85f, -1f, 1f);
            rawY = clamp(orientation[1] / 0.85f, -1f, 1f);
        }

        filteredX += LOW_PASS_ALPHA * (rawX - filteredX);
        filteredY += LOW_PASS_ALPHA * (rawY - filteredY);

        if (Math.abs(filteredX - lastDispatchedX) > EPSILON || Math.abs(filteredY - lastDispatchedY) > EPSILON) {
            lastDispatchedX = filteredX;
            lastDispatchedY = filteredY;
            dispatchToListeners();
        }
    }

    private void dispatchToListeners() {
        boolean pruned = false;
        for (WeakReference<Listener> ref : listeners) {
            Listener l = ref.get();
            if (l == null) {
                listeners.remove(ref);
                pruned = true;
            } else {
                l.onTiltUpdated(filteredX, filteredY);
            }
        }
        if (pruned) {
            updateSensorRegistration();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
