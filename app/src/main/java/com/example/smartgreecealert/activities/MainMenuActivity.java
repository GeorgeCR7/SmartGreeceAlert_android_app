package com.example.smartgreecealert.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartgreecealert.R;
import com.example.smartgreecealert.helpers.Enums;
import com.example.smartgreecealert.helpers.Language;
import com.example.smartgreecealert.services.LocationService;
import com.example.smartgreecealert.services.PowerService;
import com.example.smartgreecealert.models.Danger;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.Map;
import java.util.UUID;

import static com.example.smartgreecealert.activities.ContactActivity.PREFS_CONTACTS;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Locale;

public class MainMenuActivity extends AppCompatActivity implements SensorEventListener {

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private Language languageHelper;
    private static final int REQ_READ_CONTACTS = 1122;
    private static final int REQ_FINE_LOC = 5432;
    private static final int REQ_SEND_SMS = 2233;
    private static final int PICK_IMAGE_REQUEST = 3344;
    private LocationService locationService;
    private PowerService powerService;
    private boolean locationBound;
    private boolean powerBound;
    private String permissionMessage;
    // instance for firebase storage and StorageReference
    private FirebaseStorage storage;
    private StorageReference storageReference;

    //Countdown timer variables & buttons.
    private static final long START_TIME_IN_MILLIS = 30000;
    private TextView mTextViewCountDown;
    private TextView countdownLabel;
    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mTimeLeftInMillis = START_TIME_IN_MILLIS;

    //Media player for countdown sound effect.
    private MediaPlayer countdownSound;

    //Sensors.
    private SensorManager sensorManager;
    private Sensor sensor;
    private long lastUpdate = 0;
    private float lastX;
    private float lastY;
    private float lastZ;
    private static final int SHAKE_THRESHOLD = 800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        languageHelper = new Language(this);

        //Initialize countdown timer variables.
        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        countdownLabel = findViewById(R.id.countdownLabel);
        mTimerRunning = false;

        mTextViewCountDown.setVisibility(View.INVISIBLE);
        countdownLabel.setVisibility(View.INVISIBLE);

        updateCountDownText();

        //Create sensor manager.
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        //Accelerometer sensor.
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //Register the listener to sensor manager.
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_FINE_LOC);
        } else {
            // Binds the service that monitors location changes
            bindService(new Intent(this, LocationService.class), locationConnection, Context.BIND_AUTO_CREATE);

            // Binds the service that monitors power charging status changes
            bindService(new Intent(this, PowerService.class), powerConnection, Context.BIND_AUTO_CREATE);
        }

        // get the Firebase  storage reference
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
    }

    /**
     * Starts fall countdown timer
     */
    private void startTimer () {
        mTextViewCountDown.setVisibility(View.VISIBLE);
        countdownLabel.setVisibility(View.VISIBLE);

        //Start playing countdown sound effect.
        startPlayer();

        //Start the countdown timer 30 secs ---> 0 secs.
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                mTextViewCountDown.setVisibility(View.INVISIBLE);
                countdownLabel.setVisibility(View.INVISIBLE);

                //Send a SMS message to user's contacts, cause fall-countdown finish.
                if (ContextCompat.checkSelfPermission(MainMenuActivity.this,
                        Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    double longitude = LocationService.getLocation().getLongitude();
                    double latitude = LocationService.getLocation().getLatitude();
                    String message = getString(R.string.fallDanger)
                            .replace("LONGITUDE", String.valueOf(longitude))
                            .replace("LATITUDE", String.valueOf(latitude));
                    sendMessage(message);
                } else {
                    ActivityCompat.requestPermissions(MainMenuActivity.this,
                            new String[] {Manifest.permission.SEND_SMS},
                            200);
                }
                //Reset the timer, cause finish.
                mTimeLeftInMillis = START_TIME_IN_MILLIS;
                updateCountDownText();

                //Write this danger-fall-event to the firebase.
                writeDB(false);
            }
        }.start();

        mTimerRunning = true;
    }

    /**
     * Aborts fall countdown timer
     */
    public void abortTimer (View view) {
        if (!mTimerRunning || mCountDownTimer == null) {
            Toast.makeText(MainMenuActivity.this, R.string.noActiveDanger, Toast.LENGTH_SHORT).show();
            return;
        }
        mCountDownTimer.cancel();
        mTimerRunning = false;

        stopPlayer();
        writeDB(true);

        mTimeLeftInMillis = START_TIME_IN_MILLIS;
        updateCountDownText();

        mTextViewCountDown.setVisibility(View.INVISIBLE);
        countdownLabel.setVisibility(View.INVISIBLE);
    }

    /**
     * Updates countdown timer in text
     */
    private void updateCountDownText () {
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        mTextViewCountDown.setText(timeLeftFormatted);
    }

    //This method creates a new media player and start the sound effect.
    private void startPlayer () {

        //If we do not have a media player, create a new one.
        if (countdownSound == null) {
            countdownSound = MediaPlayer.create(this, R.raw.countdown_30secs);
            countdownSound.setOnCompletionListener(mp -> {
                //Stop the sound effect when it's over.
                stopPlayer();
            });
        }

        //Start the sound of the player.
        countdownSound.start();
    }

    //This method stops the sound effect of the countdown timer.
    private void stopPlayer () {

        if (countdownSound != null) {
            countdownSound.release();
            countdownSound = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayer();
    }

    //Write the danger-fall-event to the firebase.
    private void writeDB(boolean isAborted){

        //Take the reference from Firebase.
        DatabaseReference notificationRef = database.getReference().child("notifications");

        //Take the current timestamp.
        Timestamp timestamp = new Timestamp(new Date().getTime());

        //Create a new Danger object, add this to firebase.
        Danger danger = new Danger
                .Builder()
                .withUserID(mAuth.getUid())
                .withLatLong(LocationService.getLocation().getLatitude(), LocationService.getLocation().getLongitude())
                .withTimestamp(timestamp.toString())
                .withType(Enums.dangerType.FALL)
                .aborted(isAborted)
                .build();
        notificationRef.push().setValue(danger);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQ_READ_CONTACTS: // Contacts permission
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
                            PackageManager.PERMISSION_GRANTED) {
                        Intent intent = new Intent(MainMenuActivity.this, ContactActivity.class);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(MainMenuActivity.this, R.string.contactsPermissionDenied,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case REQ_FINE_LOC: // Location permission
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED) {
                        bindService(new Intent(this, LocationService.class), locationConnection, Context.BIND_AUTO_CREATE);
                        bindService(new Intent(this, PowerService.class), powerConnection, Context.BIND_AUTO_CREATE);
                    }
                } else {
                    Toast.makeText(MainMenuActivity.this, R.string.locationPermissionDenied,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case REQ_SEND_SMS: // Send sms permission
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) ==
                            PackageManager.PERMISSION_GRANTED) {
                        sendMessage(permissionMessage);
                    }
                } else {
                    Toast.makeText(MainMenuActivity.this, R.string.smsPermissionDenied,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    /**
     * Changes application's language
     *
     * @param view View
     */
    public void changeLanguage(View view) {
        languageHelper.showChangeLanguageDialog();
    }

    /**
     * Uploads an image with the fire to firebase storage and notifies
     * all added contacts about the hazard including user's location
     *
     * @param view View
     */
    public void fireHazard(View view) {
        double longitude = LocationService.getLocation().getLongitude();
        double latitude = LocationService.getLocation().getLatitude();
        String message = getString(R.string.fireHazard)
                .replace("LONGITUDE", String.valueOf(longitude))
                .replace("LATITUDE", String.valueOf(latitude));
        uploadImage();
        sendMessage(message);
    }

    /**
     * Uploads an image from user's storage to firebase storage
     */
    public void uploadImage() {
        // Defining Implicit Intent to mobile gallery
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(
                Intent.createChooser(
                        intent,
                        "Select Image from here..."),
                PICK_IMAGE_REQUEST);
    }

    // Override onActivityResult method
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode,
                resultCode,
                data);

        // checking request code and result code
        // if request code is PICK_IMAGE_REQUEST and
        // resultCode is RESULT_OK
        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            // Get the Uri of data
            Uri filePath = data.getData();

            if (filePath != null) {
                // Code for showing progressDialog while uploading
                ProgressDialog progressDialog
                        = new ProgressDialog(this);
                progressDialog.setTitle(getString(R.string.uploading));
                progressDialog.show();

                // Defining the child of storageReference
                StorageReference storageRef = storageReference.child("images/" + UUID.randomUUID().toString());

                // adding listeners on upload
                // or failure of image
                // Progress Listener for loading
                // percentage on the dialog box
                storageRef.putFile(filePath)
                        .addOnSuccessListener(
                                taskSnapshot -> {
                                    // Image uploaded successfully
                                    // Dismiss dialog
                                    progressDialog.dismiss();
                                    Toast.makeText(MainMenuActivity.this,R.string.imageUploaded, Toast.LENGTH_SHORT).show();
                                })
                        .addOnFailureListener(e -> {
                            // Error, Image not uploaded
                            progressDialog.dismiss();
                            Toast.makeText(MainMenuActivity.this,getString(R.string.failure) + e.getMessage(), Toast.LENGTH_SHORT).show();
                        })
                        .addOnProgressListener(
                                taskSnapshot -> {
                                    double progress
                                            = (100.0
                                            * taskSnapshot.getBytesTransferred()
                                            / taskSnapshot.getTotalByteCount());
                                    progressDialog.setMessage(getString(R.string.uploaded) + " " + (int) progress + "%");
                                });
            }
        }
    }

    /**
     * Sends sms message to all added contacts
     *
     * @param message String
     * @return boolean
     */
    public boolean sendMessage(String message) {
        // Request sms send permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.SEND_SMS},
                    REQ_SEND_SMS);
            permissionMessage = message;
            return false;
        } else {
            SharedPreferences contacts = this.getSharedPreferences(PREFS_CONTACTS, MODE_PRIVATE);
            Map<String, ?> keys = contacts.getAll();
            if (keys.isEmpty()) {
                return false;
            }
            SmsManager smsManager = SmsManager.getDefault();
            for(Map.Entry<String, ?> entry : keys.entrySet()) {
                smsManager.sendTextMessage(entry.getValue().toString(), null, message, null, null);
            }
            Toast.makeText(MainMenuActivity.this, R.string.messageSent,
                    Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    /**
     * Log out from Firebase
     *
     * @param view View
     */
    public void logout(View view) {
        mAuth.signOut();
        Intent intent = new Intent(MainMenuActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        if (powerBound) {
            unbindService(powerConnection);
        }
        if (locationBound) {
            unbindService(locationConnection);
        }
        super.onDestroy();
    }

    /**
     * Close app on back pressed
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Gets user contacts
     *
     * @param view View
     */
    public void getContacts(View view) {
        // Request contact permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.READ_CONTACTS},
                    REQ_READ_CONTACTS);
        } else {
            Intent intent = new Intent(MainMenuActivity.this, ContactActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Register the listener to sensor manager.
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - lastX - lastY - lastZ)/ diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    if (!mTimerRunning) {
                        startTimer();
                    }
                }

                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }
    }

    //This method shows the Statistics Activity when proper button clicked.
    public void showStatistics (View view) {
        Intent intent = new Intent(MainMenuActivity.this, StatisticsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // On accuracy changed
    }

    private ServiceConnection locationConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocationService.LocationBinder binder = (LocationService.LocationBinder) service;
            locationService = binder.getService();
            locationBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            locationBound = false;
        }
    };

    private ServiceConnection powerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PowerService.PowerBinder binder = (PowerService.PowerBinder) service;
            powerService = binder.getService();
            powerBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            locationBound = false;
        }
    };
}
