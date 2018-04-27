package com.example.hari.bluetoothconnectiontest;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import java.util.Set;

public class BlueToothInit extends AppCompatActivity implements LocationListener {

    private static final String TAG = "RASP";
    private static final String ERROR_TAG = "ERR_RASP";

    private static final int REQUEST_ENABLE_BT = 1;
    LocationManager locationManager;
    BluetoothAdapter adapter;
    BluetoothBroadcastReceiver regularBluetoothReceiver;
    HeartRateBroadcastReceiver receiver;
    String[] arr = new String[20];
    int pointer = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_tooth_init);
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            finish();
        }

        // Start the broadcast receiver to receiver updated values from the heart rate monitor. Data is being sent from the BLEGattCallback class to here
        IntentFilter filter = new IntentFilter("com.bluetooth.receiveHeartRate");
        receiver = new HeartRateBroadcastReceiver();
        registerReceiver(receiver, filter);

        // Check if your android phone has permissions for accessing location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        } else {
            // Start the locationManager update cycles
            locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER
                    , 1000 * 60, 2, this);
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                Log.d(TAG, String.valueOf(location.getLatitude()));
            }
        }

        // Check if your Android phone has permissions for using SMS
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage("+15619061056", null, "Welcome to the heart rate monitor app", null, null);
        } else {
            ActivityCompat.requestPermissions(BlueToothInit.this,new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE},2);
        }

        // Button listener to request a location; Used mainly to test the the location code
        Button reqLoc = findViewById(R.id.request_location);
        reqLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(BlueToothInit.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(BlueToothInit.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(BlueToothInit.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
                } else {
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null) {
                        Log.d(TAG, String.valueOf(location.getLatitude()));
                    }

                }
            }
        });

        //Button to start discovering devices over Bluetooth LE
        Button queueDevices = findViewById(R.id.queue_paired_devices);
        queueDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    // if the Adafruit Bluefruit LE chip is found, connect to it
                    adapter.startLeScan(new BLEScanner("Adafruit Bluefruit LE", adapter, BlueToothInit.this, locationManager));

                }


            }
        });
    }

    // A callback function to handle the permissions requested of the user
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        Log.d(TAG, "Permissions granted");
                        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, this);


                    }

                }
            } case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.SEND_SMS)
                            == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                            Manifest.permission.READ_PHONE_STATE)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        Log.d(TAG, "Permissions granted");

                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage("+15619061056", null, "Welcome to the heart rate monitor app", null, null);

                    }
                }
            }

        }
    }


    // Notify if there is a location change
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Current Location: " + location.getLatitude() + ", " + location.getLongitude());
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public class HeartRateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Receiver", String.valueOf(intent.getIntExtra("Value", 0)));
            int val = intent.getIntExtra("Value", 0);
            if (val >= 30 && val < 220) {
                ListView v = findViewById(R.id.heart_rate_list);

                arr[pointer % arr.length] = String.valueOf(intent.getIntExtra("Value", 0));
                pointer++;
                if (pointer % arr.length == 0 && pointer > 0) {
                    ArrayAdapter adapter = new ArrayAdapter(BlueToothInit.this, android.R.layout.simple_list_item_1, arr);
                    v.setAdapter(adapter);
                }
            }

        }
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
        unregisterReceiver(receiver);
    }
}


