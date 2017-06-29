package com.cpjd.roblu.cloud.sync;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;

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
        for(int i = 0; i < teams.length; i++) {
            teams[i].verify(form);
            l.saveTeam(teams[i], eventID);
        }

        // Create pit checkouts first
        for(RTeam team : teams) {
            RTeam temp = team.duplicate();
            temp.removeAllTabsButPIT();
            checkouts.add(new RCheckout(eventID, temp));
        }

        /*
         * Next, add an assignment for every match, for every team
         */
        for(RTeam team : teams) {
            if(team.getTabs() == null || team.getTabs().size() == 0) continue;
            for(int i = 2; i < team.getTabs().size(); i++) {
                RTeam temp = team.duplicate();
                temp.removeAllTabsBut(i);
                checkouts.add(new RCheckout(eventID, temp));
            }
        }

        /*
         * Process images
         */

        // begin uploading
        Log.i("[*] ", "There are "+checkouts.size());
        for(RCheckout checkout : checkouts) {
            Log.i("[*] ",checkout.getTeam().getName()+" tab: "+ checkout.getTeam().getTabs().get(0).getTitle());
        }

        return true;
    }



}
