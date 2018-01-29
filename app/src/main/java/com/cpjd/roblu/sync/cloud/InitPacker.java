package com.cpjd.roblu.sync.cloud;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.util.Log;

import com.cpjd.http.Request;
import com.cpjd.listeners.RawResponseListener;
import com.cpjd.requests.CloudCheckoutRequest;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.ui.settings.customPreferences.RUICheckPreference;
import com.cpjd.roblu.utils.HandoffStatus;

import org.codehaus.jackson.map.ObjectMapper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import lombok.Setter;

/**
 * The initial passive push that pushes around 500
 * checkouts to the cloud! doInBackground() returns
 * true if the push & upload was successful
 *
 * We need to generate 1 checkout model per match, per team. So there will be
 * a lot of checkouts.
 *
 * Make sure that teams exist before this is run!
 *
 * @version 2
 * @since 3.5.9
 * @author Will Davies
 */
public class InitPacker extends AsyncTask<Void, Integer, Boolean> {

    private WeakReference<ProgressDialog> progressDialogWeakReference;
    private WeakReference<IO> ioWeakReference;
    private WeakReference<RUICheckPreference> ruiCheckPreferenceWeakReference;
    private int eventID;

    @Setter
    private StatusListener listener;

    public interface StatusListener {
        void statusUpdate(String message);
    }

    public InitPacker(ProgressDialog progressDialog, RUICheckPreference ruiCheckPreference, IO io, int eventID) {
        this.progressDialogWeakReference = new WeakReference<>(progressDialog);
        this.ruiCheckPreferenceWeakReference = new WeakReference<>(ruiCheckPreference);
        this.ioWeakReference = new WeakReference<>(io);
        this.eventID = eventID;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        progressDialogWeakReference.get().setProgress(progress[0]);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        /*
         * Make sure this thread has network permissions
         */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);


        Log.d("RBS", "Executing InitPacker task...");

        IO io = ioWeakReference.get();
        RSettings settings = io.loadSettings();
        Request r = new Request(settings.getServerIP());

        if(!r.ping()) {
            listener.statusUpdate("It appears as though the server is offline. Try again later.");
            return false;
        }

        /*
         * Load all teams from the event, also make sure that that the teams are verified
         */
        REvent event = io.loadEvent(eventID);
        RForm form = io.loadForm(eventID);
        RTeam[] teams = io.loadTeams(eventID);
        if(event == null || form == null || teams == null || teams.length == 0) {
            Log.d("RBS", "Not enough data to warrant an event upload.");
            listener.statusUpdate("This event doesn't contain any teams or sufficient data to upload to the server. Create some teams!");
            return false;
        }

        double inc = 0;
        double totalProgress = teams.length * 3;

        /*
         * Verify everything
         */
        for(RTeam team : teams) {
            team.verify(form);
            io.saveTeam(eventID, team);
            inc++;
            publishProgress((int)(inc / totalProgress * 100.0));
        }

        /*
         * Start packaging
         * Important note: We have to clone each team so that they don't have to be re-loaded
         */
        ArrayList<RCheckout> checkouts = new ArrayList<>();
        int id = 0;
        // Package PIT & Predictions checkouts first
        for(RTeam team : teams) {
            RTeam temp = team.clone();
            temp.removeAllTabsButPIT();
            RCheckout newCheckout = new RCheckout(temp);
            newCheckout.setID(id);
            newCheckout.setStatus(HandoffStatus.AVAILABLE);
            checkouts.add(newCheckout);
            id++;
            inc++;
            publishProgress((int)(inc / totalProgress * 100.0));
        }
        // Package matches checkouts

        /*
         * Next, add an assignment for every match, for every team
         */
        for(RTeam team : teams) {
            inc++;
            publishProgress((int)(inc / totalProgress * 100.0));
            if(team.getTabs() == null || team.getTabs().size() == 0) continue;
            for(int i = 2; i < team.getTabs().size(); i++) {
                RTeam temp = team.clone();
                temp.setPage(0);
                temp.removeAllTabsBut(i);
                RCheckout check = new RCheckout(temp);
                check.setID(id);
                check.setStatus(HandoffStatus.AVAILABLE);
                checkouts.add(check);
                id++;
            }
        }

        // save the checkouts locally
        for(RCheckout checkout : checkouts) {
            io.saveCheckout(checkout);
        }

        /*
         * Convert into JSON and upload
         */
        ObjectMapper mapper = new ObjectMapper();
        try {
            // serialization all the checkouts and pack them in an json array, this will be processed by the server
            String serializedCheckouts = mapper.writeValueAsString(checkouts);
            String serializedForm = mapper.writeValueAsString(form);
            String serializedUI = mapper.writeValueAsString(settings.getRui());
            String eventName = event.getName();
            if(eventName == null) eventName = "";

            r.setListener(new RawResponseListener() {
                @Override
                public void messageReceived(String s) {
                    Log.d("RBS", "MSG: "+s);
                }
            });
            Request.OUTPUT_RAW_RESPONSES = true;
            CloudCheckoutRequest ccr = new CloudCheckoutRequest(r, settings.getCode());
            Log.d("RBS", "Initializing init packer upload...");
            boolean success = ccr.init(settings.getTeamNumber(), eventName, serializedForm, serializedUI, serializedCheckouts);
            Log.d("RBS", "Output: "+serializedCheckouts);

            /*
             * Disable all other events with cloud syncing enabled
             */
            if(success) {
                REvent[] events = io.loadEvents();
                for(int i = 0; events != null && i < events.length; i++) {
                    events[i].setCloudEnabled(events[i].getID() == eventID);
                    io.saveEvent(events[i]);
                }
            }

            return success;

        } catch(Exception e) {
            Log.d("RBS", "An error occurred in InitPacker: "+e.getMessage());
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean params) {
        Log.d("RBS", "InitPacker successful: "+params);
        if(params) listener.statusUpdate("Event successfully uploaded. It will appear on all devices in a few moments.");
        ruiCheckPreferenceWeakReference.get().setChecked(params);
        progressDialogWeakReference.get().dismiss();
    }


}


