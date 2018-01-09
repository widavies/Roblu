package com.cpjd.roblu.sync.cloud.sync;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import com.cpjd.roblu.R;
import com.cpjd.roblu.sync.cloud.api.CloudRequest;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.utils.Utils;

import org.codehaus.jackson.map.ObjectMapper;

import java.util.ArrayList;

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
        Loader l = new Loader(activity);

        // Load all the teams from this event
        RForm form = l.loadForm(eventID);
        RTeam[] teams = l.getTeams(eventID);
        if(teams == null || teams.length == 0) {
            return false;
        }

        // Tell the background service that both the form and the UI need to be synced to the server
        form.setModified(true);
        RSettings settings = l.loadSettings();
        settings.getRui().setModified(true);
        l.saveSettings(settings);
        l.saveForm(form, eventID);

        ArrayList<RCheckout> checkouts = new ArrayList<>();
        // Verify everything - this keeps it in sync witht he form
        for(RTeam team : teams) {
            team.verify(form);
            l.saveTeam(team, eventID);
        }
        int id = 0;
        // Create pit checkouts first
        for(RTeam team : teams) {
            RTeam temp = team.duplicate();
            temp.removeAllTabsButPIT();
            RCheckout check = new RCheckout(temp);
            check.setID(id);
            check.setStatus("Available");
            checkouts.add(check);
            id++;
        }

        /*
         * Next, add an assignment for every match, for every team
         */
        for(RTeam team : teams) {
            if(team.getTabs() == null || team.getTabs().size() == 0) continue;
            for(int i = 2; i < team.getTabs().size(); i++) {
                RTeam temp = team.duplicate();
                temp.setPage(0);
                temp.removeAllTabsBut(i);
                RCheckout check = new RCheckout(temp);
                check.setID(id);
                check.setStatus("Available");
                checkouts.add(check);
                id++;
            }
        }

        // if no checkouts were generated, might as well stop this thread because we don't need to update anything
        if(checkouts.size() == 0) {
            return false;
        }

        /*
         * Convert into JSON and upload
         */
        ObjectMapper mapper = new ObjectMapper();
        try {
            // serialization all the checkouts and pack them in an json array, this will be processed by the server
            String json = mapper.writeValueAsString(checkouts);
            String eventName = l.getEvent(eventID).getName();
            if(eventName == null || eventName.equals("")) eventName = " ";
            else if(eventName.equals("null")) eventName = "null ";

            Log.d("RBS",new CloudRequest(l.loadSettings().getAuth(), l.loadSettings().getTeamCode(), Utils.getDeviceID(activity)).initPushCheckouts(eventName, json).toString());
        } catch(Exception e) {
            System.out.println("An error occured: "+e.getMessage());
            e.printStackTrace();
        }

        /*
         * Disable all other events with cloud syncing enabled
         */
        REvent[] events = l.getEvents();
        for(int i = 0; events != null && i < events.length; i++) {
            events[i].setCloudEnabled(events[i].getID() == eventID);
            l.saveEvent(events[i]);
        }

        /*
         * Clear conflicts & inbox here
         */
         l.clearCheckouts();

        return true;
    }
    @Override // Update UI with either success or fail message
    protected void onPostExecute(Boolean result) {
        d.dismiss();
        try {
            if(result) Utils.showSnackbar(activity.findViewById(R.id.event_settings), activity, "Event successfully synced", false, new Loader(activity).loadSettings().getRui().getPrimaryColor());
            else Utils.showSnackbar(activity.findViewById(R.id.event_settings), activity, "No match data found. Please create some before uploading.", true, 0);
        } catch(Exception e) {}
    }



}
