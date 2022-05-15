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
    private static final float EPSILON = 0.1f;
    public static final float STANDARD_GRAVITY = 9.80665f;

    private boolean isStarted() {
        return startTime != 0;
    }

    MutableLiveData<String> startStopButtonTextLiveData = new MutableLiveData<String>();
    MutableLiveData<String> timerTextLiveData = new MutableLiveData<String>();
    MutableLiveData<List<Entry>> showingRealTimeEntriesLiveData = new MutableLiveData<>();
    List<Entry> allEntries = new ArrayList<>();
    MutableLiveData<List<Entry>> showingFinalEntriesLiveData = new MutableLiveData<>();


    private Sensor gyroscopeSensor;
    private SensorEventListener gyroscopeListener;
    private float rotationDelta;

    private Sensor accelerometerSensor;
    private SensorEventListener accelerometerListener;
    private float accelerometerDelta;

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
                String finalText = String.format("%02d", minutes) + ":" + String.format("%02d", seconds) + ":"
                        + String.format("%03d", milliseconds);
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
        showingRealTimeEntriesLiveData.setValue(Collections.emptyList());
        allEntries.clear();
        //
        xCounter = 0;
        zDist = 0;
        zVel = 0;
        zAccel = 0;
        currentRotationY = 0;
        showingFinalEntriesLiveData.setValue(Collections.emptyList());
    }

    private void stopRunning() {
        stopSensors();
        stopTimer();
        startStopButtonTextLiveData.setValue("Start");

        List finalList = new ArrayList<>(allEntries);
//        for (int i = finalList.size()-1; i>=0; i--) {
//            if (i % 2 == 0) {
//                finalList.remove(i);
//            }
//        }
        showingFinalEntriesLiveData.setValue(finalList);
        showingRealTimeEntriesLiveData.setValue(Collections.emptyList());
         allEntries.clear();
         xCounter = 0;
    }

    private void startSensors() {
        Application app = getApplication();
        SensorManager sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);

        initListeners();

        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(gyroscopeListener, gyroscopeSensor, 50000);

        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(accelerometerListener, accelerometerSensor, 50000);
    }

    private float currentRotationRateX, currentRotationRateY, currentRotationRateZ;
    private float currentRotationY = 0f;
    private long currentRotationTimestamp = 0;

    private float currentAccelX, currentAccelY, currentAccelZ;
    private long currentAccelTimestamp = 0;

    private void initListeners() {
        gyroscopeListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                // This timestep's delta rotation to be multiplied by the current rotation
                // after computing it from the gyro sample data.
                if (currentRotationTimestamp != 0) {
                    rotationDelta = (float) (event.timestamp - currentRotationTimestamp) / 1000000000;
                    // Axis of the rotation sample, not normalized yet.
                    float axisX = event.values[0];
                    float axisY = event.values[1];
                    float axisZ = event.values[2];

                    // Calculate the angular speed of the sample
                    // float omegaMagnitude = (float) sqrt(axisX * axisX + axisY * axisY + axisZ *
                    // axisZ);

                    // Normalize the rotation vector if it's big enough to get the axis
                    // (that is, EPSILON should represent your maximum allowable margin of error)
                    // if (omegaMagnitude > EPSILON) {
                    // axisX /= omegaMagnitude;
                    // axisY /= omegaMagnitude;
                    // axisZ /= omegaMagnitude;
                    // }

                    currentRotationRateX = axisX;
                    currentRotationRateY = axisY;
                    currentRotationRateZ = axisZ;
                    currentRotationY += currentRotationRateY;
                    calculateShowingDate();
                }
                currentRotationTimestamp = event.timestamp;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        accelerometerListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (currentAccelTimestamp != 0) {
                    accelerometerDelta = (float) (event.timestamp - currentAccelTimestamp) / 1000000000;
                    // acceleration values are m/s^2
                    currentAccelX = event.values[0];
                    currentAccelY = event.values[1];
                    currentAccelZ = event.values[2];

                    float accuracy = event.accuracy;
                    calculateShowingDate();
                }
                currentAccelTimestamp = event.timestamp;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
    }

    int xCounter = 0;
    float zDist = 0;
    float zVel = 0;
    float zAccel = 0;
    private float Z_ACCEL_THRESH = 0.85f;
    private float Z_VEL_CONV_RATE = 0.99f;
    private float Z_VEL_CAP = 2f;
    private float Y_ROT_THRESH = (float) (Math.PI / 6);
    private float UPWARD_SCALE = 2f;
    private float DOWNWARD_SCALE = 1f;
    private float Z_DIST_CAP = 2f;
    private float GYRO_AUG_STEP = 0.05f;


    private void calculateShowingDate() {
        if (currentRotationTimestamp == 0 || currentAccelTimestamp == 0) {
            return;
        }
        if (Math.abs(currentAccelX) <= 0.05) {
            return;
        }

        zAccel = currentAccelZ;
        // converging velocity to 0 when acceleration is low
        zVel = (Math.abs(zAccel) < Z_ACCEL_THRESH) ? (float) (zVel * Z_VEL_CONV_RATE)
                : zVel + (float) (zAccel * accelerometerDelta);
        // added cap for vel
        zVel = (zVel > Z_VEL_CAP) ? Z_VEL_CAP : (zVel < -Z_VEL_CAP) ? -Z_VEL_CAP : zVel;

        float displacement = (float) (zVel * accelerometerDelta);
        // using gyroscope to augment ascending/descending data
        zDist += (currentRotationY < -Y_ROT_THRESH) ? GYRO_AUG_STEP : (currentRotationY > Y_ROT_THRESH) ? -GYRO_AUG_STEP : 0;
        // scaling displacement
        zDist += (displacement > 0) ? displacement * UPWARD_SCALE : displacement * DOWNWARD_SCALE;
        // added cap for dist
        zDist = (zDist > Z_DIST_CAP) ? Z_DIST_CAP : (zDist < -Z_DIST_CAP) ? -Z_DIST_CAP : zDist;

        // Log.d("SS", "currentTotationY: " + currentRotationY);
        // Log.d("SS", "displacement: " + displacement);

        float y = zDist;
        float x = ++xCounter;

        // sending data to be displayed on chart
        List<Entry> showingList = showingRealTimeEntriesLiveData.getValue();

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
        showingRealTimeEntriesLiveData.postValue(showingList);
        Log.d("SS", "(" + x + " , " + y + ")");
    }

    private void stopSensors() {
        Application app = getApplication();
        SensorManager sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(gyroscopeListener, gyroscopeSensor);
        sensorManager.unregisterListener(accelerometerListener, accelerometerSensor);
    }
}
