package com.example.hari.bluetoothconnectiontest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

/**
 * Created by hari on 4/18/18.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BLEScanner implements BluetoothAdapter.LeScanCallback {

    private static final String TAG = "BLUETOOTH_SCAN";

    private String deviceName;
    private BluetoothAdapter adapter;
    private Context context;
    private LocationManager manager;

    public BLEScanner(String deviceName, BluetoothAdapter adapter, Context context, LocationManager manager) {
        this.deviceName = deviceName;
        this.adapter = adapter;
        this.context = context;
        this.manager = manager;
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {

        if (bluetoothDevice.getName() != null) {
            Log.d(TAG, "Device found " + bluetoothDevice.getName() + " " + bluetoothDevice.getAddress());
        }

        if (bluetoothDevice.getName() != null && bluetoothDevice.getName().equals(deviceName)) {
            adapter.stopLeScan(this);
            Log.d(TAG, "Device found " + bluetoothDevice.getName() + " " + bluetoothDevice.getAddress());
            BluetoothGatt gatt = bluetoothDevice.connectGatt(context, true, new BLEGattCallback(this.context, manager));
        }
    }
}
