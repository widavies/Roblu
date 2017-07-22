package com.cpjd.roblu.cloud.sync;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;

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

        // Create pit checkouts first
        for(RTeam team : teams) {
            RTeam temp = team.duplicate();
            temp.removeAllTabsButPIT();
            checkouts.add(new RCheckout(temp));
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
                checkouts.add(new RCheckout(temp));
            }
        }

        /*
         * Process images
         */
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(checkouts);
            System.out.println("Exported: "+json);
            RCheckout[] imported = mapper.readValue(json, TypeFactory.defaultInstance().constructArrayType(RCheckout.class));
            System.out.println("Imported title: "+imported[0].getTeam().getTabs().get(0).getTitle());
            System.out.println("Imported size: "+imported.length);

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"wdavies973@gmail.com"});
            i.putExtra(Intent.EXTRA_SUBJECT, "Roblu Bug Report");
            i.putExtra(Intent.EXTRA_TEXT   , json);
            context.startActivity(Intent.createChooser(i, "yeet"));
        } catch(Exception e) {
            System.out.println("An error occured: "+e.getMessage());
            e.printStackTrace();
        }


        // begin uploading
        Log.i("[*] ", "There are "+checkouts.size());
        for(RCheckout checkout : checkouts) {
            Log.i("[*] ",checkout.getTeam().getName()+" tab: "+ checkout.getTeam().getTabs().get(0).getTitle());
        }

        return true;
    }



}
