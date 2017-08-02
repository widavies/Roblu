package com.cpjd.roblu.cloud.sync;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.cpjd.roblu.cloud.api.CloudRequest;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;

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

    private final Context context;
    private final long eventID;

    public InitPacker(Context context, long eventID) {
        this.context = context;
        this.eventID = eventID;
    }

    protected Boolean doInBackground(Void... params) {
        Loader l = new Loader(context);

        // loading
        RForm form = l.loadForm(eventID);
        RTeam[] teams = l.getTeams(eventID);
        if(teams == null || teams.length == 0) return false;

        ArrayList<RCheckout> checkouts = new ArrayList<>();

        // Verify everything
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

        /*
         * Convert into JSON and upload
         */
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(checkouts);

            System.out.println(new CloudRequest(l.loadSettings().getAuth(), l.loadSettings().getTeamCode()).initPushCheckouts(l.getEvent(eventID).getName(), json));
//            RCheckout[] imported = mapper.readValue(json, TypeFactory.defaultInstance().constructArrayType(RCheckout.class));

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

        /*
         * Clear tasks in background service
         */

        return true;
    }
    @Override
    protected void onPostExecute(Boolean result) {
        Toast.makeText(context, "Successfully uploaded checkouts", Toast.LENGTH_LONG).show();
    }



}
