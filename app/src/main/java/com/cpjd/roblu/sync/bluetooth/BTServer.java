package com.cpjd.roblu.sync.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.CommunicationCallback;

/**
 * Manages a Bluetooth connection with a client device and data transfer over it.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class BTServer extends Thread implements CommunicationCallback {

    /**
     * Roblu uses a Bluetooth wrapper library to simplify connections and lessen the amount of bugs.
     * The library is available here: https://github.com/OmarAflak/Bluetooth-Library
     * This library provides easy access to searching, connecting, and sending data between
     * Bluetooth capable devices.
     */
    private Bluetooth bluetooth;

    /**
     * Provides access to a context reference for accessing the file system
     */
    private Context context;

    /**
     * Used for deserializing and serializing objects to and from strings
     */
    private ObjectMapper mapper = new ObjectMapper().configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Creates a BTServer object for syncing to a Bluetooth device
     * @param context {@link #context}
     * @param bluetooth {@link #bluetooth}
     */
    public BTServer(Context context, Bluetooth bluetooth) {
        this.bluetooth = bluetooth;
        this.context = context;
    }

    /**
     * Starts the sync task
     */
    @Override
    public void run() {
        bluetooth.send("Hello from Roblu Master!");
    }

    @Override
    public void onConnect(BluetoothDevice device) {
        Log.d("RBS", "Successfully connected to device "+device.getName()+". Performing a sync now.");

    }

    @Override
    public void onDisconnect(BluetoothDevice device, String message) {
        Log.d("RBS", "Disconnected from Bluetooth device "+device.getName()+". Attempting to connect to the next device in the queue.");
    }

    @Override
    public void onMessage(String message) {
        Log.d("RBS", "Received message from Bluetooth: "+message);
    }

    @Override
    public void onError(String message) {
        Log.d("RBS", "An error occurred in BTServer: "+message);
    }

    @Override
    public void onConnectError(BluetoothDevice device, String message) {
        Log.d("RBS", "Failed to connect to device: "+message);
    }
}
