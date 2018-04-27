package com.example.hari.bluetoothconnectiontest;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static android.content.Context.LOCATION_SERVICE;

/**
 * Created by hari on 4/18/18.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BLEGattCallback extends BluetoothGattCallback {

    private static final String TAG = "GATT_CALLBACK";

    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    // UUID for the BTLE client characteristic which is necessary for notifications.
    public static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private boolean makeCall;
    private boolean prevCall;
    private Context context;
    private LocationManager locationManager;

    public BLEGattCallback(Context context, LocationManager manager) {
        makeCall = false;
        prevCall = false;
        this.context = context;
        this.locationManager = manager;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        Log.i("onConnectionStateChange", "Status: " + status);
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                Log.i("gattCallback", "STATE_CONNECTED");
                gatt.discoverServices();
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                Log.e("gattCallback", "STATE_DISCONNECTED");
                break;
            default:
                Log.e("gattCallback", "STATE_OTHER");
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        BluetoothGattService service = gatt.getService(UART_UUID);
        List<BluetoothGattCharacteristic> list = service.getCharacteristics();
        for (int i = 0; i < list.size(); i++) {
            Log.d(TAG, "UUID OF CHAR IS " + list.get(i).getUuid().toString());
            Log.d(TAG, String.valueOf(list.get(i).getProperties()));
        }


        BluetoothGattCharacteristic Rx = service.getCharacteristic(RX_UUID);
        gatt.setCharacteristicNotification(Rx, true);
        Log.d(TAG, String.valueOf(Rx.getProperties()));
        Log.d(TAG, "Permission is " + String.valueOf(Rx.getDescriptors().get(1).getPermissions()));
        BluetoothGattDescriptor desc = Rx.getDescriptor(Rx.getDescriptors().get(1).getUuid());
        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (!gatt.writeDescriptor(desc)) {
            Log.d(TAG, "Couldn't write RX client descriptor value!");
        }

    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        Log.d(TAG, characteristic.getStringValue(0));
        if (characteristic.getStringValue(0).length() == 5) {
            int x2 = ((int) characteristic.getStringValue(0).charAt(1)) - 48;
            int x1 = ((int) characteristic.getStringValue(0).charAt(2)) - 48;
            int x0 = ((int) characteristic.getStringValue(0).charAt(3)) - 48;
            int bool = ((int) characteristic.getStringValue(0).charAt(4)) - 48;
            if (bool == 48) {
                bool = 0;
            } else if (bool == 49) {
                bool = 1;
            }

            int fin = x2 * 100 + x1 * 10 + x0;
            Random random = new Random();

            if (bool == 1 && !prevCall) {
                makeCall = true;
            } else {
                makeCall = false;
            }
            Intent intent = new Intent();
            intent.putExtra("Value", fin);
            intent.setAction("com.bluetooth.receiveHeartRate");
            context.sendBroadcast(intent);
            prevCall = makeCall;
        }
        if (makeCall) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            } else {
                Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    Log.d(TAG, String.valueOf(location.getLatitude()));

                    if (ContextCompat.checkSelfPermission(context,
                            Manifest.permission.SEND_SMS)
                            == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context,
                            Manifest.permission.READ_PHONE_STATE)
                            == PackageManager.PERMISSION_GRANTED) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage("", null, "My Coordinates are: " + String.valueOf(lat) + " " + String.valueOf(lon) , null, null);
                    }
                }
            }
            makeCall = false;
        } else {

        }


    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            byte[] data = characteristic.getValue();
            if (data == null) {
                Log.e(TAG, "onCharacteristicRead: data = null");
                return;
            } else {
                Log.d(TAG, data.toString());
            }
        } else {
            Log.e(TAG, "Read failed");
        }
    }


}
