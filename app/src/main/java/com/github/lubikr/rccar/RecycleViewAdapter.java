package com.github.lubikr.rccar;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecycleViewAdapter extends RecyclerView.Adapter<RecycleViewAdapter.AdapterHodler> {
    private List<BluetoothDevice> deviceList;
    private OnItemClickedListener onClickedListener;

    public RecycleViewAdapter(List<BluetoothDevice> deviceList, OnItemClickedListener onClickedListener) {
        this.deviceList = deviceList;
        this.onClickedListener = onClickedListener;
    }

    public static class AdapterHodler extends RecyclerView.ViewHolder {
        public TextView deviceName, deviceMAC;

        public AdapterHodler(View view) {
            super(view);
            deviceMAC = view.findViewById(R.id.DeviceMAC);
            deviceName = view.findViewById(R.id.DeviceName);
        }

        public void bind (final BluetoothDevice device, final OnItemClickedListener onItemClickedListener) {
            deviceName.setText(device.getName());
            deviceMAC.setText(device.getAddress());

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickedListener.onItemClick(device);
                }
            });
        }
    }

    public interface OnItemClickedListener {
        void onItemClick(BluetoothDevice device);
    }

    @Override
    public AdapterHodler onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_list_layout, parent, false);
        return new AdapterHodler(itemView);
    }

    @Override
    public void onBindViewHolder(AdapterHodler holder, int position) {
        holder.bind(deviceList.get(position), onClickedListener);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }
}
