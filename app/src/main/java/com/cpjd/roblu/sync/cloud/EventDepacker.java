package com.cpjd.roblu.sync.cloud;

import android.os.AsyncTask;
import android.os.StrictMode;
import android.util.Log;

/**
 * EventDepacker basically does the opposite of what `InitPacker` does.
 * It will download the currently active event from the Roblu Cloud server so that multiple devices
 * can act a Master apps.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class EventDepacker extends AsyncTask<Void, Void, Void> {

    public EventDepacker() {

    }

    @Override
    protected Void doInBackground(Void... params) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        Log.d("RBS", "Executing EventDepacker task...");



        return null;
    }

    @Override
    protected void onPostExecute(Void params) {

    }



}
