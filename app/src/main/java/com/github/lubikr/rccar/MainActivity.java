package com.github.lubikr.rccar;

import android.annotation.SuppressLint;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
    private final String SPEED = "X";
    private final String LED_LEFT = "A";
    private final String LED_RIGHT = "D";
    private final String LED_LIGHTS = "W";

    private TextView connectionState_TextView;
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.Toolbar);
        setSupportActionBar(toolbar);

        Intent bindIntent = new Intent(this, BluetoothLeService.class);
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        connectionState_TextView = findViewById(R.id.connectionState_TextView);

        Button btn_forward = findViewById(R.id.Btn_forward);
        btn_forward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                btn_fce(event, FORWARD);
                return false;
            }
        });

        Button btn_left = findViewById(R.id.Btn_left);
        btn_left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                btn_fce(event, LEFT);
                return false;
            }
        });

        Button btn_right = findViewById(R.id.Btn_right);
        btn_right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                btn_fce(event, RIGHT);
                return false;
            }
        });

        Button btn_back = findViewById(R.id.Btn_back);
        btn_back.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                btn_fce(event, BACKWARD);
                return false;
            }
        });

        Button btn_LeftLED = findViewById(R.id.Btn_LeftLED);
        btn_LeftLED.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(LED_LEFT);
            }
        });

        Button btn_RightLED = findViewById(R.id.Btn_RightLED);
        btn_RightLED.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(LED_RIGHT);
            }
        });

        Button btn_Lights = findViewById(R.id.Btn_Lights);
        btn_Lights.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send(LED_LIGHTS);
            }
        });

        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;
            String last_progress = "";
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (connected) {
                    // Need 5 steps, so 255/5 = 51
                    progress = progress / 51;
                    progress = progress * 51;
                    this.progress = progress;
                    String toSend = SPEED + String.format("%03d", progress);
                    if (!toSend.equals(last_progress)) {
                        last_progress = toSend;
                        serialBlePort.send(toSend);
                    }
                }
                else {
                    Toast.makeText(getBaseContext(), getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
                    seekBar.setProgress(0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
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
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
        }
    }

    private void send (String text) {
        if (connected) {
            serialBlePort.send(text);
        }
        else {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
        }
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
        Intent intent =  new Intent(MainActivity.this, DeviceListActivity.class);
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
                        text = getString(R.string.connecting);
                        connected = false;
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
                        else if (text.equals(R.string.connecting)) connectionState_TextView.setTextColor(getResources().getColor(R.color.yellowText));
                        else connectionState_TextView.setTextColor(getResources().getColor(R.color.redText));
                    }
                });
            }
        });
    }
}
