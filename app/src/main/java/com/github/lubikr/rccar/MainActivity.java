package com.github.lubikr.rccar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {
    private final String TAG = MainActivity.class.getSimpleName();
    private final int REQUEST_DEVICE  = 1;

    private final String FORWARD = "F";
    private final String RIGHT = "R";
    private final String LEFT = "L";
    private final String BACKWARD = "B";
    private final String STOP = "S";

    private Button btn_forward;
    private Button btn_back;
    private Button btn_left;
    private Button btn_right;
    private TextView connectionState_TextView;
    private Toolbar toolbar;
    private boolean connected = false;

    private BluetoothLeService serialBlePort;
    private BluetoothGattCharacteristic characteristicTX;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serialBlePort = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            serialBlePort.initialize();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serialBlePort = null;
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            updateConnectionState(action);
        }
    };

    private static IntentFilter makeBroadcastReceiverFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        return intentFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.Toolbar);

        Intent bindIntent = new Intent(this, BluetoothLeService.class);
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        connectionState_TextView = findViewById(R.id.connectionState_TextView);

        btn_forward = findViewById(R.id.Btn_forward);
        btn_forward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                btn_fce(event, FORWARD);
                return false;
            }
        });

        btn_left = findViewById(R.id.Btn_left);
        btn_left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                btn_fce(event, LEFT);
                return false;
            }
        });

        btn_right = findViewById(R.id.Btn_right);
        btn_right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                btn_fce(event, RIGHT);
                return false;
            }
        });

        btn_back = findViewById(R.id.Btn_back);
        btn_back.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                btn_fce(event, BACKWARD);
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver,makeBroadcastReceiverFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    private void btn_fce (@NotNull MotionEvent event, String direction) {
        if (connected) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                serialBlePort.send(direction);
            }
            if (action == MotionEvent.ACTION_UP) {
                serialBlePort.send(STOP);
            }
        }
        else {
            //TODO make toast to user
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serialBlePort.closeGatt();
        serialBlePort.unbindService(serviceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent =  new Intent(MainActivity.this, DeviceListAcivity.class);
        startActivityForResult(intent, REQUEST_DEVICE);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if  (requestCode == REQUEST_DEVICE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                serialBlePort.connectGatt(device);
            }
        }
    }

    private void updateConnectionState(final String action) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final String text;
                switch (action) {
                    case BluetoothLeService.ACTION_GATT_CONNECTED:
                        text = getString(R.string.connected);
                        connected = true;
                        break;
                    case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                        text = getString(R.string.disconnected);
                        connected = false;
                        break;
                    case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                        text = getString(R.string.connected);
                        connected = true;
                        break;
                        default:
                            text = "";
                            connected = false;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectionState_TextView.setText(text);
                        if (text.equals(getString(R.string.connected))) connectionState_TextView.setTextColor(getResources().getColor(R.color.greenText));
                        else connectionState_TextView.setTextColor(getResources().getColor(R.color.redText));
                    }
                });
            }
        });
    }
}
