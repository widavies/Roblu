package com.cpjd.roblu.sync.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import lombok.Data;

/**
 * This class is built off the Android Bluetooth API. All Bluetooth code should be accessed through here, this class will simplify
 * communication with callbacks and easy to use methods. If a problem is occurring with Bluetooth, check this first.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
@SuppressWarnings("unused")
@Data
public class Bluetooth {

    /**
     * This is a reference to the Bluetooth adapter, it's used for controlling many of the functions of Roblu
     */
    private BluetoothAdapter bluetoothAdapter;

    /**
     * This is the device identification Roblu is using for Bluetooth communication
     */
    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * This is the if(listener != null) listener that will receive info about certain events happening in this class
     */
    public BluetoothListener listener;

    /**
     * This is a reference to the parent activity, for registering receivers and IntentFilters
     */
    private Activity activity;

    /*
     * Communication
     * One device has to act like a server (Roblu Master in this setup). The end goal in a connection is to
     * obtain a reference to the BluetoothSocket for the connection. AcceptThread and ConnectThread are two
     * different ways to obtain this BluetoothSocket depending on whether you're a server or a client device.
     */

    /**
     * The device that is currently connected to (only applicable for client)
     */
    private BluetoothDevice device;

    /**
     * The AcceptThread will be used by the server to receive incoming connections
     */
    private AcceptThread acceptThread;
    /**
     * The ConnectThread will be used by the client to attempt to connect to a target device
     */
    private ConnectThread connectThread;
    /**
     * This is a reference (could be null) to the OuputStream of the current connection, this is used for
     * sending information to the server
     */
    private OutputStream out;

    /**
     * This if(listener != null) listener can be attached to a calling activity and will receive status updates
     * about what is going on in this class
     */
    public interface BluetoothListener {
        void deviceDiscovered(BluetoothDevice device);
        void messageReceived(String header, String message);
        void deviceConnected(BluetoothDevice device);
        void deviceDisconnected(BluetoothDevice device, String reason);
        void errorOccurred(String message);
        void stateChanged(int state);
        void discoveryStopped();
    }

    /**
     * Creates a Bluetooth Object
     * @param activity reference to the parent activity where IntentFilter should be registered to
     */
    public Bluetooth(Activity activity) {
        this.activity = activity;

        // Obtain the Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Register receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        activity.registerReceiver(receiver, filter);
    }

    /**
     * Checks if this device supports a Bluetooth connection
     * @return true if this device supports Bluetooth
     */
    public boolean isBluetoothSupported() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    /**
     * Checks if Bluetooth is currently enabled on the device
     * @return true if Bluetooth is enabled on the local device
     */
    public boolean isEnabled() {
        if(bluetoothAdapter == null) return false;

        return bluetoothAdapter.isEnabled();
    }

    /**
     * Force enables Bluetooth on the device, the user won't receive
     * a confirmation dialog
     */
    public void enable() {
        if(bluetoothAdapter == null) return;

        if(!isEnabled()) bluetoothAdapter.enable();
    }

    /**
     * Force disables Bluetooth on the device
     */
    public void disable() {
        if(bluetoothAdapter == null) return;

        bluetoothAdapter.disable();
    }

    /**
     * Forces a pair with the target device.
     * @param device The target device to pair to
     */
    public void pair(final BluetoothDevice device){
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (final Exception e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(listener != null) listener.errorOccurred("Bluetooth: Failed to pair to Bluetooth device with name "+device.getName()+".");
                        }
                    });
                }
            });
        }
    }

    /**
     * Disconnects from the current connection if one is found
     */
    public void disconnect() {
        if(acceptThread != null) {
            acceptThread.cancel();
        }
        if(connectThread != null) {
            connectThread.cancel();
        }
        device = null;
    }

    /**
     * Should be called in the parent activity's onDestroy() method
     *
     * Also stops scanning for devices and disables Bluetooth for power
     * saving.
     */
    public void onDestroy() {
        stopScanning();
        disable();
        activity.unregisterReceiver(receiver);
    }

    /**
     * Flags this device as discoverable, for the next 300 seconds, this device will be visible to all devices around it
     */
    public void enableDiscoverability() {
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        activity.startActivity(discoverableIntent);
    }

    /**
     * Starts searching for nearby for nearby devices,
     * the if(listener != null) listener will be updated when new devices are found
     */
    public void startScanning() {
        if(bluetoothAdapter == null) return;

        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    /**
     * Stops searching for other devices
     */
    public void stopScanning() {
        if(bluetoothAdapter == null) return;

        bluetoothAdapter.cancelDiscovery();
    }

    /**
     * Connects to the target device
     * @param macAddress the Bluetooth MAC address of the target device
     */
    public void connectToDevice(String macAddress) {
        ConnectThread connectThread = new ConnectThread(bluetoothAdapter.getRemoteDevice(macAddress));
        connectThread.start();
    }

    /**
     * Starts listening for a connection
     */
    public void startServer() {
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    /**
     * Sends a message to the target device
     * @param header the tag of the message, sort of a Meta identifier
     * @param message the message to send to the server
     */
    public void send(final String header,final String message) {
        try {
            String toSend = header+":"+message+"\n";
            if(out != null) out.write(toSend.getBytes());
        } catch(IOException e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(listener != null) listener.deviceDisconnected(device, "Bluetooth: Failed to send message "+message+" with header "+header+".");
                }
            });
        }
    }

    /**
     * AcceptThread acts as the server method for obtaining a connection.
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;
        private InputStream in;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("Roblu Master", uuid);
            } catch (IOException e) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(listener != null) listener.errorOccurred("Bluetooth: Failed to accept incoming Bluetooth device connection request.");
                    }
                });
            }
            serverSocket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            BluetoothSocket socket;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(listener != null) listener.errorOccurred("Bluetooth: Failed to accept incoming Bluetooth socket.");
                        }
                    });
                    break;
                }

                if(socket != null) {
                    try {
                        out = socket.getOutputStream();
                    } catch(IOException e) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(listener != null) listener.errorOccurred("Bluetooth: Failed to obtain output stream from Bluetooth socket.");
                            }
                        });

                    }

                    try {
                        in = socket.getInputStream();

                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        String msg;
                        while((msg = br.readLine()) != null) {
                            final String header = msg.split(":")[0];
                            final String content = msg.substring(header.length() + 1);

                            if(listener != null) listener.messageReceived(header, content);
                        }
                    } catch(IOException e) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(listener != null) listener.errorOccurred("Bluetooth: Failed to read data from Bluetooth socket.");
                            }
                        });
                    }

                    try {
                        serverSocket.close();
                    } catch(IOException e) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(listener != null) listener.errorOccurred("Bluetooth: Failed to close the Bluetooth server socket.");
                            }
                        });
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(listener != null) listener.errorOccurred("Bluetooth: Failed to close server socket.");
                    }
                });
            }
        }
    }

    /**
     * Connects to a target device
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket socket;
        private final BluetoothDevice mmDevice;
        private InputStream in;

        public ConnectThread(BluetoothDevice device1) {
            // Use a temporary object that is later assigned to socket
            // because socket is final.
            BluetoothSocket tmp = null;
            mmDevice = device1;
            device = device1;
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(listener != null) listener.errorOccurred("Bluetooth: Failed to connect to target device "+device.getName()+".");
                    }
                });
            }
            socket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                socket.connect();
            } catch (IOException connectException) {
                try {
                    socket.close();
                } catch (IOException closeException) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(listener != null) listener.errorOccurred("Bluetooth: Failed to close client Bluetooth socket.");
                        }
                    });
                }
                return;
            }

            boolean success = true;


            try {
                out = socket.getOutputStream();
            } catch(IOException e) {
                success = false;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(listener != null) listener.errorOccurred("Bluetooth: Failed to obtain output stream from Bluetooth socket.");
                    }
                });
            }

            if(success)
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(listener != null) listener.deviceConnected(mmDevice);
                    }
                });

            try {
                in = socket.getInputStream();
            } catch(IOException e) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(listener != null) listener.errorOccurred("Bluetooth: Failed to obtain input stream from Bluetooth socket.");
                    }
                });
            }

        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                socket.close();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(listener != null) listener.deviceDisconnected(device, "Connected canceled.");
                    }
                });
            } catch (IOException e) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(listener != null) listener.errorOccurred("Failed to close client Bluetooth socket.");
                    }
                });
            }
        }
    }

    /*
     * RECEIVERS
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(listener != null) listener.deviceDiscovered(device);
                    }
                });
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                startScanning();
            }
            else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(listener != null) listener.stateChanged(state);
                    }
                });
            }
        }
    };

}
