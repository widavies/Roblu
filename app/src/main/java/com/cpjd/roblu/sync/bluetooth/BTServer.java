package com.cpjd.roblu.sync.bluetooth;

import android.content.Context;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Manages a Bluetooth connection with a client device and data transfer over it.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class BTServer extends Thread{

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
     */
    public BTServer(Context context) {
        this.context = context;
    }

    /**
     * Starts the sync task
     */
    @Override
    public void run() {
    }
}
