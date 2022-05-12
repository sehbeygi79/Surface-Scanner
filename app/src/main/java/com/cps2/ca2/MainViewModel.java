package com.cps2.ca2;

import static java.lang.Math.sqrt;

import android.app.Application;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainViewModel extends AndroidViewModel {
    long startTime = 0;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final float EPSILON = 0.1f;
    public static final float STANDARD_GRAVITY = 9.80665f;

    private boolean isStarted() {
        return startTime != 0;
    }

    MutableLiveData<String> startStopButtonTextLiveData = new MutableLiveData<String>();
    MutableLiveData<String> timerTextLiveData = new MutableLiveData<String>();
    MutableLiveData<List<Entry>> showingEntriesLiveData = new MutableLiveData<>();
    List<Entry> allEntries = new ArrayList<>();

    private Sensor gyroscopeSensor;
    private SensorEventListener gyroscopeListener;

    private Sensor accelerometerSensor;
    private SensorEventListener accelerometerListener;

    private Timer timer = null;


    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    void onStartStopClicked() {
        if (isStarted()) {
            stopRunning();
        } else {
            startRunning();
        }
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long elapsedMillis = System.currentTimeMillis() - startTime;
                int minutes = ((int) (elapsedMillis / 1000 / 60));
                int seconds = ((int) ((elapsedMillis / 1000) % 60));
                long milliseconds = elapsedMillis % 1000;
                String finalText = String.format("%02d", minutes) + ":" + String.format("%02d", seconds) + ":" + String.format("%03d", milliseconds);
                timerTextLiveData.postValue(finalText);
            }
        }, 0, 60);
    }

    private void stopTimer() {
        startTime = 0;
        timer.cancel();
        timer = null;
    }

    private void startRunning() {
        startSensors();
        startTimer();
        startStopButtonTextLiveData.setValue("Stop");

    }

    private void stopRunning() {
        stopSensors();
        stopTimer();
        startStopButtonTextLiveData.setValue("Start");
        showingEntriesLiveData.setValue(Collections.emptyList());
        allEntries.clear();
    }

    private void startSensors() {
        Application app = getApplication();
        SensorManager sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);

        initListeners();

        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(gyroscopeListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);

        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private float currentRotationRateX, currentRotationRateY, currentRotationRateZ;
    private float currentRotationX = 0f, currentRotationY = 0f, currentRotationZ = 0f;
    private long currentRotationTimestamp = 0;

    private float currentForceX, currentForceY, currentForceZ;
    private long currentForceTimestamp = 0;

    private void initListeners() {
        gyroscopeListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                // This timestep's delta rotation to be multiplied by the current rotation
                // after computing it from the gyro sample data.
                if (currentRotationTimestamp != 0) {
                    final float dT = (event.timestamp - currentRotationTimestamp) * NS2S;
                    // Axis of the rotation sample, not normalized yet.
                    float axisX = event.values[0];
                    float axisY = event.values[1];
                    float axisZ = event.values[2];

                    // Calculate the angular speed of the sample
                    float omegaMagnitude = (float) sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

                    // Normalize the rotation vector if it's big enough to get the axis
                    // (that is, EPSILON should represent your maximum allowable margin of error)
                    if (omegaMagnitude > EPSILON) {
                        axisX /= omegaMagnitude;
                        axisY /= omegaMagnitude;
                        axisZ /= omegaMagnitude;
                    }

                    currentRotationRateX = axisX;
                    currentRotationRateY = axisY;
                    currentRotationRateZ = axisZ;
                    currentRotationX += currentRotationRateX * dT;
                    currentRotationY += currentRotationRateY * dT;
                    currentRotationZ += currentRotationRateZ * dT;
                    calculateShowingDate();
                }
                currentRotationTimestamp = event.timestamp;


//                if (currentRotationTimestamp == 0) {
//                    return;
//                }
//                long deltaTime = (long) ((event.timestamp - currentRotationTimestamp) * NS2S);
//                // Rotation values are rad/s
//                currentRotationRateX = (float) Math.toDegrees(event.values[0]);
//                currentRotationRateY = (float) Math.toDegrees(event.values[1]);
//                currentRotationRateZ = (float) Math.toDegrees(event.values[2]);
//
//
//                float accuracy = event.accuracy;
//                currentRotationTimestamp = event.timestamp;
//                doLogic();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        accelerometerListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                // Force values are m/s^2
                currentForceX = event.values[0];
                currentForceY = event.values[1];
                currentForceZ = event.values[2];

                float accuracy = event.accuracy;
                currentForceTimestamp = event.timestamp;
                calculateShowingDate();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
    }

    int xCounter = 0;

    private void calculateShowingDate() {
        if (currentRotationTimestamp == 0 || currentForceTimestamp == 0) {
            return;
        }
        if (Math.abs(currentForceX) <= 0.2 || Math.abs(currentRotationRateX) <= 0.001) {
            return;
        }

        Log.d("SS", "forceZ: " + (STANDARD_GRAVITY - Math.abs(currentForceZ)) + " rotY: " + currentRotationRateY);
        float y = (STANDARD_GRAVITY - Math.abs(currentForceZ)) * currentRotationRateY;
        float x = ++xCounter;
        List<Entry> showingList = showingEntriesLiveData.getValue();
        if (showingList == null || showingList.isEmpty()) {
            showingList = new ArrayList<>();
        }
        if (showingList.size() > 100) {
            int extraCount = showingList.size() - 100;
            for (int i = 0; i <= extraCount; i++) {
                showingList.remove(0);
            }
        }
        Entry addingEntry = new Entry(x, y);
        allEntries.add(addingEntry);
        showingList.add(addingEntry);
        showingEntriesLiveData.postValue(showingList);
    }

    private void stopSensors() {
        Application app = getApplication();
        SensorManager sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(gyroscopeListener, gyroscopeSensor);
        sensorManager.unregisterListener(accelerometerListener, accelerometerSensor);
    }
}