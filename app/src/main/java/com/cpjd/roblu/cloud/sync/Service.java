package com.cpjd.roblu.cloud.sync;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.cpjd.roblu.cloud.api.CloudRequest;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.utils.Text;

import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * This is the background service for the Roblu Cloud API.
 *
 * It performs the following:
 *
 * -Checking for RCheckouts in the InCheckouts database
 * -Pushing any changes to the form
 * -Pushing checkouts after they've been merged (either automatically or explicity)
 *
 * Requests are added to a stack, this service will continually attempt to upload them
 * until it receives a successful response.
 * -Items that are explicitly merged will be flagged with 'requestedExplicitMerge', this service will detect that and attempt to upload it
 * from (/checkoutconflicts)
 * -When we pull items, we'll instantly re-upload them if they auto merge. When we re-upload them, THATS when we delete them
 *
 * Service timing:
 * -Service will start on device startup
 * -Service will run through the stack once every 3 minutes when unopened, once every 5 seconds when open
 *
 * Service can receive new jobs from the main app thread.
 *
 * Service should check for a connection before attempting to upload something
 *
 * @since 3.6.1
 * @author Will Davies
 *
 */

public class Service extends android.app.Service {

    private Looper serviceLooper;
    private ServiceHandler handler;

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("Roblu Service", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        serviceLooper = thread.getLooper();
        handler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("RBS", "Service started at "+Text.convertTime(System.currentTimeMillis()));
        Message message = handler.obtainMessage();
        message.arg1 = startId;
        handler.sendMessage(message);
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            ObjectMapper mapper = new ObjectMapper();
            Loader l = new Loader(getApplicationContext());
            CloudRequest cr;

            while(true) {
                // sleep a bit
                try {
                    if(isAppOnForeground(getApplicationContext())) {
                        Log.d("RBS", "Sleeping for 5 seconds...");
                        Thread.sleep(5000);
                    }
                    else {
                        Log.d("RBS", "Sleeping for 15 seconds...");
                        Thread.sleep(15000);
                    }
                } catch(Exception e) {}

                // first, check if we have an internet connection, if we don't, the background service is useless
                if(!Text.hasInternetConnection(getApplicationContext())) {
                    continue;
                }
                cr = new CloudRequest(l.loadSettings().getAuth(), l.loadSettings().getTeamCode());

                // check if the UI needs to be uploaded
                RSettings settings = l.loadSettings();
                RUI rui = settings.getRui();
                if(rui != null && rui.isModified()) {
                    try {
                        Log.d("RBS", "UI was modified, pushing changes...");
                        cr.pushUI(mapper.writeValueAsString(rui));
                        rui.setModified(false);
                        l.saveSettings(settings);
                        Log.d("RBS", "Form synced");
                    } catch(Exception e) {
                        Log.d("RBS", "Error pushing UI to the server.");
                    }
                }

                /* UPDATES */
                // first, find the active event
                REvent[] events = l.getEvents();
                REvent activeEvent = null;
                for(int i = 0; events != null && events.length > 0 && i < events.length; i++) {
                    if(events[i].isCloudEnabled()) {
                        activeEvent = events[i];
                        break;
                    }
                }
                if(activeEvent == null) continue;

                // check if the form needs to be uploaded
                RForm form = l.loadForm(activeEvent.getID());
                if(form != null  && form.isModified()) {
                    try {
                        Log.d("RBS", "Form was modified, pushing changes...");
                        cr.pushForm(mapper.writeValueAsString(form));
                        form.setModified(false);
                        l.saveForm(form, activeEvent.getID());
                        Log.d("RBS", "Form synced.");
                    } catch(Exception e) {
                        Log.d("RBS", "Error pushing form to the server.");
                    }
                }

                // check if their are any checkouts in RCheckouts
                try {
                    Log.d("RBS", "Checking for ReceivedCheckouts...");
                    JSONArray checkouts = (JSONArray) ((JSONObject)cr.pullCheckouts()).get("data");
                    for(int i = 0; i < checkouts.size(); i++) {
                        JSONObject object = (JSONObject) checkouts.get(i);
                        RCheckout checkout = mapper.readValue(object.get("content").toString(), RCheckout.class);
                        checkout.setSyncRequired(false);
                        /*
                         * We need to check for conflicts (does a team already exist that's been edited, or does the team not exist)
                         */
                        RTeam temp = l.loadTeam(activeEvent.getID(), checkout.getTeam().getID());
                        if(temp == null) checkout.setConflictType("not-found");
                        else if(temp.getLastEdit() > 0) checkout.setConflictType("edited");
                        else {
                            l.saveCheckoutConflict(checkout);
                        }

                        // no conflicts, merge automatically
                        if(checkout.getConflictType() == null || checkout.getConflictType().equals("")) {
                            for(int j = 0; j < temp.getTabs().size(); j++) {
                                if(temp.getTabs().get(j).getTitle().equals(checkout.getTeam().getTabs().get(0).getTitle())) {
                                    for(int k = 0; k < checkout.getTeam().getTabs().size(); k++) {
                                        temp.getTabs().set(j, checkout.getTeam().getTabs().get(k));
                                    }
                                    l.saveTeam(temp, activeEvent.getID());
                                    // save the checkout in the merge history
                                    checkout.setMergedTime(System.currentTimeMillis());
                                    l.saveCheckout(checkout);
                                    Log.d("RBS", "Merged checkout "+checkout.getTeam().getName());
                                    break;
                                }
                            }
                        }

                    }
                    // notify the user here
                } catch(Exception e) {
                    Log.d("RBS", "Error checking for completed checkouts. Error: "+e.getMessage());
                }

            }
        }

        private boolean isAppOnForeground(Context context) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses == null) {
                return false;
            }
            final String packageName = context.getPackageName();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

    }
}
