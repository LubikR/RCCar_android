package com.github.lubikr.rccar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class DeviceListAcivity extends AppCompatActivity {
    private final String TAG = DeviceListAcivity.class.getSimpleName();

    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private static final long SCAN_PERIOD = 5000; // Stops BLE scanning after 5 seconds.
    private static final String SERIAL_SERVICE_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"; //Serial port according to bluetooth.org

    private BluetoothAdapter bluetoothAdapter;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private List<BluetoothDevice> deviceList = new ArrayList<>();
    private RecycleViewAdapter recycleViewAdapter;

    private FloatingActionButton discover_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        recyclerView = findViewById(R.id.RecyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        recycleViewAdapter = new RecycleViewAdapter(deviceList, new RecycleViewAdapter.OnItemClickedListener() {
            @Override
            public void onItemClick(BluetoothDevice device) {
                Toast.makeText(getApplicationContext(), "Kliknuto " + device.getAddress(), Toast.LENGTH_SHORT).show();

                Bundle bundle = new Bundle();
                bundle.putString(BluetoothDevice.EXTRA_DEVICE, device.getAddress());

                Intent result = new Intent();
                result.putExtras(bundle);
                setResult(Activity.RESULT_OK, result);
                scanLEDevices(false);
                finish();
            }
        });

        recyclerView.setAdapter(recycleViewAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        //Floating action button disappears when scrolling
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy < 0 && discover_btn.isShown()) {
                    discover_btn.hide();
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    discover_btn.show();
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        discover_btn = findViewById(R.id.DiscoverButton);
        discover_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLEDevices(true);
                deviceList.clear();
                recycleViewAdapter.notifyDataSetChanged();
            }
        });

        setBluetoothON();
    }

    //postDelay = stop scan after SCAN_PERIOD
    private void scanLEDevices(final boolean enable) {
        Handler handler = new Handler();

        final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        // Filter devices with Serial port only!!!
        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SERIAL_SERVICE_UUID)).build();
        List<ScanFilter> scanFilterList = new ArrayList<>();
        //scanFilterList.add(scanFilter);
        ScanSettings scanSettings = new ScanSettings.Builder().build();

        if(enable) {
            handler.postDelayed(new Runnable() {
            @Override
                public void run() {
                    //bluetoothLeScanner.stopScan(leScanCallback);
                    scanLEDevices(false);
                }
            }, SCAN_PERIOD);
                bluetoothLeScanner.startScan(scanFilterList, scanSettings, leScanCallback);
                Toast.makeText(this, R.string.scan_start, Toast.LENGTH_SHORT);
            }
            else {
                bluetoothLeScanner.stopScan(leScanCallback);
                Toast.makeText(this, R.string.scan_stop, Toast.LENGTH_LONG);
            }
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            addDevice(result.getDevice());
        }
    };

    public void addDevice (BluetoothDevice device) {
        boolean deviceFound = false;

        for (BluetoothDevice listdev : deviceList ) {
            if (listdev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }

        if (!deviceFound) {
            deviceList.add(device);
            recycleViewAdapter.notifyDataSetChanged();
        }
    }

    //Enable bluetooth
    private void setBluetoothON() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            startActivity(enableBtIntent);
        }
        else {
            Toast.makeText(this, R.string.BT_already_ON, Toast.LENGTH_SHORT).show();
        }
    }
}
