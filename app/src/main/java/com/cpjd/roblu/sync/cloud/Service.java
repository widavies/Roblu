package com.cpjd.roblu.sync.cloud;

import android.content.Intent;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cpjd.http.Request;
import com.cpjd.models.CloudCheckout;
import com.cpjd.models.CloudTeam;
import com.cpjd.requests.CloudCheckoutRequest;
import com.cpjd.requests.CloudTeamRequest;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RSyncSettings;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.notifications.Notify;
import com.cpjd.roblu.sync.SyncHelper;
import com.cpjd.roblu.utils.Utils;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This is the background service for the Roblu Cloud API.
 *
 * @version 2
 * @since 3.6.1
 * @author Will Davies
 */

public class Service extends android.app.Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.d("RBS", "Starting RBS service...");

        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                loop();
            }
        };
        timer.schedule(timerTask, 0, 8000);
        return START_STICKY;
    }

    /**
     * This is the main background service looper, this should perform any necessary
     * Roblu Cloud sync operations
     */
    public void loop() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);
        if(!Utils.hasInternetConnection(getApplicationContext())) {
            Log.d("RBS", "No internet connection detected. Ending loop() early.");
            return;
        }

        /*
         * Create all the utilities we need for this loop
         */
        IO io = new IO(getApplicationContext());
        RSettings settings = io.loadSettings();
        RSyncSettings cloudSettings = io.loadCloudSettings();
        Request r = new Request(settings.getServerIP());
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        CloudTeamRequest teamRequest = new CloudTeamRequest(r, settings.getCode());
        CloudCheckoutRequest checkoutRequest = new CloudCheckoutRequest(r, settings.getCode());

        boolean result = r.ping();
        if(result) Utils.requestServerHealthRefresh(getApplicationContext(), "online");
        else Utils.requestServerHealthRefresh(getApplicationContext(), "offline");

        if(!result) {
            Log.d("RBS", "Roblu server is down. Unable to connect.");
            return;
        }

        // Load the active event
        REvent[] events = io.loadEvents();
        REvent activeEvent = null;
        for(int i = 0; events != null && events.length > 0 && i < events.length; i++) {
            if(events[i].isCloudEnabled()) {
                activeEvent = events[i];
                break;
            }
        }

        if(activeEvent != null && activeEvent.getReadOnlyTeamNumber() != -1) {
            teamRequest.setTeamNumber(activeEvent.getReadOnlyTeamNumber());
            teamRequest.setCode("");
            checkoutRequest.setTeamNumber(activeEvent.getReadOnlyTeamNumber());
            checkoutRequest.setTeamCode("");
        }

        /*
         * Check if a purge is requested
         */
        if(cloudSettings.isPurgeRequested() && checkoutRequest.purge()) {
            cloudSettings.setPurgeRequested(false);
            cloudSettings.setTeamSyncID(0);
            cloudSettings.getCheckoutSyncIDs().clear();
            Log.d("RBS", "Event successfully purged from Roblu Cloud.");
            io.saveCloudSettings(cloudSettings);
            Notify.notifyNoAction(getApplicationContext(), "Event purged", "Active event successfully removed from Roblu Cloud.");
            return;
        }

        if(activeEvent == null) return;

        // Create the sync helper
        SyncHelper syncHelper = new SyncHelper(getApplicationContext(), activeEvent, SyncHelper.MODES.NETWORK);

        RForm form = io.loadForm(activeEvent.getID());

        /*
         *
         * Begin all the checks!
         *
         */

        /*
         * Check to see if the form was modified and needs to be uploaded
         */
        if(form.isUploadRequired()) {
            try {
                teamRequest.pushForm(mapper.writeValueAsString(form));
                form.setUploadRequired(false);
                io.saveForm(activeEvent.getID(), form);
                Notify.notifyNoAction(getApplicationContext(), "Form uploaded", "Successfully uploaded RForm to the server.");
                Log.d("RBS-Service", "Successfully uploaded RForm to the server.");
            } catch(Exception e) {
                Log.d("RBS-Service", "Failed to complete an upload required request for RForm.");
            }
        }

        /*
         * Check to see if the UI model should be uploaded
         */
        if(settings.getRui().isUploadRequired()) {
            try {
                teamRequest.pushUI(mapper.writeValueAsString(settings.getRui()));
                settings.getRui().setUploadRequired(false);
                io.saveSettings(settings);
                Log.d("RBS-Service", "Successfully uploaded RUI to the server.");
            } catch(Exception e) {
                Log.d("RBS-Service", "Failed to complete an upload required request for RUI.");
            }
        }

        /*
         * Check for cloud team updates
         */
        try {
            CloudTeam t = teamRequest.getTeam(cloudSettings.getTeamSyncID());

            if(t != null) {
                /*
                 * If a different master app overwrites the cloud app with a different event, run this check to prevent conflicts
                 * from happening.
                 */
                if(t.getActiveEventName() != null && !t.getActiveEventName().equals("") && activeEvent.getName() != null && !t.getActiveEventName().equals(activeEvent.getName())) {
                    activeEvent.setCloudEnabled(false);
                    cloudSettings.getCheckoutSyncIDs().clear();
                    io.saveCloudSettings(cloudSettings);
                    io.saveEvent(activeEvent);
                    return;
                }

                // Merge RForm
                form = mapper.readValue(t.getForm(), RForm.class);
                form.setUploadRequired(false);
                io.saveForm(activeEvent.getID(), form);

                // Merge RUI
                RUI rui = mapper.readValue(t.getUi(), RUI.class);
                rui.setUploadRequired(false);
                settings.setRui(rui);
                settings = io.loadSettings(); // make sure to refresh this
                io.saveSettings(settings);

                // Update the sync ID
                cloudSettings.setTeamSyncID((int)t.getSyncID());
                io.saveCloudSettings(cloudSettings);

                Log.d("RBS-Service", "Successfully pulled team data from the server.");
            }
        } catch(Exception e) {
            Log.d("RBS-Service", "Failed to pull team data from the server: "+e.getMessage());
        }

        /*
         *
         * Alright, into the belly of the beast.
         * This code will check for completed checkouts on the server and merge them with the local repository.
         * Shall we begin?
         *
         */
        try {
            CloudCheckout[] checkouts = checkoutRequest.pullCompletedCheckouts(syncHelper.packSyncIDs(cloudSettings.getCheckoutSyncIDs()));

            syncHelper.unpackCheckouts(checkouts, cloudSettings);

            io.saveCloudSettings(cloudSettings);

        } catch(Exception e) {
            Log.d("RBS-Service", "An error occurred while fetching completed checkouts. "+e.getMessage());
        }

        /*
         * Next, uploading everything from /pending/
         */
        try {
            Log.d("RBS-Service", "Checking for any checkouts to upload...");

            ArrayList<RCheckout> checkouts = new ArrayList<>(Arrays.asList(io.loadPendingCheckouts()));

            boolean wasSuccess = checkoutRequest.pushCheckouts(syncHelper.packCheckouts(checkouts));

            if(wasSuccess) {
                for(RCheckout checkout : checkouts) {
                    io.deletePendingCheckout(checkout.getID());
                }
                Notify.notifyNoAction(getApplicationContext(), "Uploaded new checkouts", "Uploaded "+checkouts.size()+" new checkout(s).");
            }
            Log.d("RBS-Service", "Uploaded "+checkouts.size()+" checkouts.");
        } catch(Exception e) {
            Log.d("RBS-Service", "An error occurred while attempting to push /pending/ checkouts: "+e.getMessage());
        }
        io.saveCloudSettings(cloudSettings);
        Log.d("RBS-Service", "Sleeping Roblu background service for 10 seconds...");
    }
}