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
import com.cpjd.roblu.models.RCloudSettings;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.notifications.Notify;
import com.cpjd.roblu.utils.Utils;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
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
            Log.d("RBS-Service", "No internet connection detected. Ending loop() early.");
            return;
        }

        /*
         * Create all the utilities we need for this loop
         */
        IO io = new IO(getApplicationContext());
        RSettings settings = io.loadSettings();
        RCloudSettings cloudSettings = io.loadCloudSettings();
        Request r = new Request(settings.getServerIP());
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        CloudTeamRequest teamRequest = new CloudTeamRequest(r, settings.getCode());
        CloudCheckoutRequest checkoutRequest = new CloudCheckoutRequest(r, settings.getCode());

        boolean result = r.ping();
        if(result) Utils.requestServerHealthRefresh(getApplicationContext(), "online");
        else Utils.requestServerHealthRefresh(getApplicationContext(), "offline");

        if(!result) {
            Log.d("Service-RSBS", "Roblu server is down. Unable to connect.");
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

        /*
         * Check if a purge is requested
         */
        if(cloudSettings.isPurgeRequested() && checkoutRequest.purge()) {
            cloudSettings.setPurgeRequested(false);
            Log.d("RBS-Service", "Event successfully purged from Roblu Cloud.");
            io.saveCloudSettings(cloudSettings);
            Notify.notifyNoAction(getApplicationContext(), "Event purged", "Active event successfully removed from Roblu Cloud.");
            return;
        }

        if(activeEvent == null) return;
        RForm form = io.loadForm(activeEvent.getID());

        /*
         *
         * Begin all the checks!
         *
         */

        /*
         * Check to see if the UI was modified and needs to be uploaded
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
        // Download the RUI model from the server
        else {
            try {
                CloudTeam t = teamRequest.getTeam(cloudSettings.getLastTeamSync());
                settings.setRui(mapper.readValue(t.getUi(), RUI.class));
                cloudSettings.setLastTeamSync(System.currentTimeMillis());
                Log.d("RBS-Service", "Successfully downloaded RUI");
            } catch(Exception e) {
                Log.d("RBS-Service", "Failed to download an RUI from the server.");
            }
        }

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
        // Download the RForm from the server
        else {
            try {
                CloudTeam t = teamRequest.getTeam(cloudSettings.getLastTeamSync());
                form = mapper.readValue(t.getForm(), RForm.class);
                form.setUploadRequired(false);
                io.saveForm(activeEvent.getID(), form);
                cloudSettings.setLastTeamSync(System.currentTimeMillis());
                io.saveCloudSettings(cloudSettings);
                Log.d("RBS-Service", "Successfully downloaded a form");
            } catch(Exception e) {
                Log.d("RBS-Service", "Failed to download an RForm from the server.");
            }
        }

        /*
         *
         * Alright, into the belly of the beast.
         * This code will check for completed checkouts on the server and merge them with the local repository.
         * Shall we begin?
         *
         */
        try {
            Log.d("RBS-Service", "Checking for completed checkouts...");
            long maxTimestamp = 0;
            CloudCheckout[] checkouts = checkoutRequest.pullCompletedCheckouts(cloudSettings.getLastCheckoutSync());
            Log.d("RBS-Service", "Pulled: "+checkouts.length);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS", Locale.getDefault());

            for(CloudCheckout s : checkouts) {
                // Handle the timestamp
                long time = 0;
                try {
                    sdf.parse(s.getTime().replace("T", " ").replace("Z", " "));
                } catch(Exception e) {
                    time = 0;
                }
                if(time > maxTimestamp) {
                    maxTimestamp = time;
                }

                // Deserialize the checkout
                RCheckout checkout = mapper.readValue(s.getContent(), RCheckout.class);

                // Make sure to verify the checkout's team
                checkout.getTeam().verify(form);

                /*
                 * BEGIN MERGING
                 * -Let's check for possible conflicts
                 */
                RTeam team = io.loadTeam(activeEvent.getID(), checkout.getTeam().getID());

                // The team doesn't exist locally, so create it anew
                if(team == null) {
                    RTeam newTeam = new RTeam(checkout.getTeam().getName(), checkout.getTeam().getNumber(), checkout.getTeam().getID());
                    newTeam.verify(form);

                    if(checkout.getTeam().getTabs().size() > 1) { // this means the downloaded team was a PIT tab, so override the new team's tabs
                        newTeam.setTabs(checkout.getTeam().getTabs());
                    } else { // otherwise just add them
                        newTeam.addTab(checkout.getTeam().getTabs().get(0));
                    }
                }
                // Data already exists, so do a 'smart' merge
                else {
                    team.verify(form);

                    for(RTab downloadedTab : checkout.getTeam().getTabs()) {
                        boolean matchLocated = false;
                        for(RTab localTab : team.getTabs()) {
                            localTab.setWon(downloadedTab.isWon());

                            // Found the match, start merging
                            if(localTab.getTitle().equalsIgnoreCase(downloadedTab.getTitle())) {
                                /*
                                 * Copy over the edit tabs
                                 */
                                if(downloadedTab.getEdits() != null) localTab.setEdits(downloadedTab.getEdits());

                                for(RMetric downloadedMetric : downloadedTab.getMetrics()) {
                                    for(RMetric localMetric : localTab.getMetrics()) {
                                        // Found the metric, determine if a merge needs to occur
                                        if(downloadedMetric.getID() == localMetric.getID()) {
                                            /*
                                             * We have to deal with one special case scenario - the gallery.
                                             * The gallery should never be overrided, just added to
                                             */
                                            if(downloadedMetric instanceof RGallery && localMetric instanceof RGallery && ((RGallery)localMetric).getImages() != null && ((RGallery)downloadedMetric).getImages() != null) {
                                                ((RGallery)localMetric).getImages().addAll(((RGallery)downloadedMetric).getImages());
                                            }
                                            // If the local metric is already edited, keep whichever data is newest
                                            if(localMetric.isModified()) {
                                                if(checkout.getTeam().getLastEdit() >= team.getLastEdit()) {
                                                    int replaceIndex = localTab.getMetrics().indexOf(localMetric);
                                                    localTab.getMetrics().set(replaceIndex, downloadedMetric);
                                                }
                                            }
                                            // Otherwise, just do a straight override
                                            else {
                                                int replaceIndex = localTab.getMetrics().indexOf(localMetric);
                                                localTab.getMetrics().set(replaceIndex, downloadedMetric);
                                            }
                                            break;
                                        }
                                    }
                                }
                                matchLocated = true;
                                break;
                            }
                        }
                        if(!matchLocated) {
                            // Add as a new match if a merge wasn't performed
                            team.addTab(checkout.getTeam().getTabs().get(0));
                            Collections.sort(team.getTabs());
                        }
                    }

                }

                /*
                 * Reset the last edit tag, if applicable
                 */
                boolean shouldNotify = false;
                if(checkout.getTeam().getLastEdit() != team.getLastEdit()) shouldNotify = true;

                if(checkout.getTeam().getLastEdit() > team.getLastEdit()) team.setLastEdit(checkout.getTeam().getLastEdit());
                io.saveTeam(activeEvent.getID(), team);

                Log.d("RBS-Service", "Merged team: "+checkout.getTeam().getName());

                /*
                 * The way the server is designed, occasionally a checkout will get pulled twice.
                 * If the RTeam time stamp differs from the current, just ignore the notification.
                 */
                if(shouldNotify) Notify.notifyMerged(getApplicationContext(), activeEvent.getID(), checkout);

                // Notify the TeamViewer in case Roblu Master is viewing the data that was just modified
                Utils.requestTeamViewerRefresh(getApplicationContext(), team.getName());
            }

            if(checkouts != null && checkouts.length > 0) {
                cloudSettings.setLastCheckoutSync(maxTimestamp);
                io.saveCloudSettings(cloudSettings);
            }

            // Refresh the UI
            if(checkouts != null && checkouts.length > 0) Utils.requestUIRefresh(getApplicationContext());

        } catch(Exception e) {
            Log.d("RBS-Service", "An error occurred while checking for completed checkouts. "+e.getMessage());
        }

        /*
         * Next, uploading everything from /pending/
         */
        try {
            Log.d("RBS-Service", "Checking for any checkouts to upload...");
            RCheckout[] checkouts = io.loadPendingCheckouts();
            ArrayList<RCheckout> toUpload = new ArrayList<>();
            toUpload.addAll(Arrays.asList(checkouts));
            if(toUpload.size() > 0) {
                boolean wasSuccess = checkoutRequest.pushCheckouts(mapper.writeValueAsString(toUpload));
                if(wasSuccess) {
                    for(RCheckout checkout : toUpload) {
                        io.deletePendingCheckout(checkout.getID());
                    }
                }
            }
            Log.d("RBS-Service", "Uploaded "+toUpload.size()+" checkouts.");
        } catch(Exception e) {
            Log.d("RBS-Service", "An error occurred while attempting to push /pending/ checkouts.");
        }
        io.saveCloudSettings(cloudSettings);
        Log.d("RBS-Service", "Sleeping Roblu background service for 10 seconds...");
    }

}