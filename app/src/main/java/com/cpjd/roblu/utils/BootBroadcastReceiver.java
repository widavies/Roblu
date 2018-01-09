package com.cpjd.roblu.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cpjd.roblu.sync.cloud.sync.Service;

/**
 * Created by Will Davies on 7/22/2017.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {
    static final String ACTION = "android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {
        // BOOT_COMPLETED” start Service
        if (intent.getAction().equals(ACTION)) {
            //Service
            System.out.println("Starting from bootup...");
            Intent serviceIntent = new Intent(context, Service.class);
            context.startService(serviceIntent);
        }
    }


}
