package com.example.hari.bluetoothconnectiontest;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import java.util.Set;

public class BlueToothInit extends AppCompatActivity {

    private static final String TAG = "RASP";
    private static final String ERROR_TAG = "ERR_RASP";
    private static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter adapter;

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
                if (!adapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }

            }
        });

        Button queueDevices = findViewById(R.id.queue_paired_devices);
        queueDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();

                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress(); // MAC address
                        Log.d(TAG, deviceName + " " + deviceHardwareAddress);
                        if (device.getName() != null && device.getName().equals("raspberrypi")) {
                            Intent navIntent = new Intent(BlueToothInit.this, SetUpConnection.class);
                            navIntent.putExtra("Address", device);
                            startActivity(navIntent);
                        }
                    }
                } else {
                    Log.e(ERROR_TAG, "No devices");
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(mReceiver, filter);
                    adapter.startDiscovery();
                }
            }
        });


    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, deviceName + " " + deviceHardwareAddress);
                if (device.getName() != null && device.getName().equals("raspberrypi")) {
                    Intent navIntent = new Intent(BlueToothInit.this, SetUpConnection.class);
                    navIntent.putExtra("Address", device);
                    context.startActivity(navIntent);

                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}


