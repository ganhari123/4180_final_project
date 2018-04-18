package com.example.hari.bluetoothconnectiontest;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BlueToothInit extends AppCompatActivity {

    private static final String TAG = "RASP";
    private static final String ERROR_TAG = "ERR_RASP";

    private static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter adapter;
    BluetoothGatt gatt;
    BluetoothBroadcastReceiver regularBluetoothReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_tooth_init);
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            finish();
        }
        Button enableBlueTooth = findViewById(R.id.enable_bluetooth);
        enableBlueTooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                turnBluetoothOn();
            }
        });


        Button queueDevices = findViewById(R.id.queue_paired_devices);
        queueDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //checkForBondedDevicesAndConnect();

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    adapter.startLeScan(new BLEScanner("Adafruit Bluefruit LE 5AF8", adapter, BlueToothInit.this));

                }
            }
        });
    }

    private void turnBluetoothOn() {
        if (!adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void checkForBondedDevicesAndConnect() {
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        boolean foundPair = false;
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, deviceName + " " + deviceHardwareAddress);
                if (device.getName() != null && device.getName().equals("Adafruit Bluefruit LE 5AF8")) {
                    foundPair = true;
                    Intent navIntent = new Intent(BlueToothInit.this, SetUpConnection.class);
                    navIntent.putExtra("Address", device);
                    startActivity(navIntent);
                }
            }
            if (foundPair) {
                Log.e(ERROR_TAG, "No devices");
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(regularBluetoothReceiver, filter);
                adapter.startDiscovery();
            }
        } else {
            Log.e(ERROR_TAG, "No devices");
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(regularBluetoothReceiver, filter);
            adapter.startDiscovery();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(regularBluetoothReceiver);
    }
}


