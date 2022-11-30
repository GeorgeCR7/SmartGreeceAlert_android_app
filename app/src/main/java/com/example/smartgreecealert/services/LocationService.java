package com.example.smartgreecealert.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LocationService extends Service implements LocationListener {

    private LocationBinder locationBinder = new LocationBinder();
    private static LocationManager manager;
    private static Location myLocation;

    public LocationService() {
        // Service constructor
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return locationBinder;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        myLocation = location;
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        // On provider enabled
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        // On provider disabled
    }

    public class LocationBinder extends Binder {
        @SuppressLint("MissingPermission")
        public LocationService getService() {
            manager = (LocationManager) getSystemService(LOCATION_SERVICE);
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, LocationService.this);
            return LocationService.this;
        }
    }

    @SuppressLint("MissingPermission")
    public static Location getLocation() {
        if (myLocation != null) {
            return myLocation;
        } else {
            return manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
    }

    @Override
    public void onDestroy() {
        myLocation = null;
        super.onDestroy();
    }
}
