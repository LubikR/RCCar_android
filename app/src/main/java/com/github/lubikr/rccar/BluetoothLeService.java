package com.github.lubikr.rccar;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.UUID;

public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    public static final UUID SERIAL_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_CHAR_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private final Binder binder = new LocalBinder();
    private BluetoothGatt gatt;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothGattCharacteristic tx;

    public final static String ACTION_GATT_CONNECTED = "com.github.lubikr.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.github.lubikr.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.github.lubikr.ACTION_GATT_SERVICES_DISCOVERED";

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        closeGatt();
        return super.onUnbind(intent);
    }

    public void initialize() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public void connectGatt(BluetoothDevice device) {
        gatt = device.connectGatt(this, false, gattCallback);
    }

    public void disconnect() {
        if (gatt != null) {
            gatt.disconnect();
        }
    }

    public void closeGatt() {
        if (gatt != null) {
            gatt.close();
            gatt = null;
        }
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    public void send(String string) {
        tx.setValue(string);
        gatt.writeCharacteristic(tx);
        //TODO solve write in progress
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                tx =  gatt.getService(SERIAL_SERVICE_UUID).getCharacteristic(TX_CHAR_UUID);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        private void broadcastUpdate(final String action) {
            final Intent intent = new Intent(action);
            sendBroadcast(intent);
        }
    };
}
