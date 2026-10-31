package com.google.ambient.nesthub.receivers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BatteryStatusReceiver extends BroadcastReceiver {

    public interface BatteryListener {
        void onDeviceBatteryChanged(int percent, boolean isCharging, String chargeType);
        void onBluetoothBatteriesChanged(List<BluetoothDeviceInfo> devices);
    }

    public static class BluetoothDeviceInfo {
        public String name;
        public int batteryPercent; // -1 if unknown

        public BluetoothDeviceInfo(String name, int batteryPercent) {
            this.name = name;
            this.batteryPercent = batteryPercent;
        }
    }

    private final BatteryListener listener;
    private int currentPercent = 100;
    private boolean isCharging = false;
    private String chargeType = "Battery";

    public BatteryStatusReceiver(BatteryListener listener) {
        this.listener = listener;
    }

    public void register(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED");
        context.registerReceiver(this, filter);

        // Immediate initial query
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            handleBatteryChanged(context, batteryIntent);
        }
        updateBluetoothDevices(context);
    }

    public void unregister(Context context) {
        try {
            context.unregisterReceiver(this);
        } catch (Exception ignored) {}
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BATTERY_CHANGED.equals(action) ||
            Intent.ACTION_POWER_CONNECTED.equals(action) ||
            Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            handleBatteryChanged(context, intent);
        } else {
            updateBluetoothDevices(context);
        }
    }

    private void handleBatteryChanged(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);

        if (level >= 0 && scale > 0) {
            currentPercent = (int) ((level / (float) scale) * 100);
        }
        isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL);

        if (plugged == BatteryManager.BATTERY_PLUGGED_AC) {
            chargeType = "AC Fast Charge";
        } else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
            chargeType = "USB Charging";
        } else if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
            chargeType = "Dock / Wireless";
        } else {
            chargeType = "Discharging";
        }

        if (listener != null) {
            listener.onDeviceBatteryChanged(currentPercent, isCharging, chargeType);
        }
    }

    public void updateBluetoothDevices(Context context) {
        List<BluetoothDeviceInfo> list = new ArrayList<>();
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && adapter.isEnabled()) {
                Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
                if (bondedDevices != null) {
                    for (BluetoothDevice device : bondedDevices) {
                        try {
                            Method isConnectedMethod = BluetoothDevice.class.getMethod("isConnected");
                            boolean isConnected = (boolean) isConnectedMethod.invoke(device);
                            if (isConnected) {
                                int battery = -1;
                                try {
                                    Method getBattery = BluetoothDevice.class.getMethod("getBatteryLevel");
                                    Object res = getBattery.invoke(device);
                                    if (res instanceof Integer) {
                                        battery = (int) res;
                                    }
                                } catch (Exception ignored) {}
                                String name = device.getName();
                                if (name == null || name.isEmpty()) name = "Bluetooth Device";
                                list.add(new BluetoothDeviceInfo(name, battery));
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (SecurityException ignored) {
            // Missing Bluetooth permission on newer Android versions
        }

        if (listener != null) {
            listener.onBluetoothBatteriesChanged(list);
        }
    }
}

