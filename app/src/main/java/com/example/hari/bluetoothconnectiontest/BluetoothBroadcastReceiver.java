package com.example.hari.bluetoothconnectiontest;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by hari on 4/18/18.
 */

public class BluetoothBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "RASP";
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address
            Log.d(TAG, deviceName + " " + deviceHardwareAddress);
            if (device.getName() != null && device.getName().equals("Adafruit Bluefruit LE 5AF8")) {
                Intent navIntent = new Intent(context, SetUpConnection.class);
                navIntent.putExtra("Address", device);
                context.startActivity(navIntent);

            }
        }
    }
}
