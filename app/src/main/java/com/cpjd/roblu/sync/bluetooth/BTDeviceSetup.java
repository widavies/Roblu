package com.cpjd.roblu.sync.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.CommunicationCallback;

/**
 * This class will send the local Bluetooth MAC address to another Bluetooth device so the device doesn't
 * have to search for devices every time it wants to connect. The goal for Bluetooth is simplicity,
 * so forcing device setup will enable all future connections to be "one tap" syncs.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class BTDeviceSetup {

    /**
     * Bluetooth connects are handled by a third party library (https://github.com/OmarAflak/Bluetooth-Library).
     * This library makes Bluetooth connects a whole lot simpler. This variable allows us to interface with it.
     *
     */
    private Bluetooth bluetooth;

    private BTDeviceSetupListener listener;

    public interface BTDeviceSetupListener {
        void success();
        void error(String message);
    }

    public BTDeviceSetup(Bluetooth bluetooth, BTDeviceSetupListener listener) {
        this.bluetooth = bluetooth;
        this.listener = listener;
        if(!bluetooth.isEnabled()) bluetooth.enable();

        start();
    }

    /**
     * Starts listening for a connection
     */
    private void start() {
        /*
         * Retrieve the Bluetooth MAC address that will be sent to the client device
         */
        final String macAddress = BluetoothAdapter.getDefaultAdapter().getAddress();

        bluetooth.setCommunicationCallback(new CommunicationCallback() {
            @Override
            public void onConnect(BluetoothDevice device) {
                Log.d("RBS", "Received connection request from "+device.getName()+", returning BT MAC address...");

                bluetooth.connectToAddress(device.getAddress());
                bluetooth.send(macAddress);
                listener.success();
            }

            @Override
            public void onDisconnect(BluetoothDevice device, String message) {

            }

            @Override
            public void onMessage(String message) {

            }

            @Override
            public void onError(String message) {
                listener.error("An error occurred: "+message);
            }

            @Override
            public void onConnectError(BluetoothDevice device, String message) {
                listener.error("Unable to connect to Bluetooth device "+device.getName()+". Error message: "+message);
            }
        });
    }




}
