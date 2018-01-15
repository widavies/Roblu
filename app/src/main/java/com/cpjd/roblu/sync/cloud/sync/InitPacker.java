package com.cpjd.roblu.sync.cloud.sync;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

/**
 * The initial passive push that pushes around 500
 * checkouts to the cloud! doInBackground() returns
 * true if the push & upload was successful
 *
 * We need to generate 1 checkout model per match, per team. So there will be
 * a lot of checkouts.
 *
 * @since 3.5.9
 * @author Will Davies
 */
public class InitPacker extends AsyncTask<Void, Void, Boolean> {

    private final Activity activity;
    private final long eventID;

    private ProgressDialog d;

    public InitPacker(Activity activity, long eventID) {
        this.activity = activity;
        this.eventID = eventID;

        d = ProgressDialog.show(activity, "Uploading...", "Please wait while Roblu uploads the event to Roblu Cloud.", true);
    }

    protected Boolean doInBackground(Void... params) {
        return true;
    }
    @Override // Update UI with either success or fail message
    protected void onPostExecute(Boolean result) {

    }



}
