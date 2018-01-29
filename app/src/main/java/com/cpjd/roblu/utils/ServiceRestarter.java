package com.cpjd.roblu.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cpjd.roblu.sync.cloud.Service;

public class ServiceRestarter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, Service.class);
        context.startService(serviceIntent);
    }
}
