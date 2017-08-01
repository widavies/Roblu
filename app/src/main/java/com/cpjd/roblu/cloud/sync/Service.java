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

import com.cpjd.roblu.cloud.api.CloudRequest;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.utils.Text;

import org.codehaus.jackson.map.ObjectMapper;

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
        //Toast.makeText(this, "onStartCommand", Toast.LENGTH_SHORT).show();
        //TODO do something useful
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

            // Add your cpu-blocking activity here
            while(true) {
                // sleep a bit
                try {
                    if(isAppOnForeground(getApplicationContext())) Thread.sleep(5000);
                    else Thread.sleep(300000);
                } catch(Exception e) {}

                // first, check if we have an internet connection, if we don't, the background service is useless
                if(!Text.hasInternetConnection(getApplicationContext())) {
                    continue;
                }
                cr = new CloudRequest(l.loadSettings().getAuth(), l.loadSettings().getTeamCode());

                // check if the UI needs to be uploaded
                System.out.println("here");
                RSettings settings = l.loadSettings();
                RUI rui = settings.getRui();
                if(rui != null && rui.isModified()) {
                    try {
                        System.out.println("[Roblu Background Service] Pushed UI with return message: "+cr.pushUI(mapper.writeValueAsString(rui)));
                        rui.setModified(false);
                        l.saveSettings(settings);
                    } catch(Exception e) {
                        System.out.println("error: "+e.getMessage());
                    }
                } else {
                    System.out.println("RUI is null or not modified"+ rui == null);
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
                        System.out.println("[Roblu Background Service] Successfully pushed form: "+
                                cr.pushForm(mapper.writeValueAsString(form)));
                        form.setModified(false);
                        l.saveForm(form, activeEvent.getID());
                    } catch(Exception e) {
                        System.out.println("[Roblu Background Service] Failed to push form: "+e);
                    }
                }

                // check if their are any checkouts in RCheckouts
                System.out.println("[Roblu Background Service] Pulling checkouts...");
                try {
                    System.out.println(cr.pullCheckouts());
                    // notify the user here
                    System.out.println("[Roblu Background Service] Successfully pulled checkouts");
                } catch(Exception e) {
                    System.out.println("[Roblu Background Service] Failed to pull checkouts");
                }



                try {
                  //  Notify.notify(getApplicationContext(), "IS Roblu Running? "+isAppOnForeground(getApplicationContext()), "Time: "+System.currentTimeMillis());
                } catch(Exception e) {}
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
