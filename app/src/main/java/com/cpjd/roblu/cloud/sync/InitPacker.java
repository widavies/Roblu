package com.cpjd.roblu.cloud.sync;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import com.cpjd.roblu.R;
import com.cpjd.roblu.cloud.api.CloudRequest;
import com.cpjd.roblu.forms.elements.EGallery;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.utils.Text;

import org.codehaus.jackson.map.ObjectMapper;

import java.util.ArrayList;

/**
 * The initial passive push that pushes around 500
 * checkouts to the cloud! doInBackground() returns
 * true if the push & upload was successful
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

        // loading
        RForm form = l.loadForm(eventID);
        RTeam[] teams = l.getTeams(eventID);
        if(teams == null || teams.length == 0) return false;

        form.setModified(true); // form needs to be synced the first time
        l.saveForm(form, eventID);

        ArrayList<RCheckout> checkouts = new ArrayList<>();

        // Verify everything
        for(RTeam team : teams) {
            team.verify(form);
            l.saveTeam(team, eventID);
        }
        int id = 0;
        // Create pit checkouts first
        for(RTeam team : teams) {
            if(team.getName().startsWith("Team RUSH")) {
                Log.d("RBS", "There are "+((EGallery)team.getTabs().get(0).getElements().get(3)).getImages().size()+" images");
            }

            RTeam temp = team.duplicate();
            if(temp.getName().startsWith("Team RUSH")) {
                Log.d("RBS", "Thasdfasdfere are "+((EGallery)temp.getTabs().get(0).getElements().get(3)).getImages().size()+" images");
            }
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

        /*
         * Convert into JSON and upload
         */
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(checkouts);
            String eventName = l.getEvent(eventID).getName();
            if(eventName.equals("null")) eventName = "nnull";

            new CloudRequest(l.loadSettings().getAuth(), l.loadSettings().getTeamCode()).initPushCheckouts(eventName, json);
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
    @Override
    protected void onPostExecute(Boolean result) {
        d.dismiss();
        Text.showSnackbar(activity.findViewById(R.id.event_settings), activity, "Event successfully synced", false, new Loader(activity).loadSettings().getRui().getPrimaryColor());
    }



}
