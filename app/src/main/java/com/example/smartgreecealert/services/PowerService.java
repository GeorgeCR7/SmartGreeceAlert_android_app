package com.example.smartgreecealert.services;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.smartgreecealert.receivers.PowerBroadcastReceiver;

public class PowerService extends Service {

    private PowerBinder powerBinder = new PowerBinder();
    private PowerBroadcastReceiver powerBroadcastReceiver;

    public PowerService() {
        // Service constructor
    }

    public class PowerBinder extends Binder {
        public PowerService getService() {
            powerBroadcastReceiver = new PowerBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_POWER_CONNECTED);
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            registerReceiver(powerBroadcastReceiver, filter);
            return PowerService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return powerBinder;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(powerBroadcastReceiver);
        super.onDestroy();
    }
}
