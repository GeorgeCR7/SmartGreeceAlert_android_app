package com.example.smartgreecealert.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.smartgreecealert.R;
import com.example.smartgreecealert.helpers.Enums;
import com.example.smartgreecealert.models.Danger;
import com.example.smartgreecealert.models.PotentialDanger;
import com.example.smartgreecealert.services.LocationService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static android.content.Context.SENSOR_SERVICE;

public class PowerBroadcastReceiver extends BroadcastReceiver implements SensorEventListener {
    private float lastXAxis;
    private float lastYAxis;
    private float lastZAxis;
    private long lastTime;
    private static final int EARTHQUAKE_THRESHOLD = 150;
    private static final int EARTHQUAKE_SEC_THRESHOLD = 60; // 1 minute
    private static final float EARTHQUAKE_DISTANCE = 20 * 1000f; // 20 kilometers
    private boolean checkAccelerometer;
    private FirebaseAuth mAuth;
    private DatabaseReference potentialDangersRef;
    private DatabaseReference notificationsRef;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        SensorManager sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        potentialDangersRef = database.getReference("potential_dangers");
        notificationsRef = database.getReference("notifications");
        if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    checkAccelerometer = true;
                }
            }, 5000);
        } else {
            checkAccelerometer = false;
            sensorManager.unregisterListener(this, accelerometer);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && checkAccelerometer) {
            float xAxis = event.values[0];
            float yAxis = event.values[1];
            float zAxis = event.values[2];

            long currentTime = System.currentTimeMillis();

            if ((currentTime - lastTime) > 100) {
                long diffTime = (currentTime - lastTime);
                lastTime = currentTime;

                float speed = Math.abs(xAxis + yAxis + zAxis - lastXAxis - lastYAxis - lastZAxis) / diffTime * 10000;

                if (speed > EARTHQUAKE_THRESHOLD) {
                    checkAccelerometer = false;
                    // Check again for earthquakes after 2 minutes
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            checkAccelerometer = true;
                        }
                    }, 120 * 1000L);
                    checkForDanger();
                }

                lastXAxis = xAxis;
                lastYAxis = yAxis;
                lastZAxis = zAxis;
            }
        }
    }

    public void checkForDanger() {
        long now = new Date().getTime();
        PotentialDanger potentialDanger = new PotentialDanger(
                now,
                LocationService.getLocation().getLatitude(),
                LocationService.getLocation().getLongitude()
        );
        potentialDangersRef.child(mAuth.getUid()).setValue(potentialDanger);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                updateFirebase(now);
            }
        }, 2 * 1000L);
    }

    public void updateFirebase(long now) {
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot user : snapshot.getChildren()) {
                    if (!user.getKey().equals(mAuth.getUid()) &&
                            getTimeDiff(user.child("timestamp").getValue(Long.class), now, TimeUnit.SECONDS) < EARTHQUAKE_SEC_THRESHOLD) {
                        Location userLocation = new Location(LocationManager.GPS_PROVIDER);
                        userLocation.setLatitude(user.child("latitude").getValue(Float.class));
                        userLocation.setLongitude(user.child("longitude").getValue(Float.class));
                        float distance = LocationService.getLocation().distanceTo(userLocation);
                        if (distance < EARTHQUAKE_DISTANCE) {
                            Danger danger = new Danger.Builder()
                                    .withUserID(mAuth.getUid())
                                    .withType(Enums.dangerType.EARTHQUAKE)
                                    .withLatLong(LocationService.getLocation().getLatitude(), LocationService.getLocation().getLongitude())
                                    .withTimestamp(new Timestamp(now).toString())
                                    .build();
                            potentialDangersRef.removeEventListener(this);
                            notificationsRef.push().setValue(danger);
                            Toast.makeText(context, R.string.earthquakeDetected,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // On Cancelled
            }
        };
        potentialDangersRef.addValueEventListener(valueEventListener);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                potentialDangersRef.removeEventListener(valueEventListener);
            }
        }, 60 * 1000L);
    }

    public long getTimeDiff(long timeUpdate, long timeNow, TimeUnit timeUnit)
    {
        long diffInSeconds = Math.abs(timeNow - timeUpdate);
        return timeUnit.convert(diffInSeconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // On accuracy changed
    }
}
