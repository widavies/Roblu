package com.cpjd.roblu.sync.cloud;

import android.os.AsyncTask;
import android.os.StrictMode;
import android.util.Log;

import com.cpjd.http.Request;
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

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import lombok.Setter;

/**
 * EventDepacker basically does the opposite of what `InitPacker` does.
 * It will download the currently active event from the Roblu Cloud server so that multiple devices
 * can act a Master apps.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class EventDepacker extends AsyncTask<Void, Void, Void> {

    private IO io;

    @Setter
    private EventDepackerListener listener;

    public interface EventDepackerListener {
        void errorOccurred(String message);
        void success(REvent event);
    }

    public EventDepacker(IO io) {
        this.io = io;
    }

    @Override
    protected Void doInBackground(Void... params) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        Log.d("RBS", "Executing EventDepacker task...");

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        RSettings settings = io.loadSettings();
        RCloudSettings cloudSettings = io.loadCloudSettings();
        cloudSettings.setLastCheckoutSync(0);
        cloudSettings.setLastTeamSync(0);
        cloudSettings.setPurgeRequested(false);
        io.saveCloudSettings(cloudSettings);
        Request r = new Request(settings.getServerIP());
        CloudTeamRequest ctr = new CloudTeamRequest(r, settings.getCode());
        CloudCheckoutRequest ccr = new CloudCheckoutRequest(r, settings.getCode());

        if(settings.getCode() == null || settings.getCode().equals("")) {
            if(listener != null) listener.errorOccurred("No team code found in settings. Unable to import event.");
            return null;
        }

        // Ping
        if(!r.ping()) {
            if(listener != null) listener.errorOccurred("It appears as though the server is offline. Try again later.");
            return null;
        }

        if(!ctr.isActive()) {
            if(listener != null) listener.errorOccurred("No event found on Roblu Cloud.");
            return null;
        }

        if(settings.getCode() == null || settings.getCode().equals("")) {
            if(listener != null) listener.errorOccurred("Please enter a team code to download events.");
            return null;
        }

        /*
         * Download everything
         */
        CloudTeam team = ctr.getTeam(-1);
        String[] pulledCheckouts = ccr.pullCheckouts(0);

        Log.d("RBS", "Pulled "+pulledCheckouts.length+" checkouts");

        // Get a new event ID
        REvent event = new REvent(io.getNewEventID(), team.getActiveEventName());
        event.setKey(team.getTbaKey());
        event.setID(io.getNewEventID());
        event.setCloudEnabled(true);
        io.saveEvent(event);

        /*
         * Un-package team information
         */
        try {
            settings.setTeamNumber((int)team.getNumber());
            RForm form = mapper.readValue(team.getForm(), RForm.class);
            io.saveForm(event.getID(), form);
            settings.setRui(mapper.readValue(team.getUi(), RUI.class));
        } catch(IOException e) {
            Log.d("RBS", "Failed to download event");
            listener.errorOccurred("Failed to import Roblu Cloud event.");
            return null;
        } finally {
            io.saveSettings(settings);
        }

        /*
         * Un-package checkouts into a teams array
         */
        ArrayList<RCheckout> checkouts = new ArrayList<>();
        try {
            for(String s : pulledCheckouts) checkouts.add(mapper.readValue(s, RCheckout.class));
        } catch(IOException e) {
            Log.d("RBS", "Failed to de-package checkouts.");
            listener.errorOccurred("Failed to import Roblu Cloud event.");
            return null;
        }

        /*
         * Start sorting the checkouts into teams
         */
        ArrayList<RTeam> teams = new ArrayList<>();
        for(RCheckout checkout : checkouts) {
            // First, check if the team has already been created
            boolean found = false;
            for(RTeam t : teams) {
                if(t.getID() == checkout.getTeam().getID()) {
                    // Add the checkout information to the team
                    t.getTabs().addAll(checkout.getTeam().getTabs());
                    found = true;
                    break;
                }
            }

            // If not found, create a new team
            if(!found) {
                RTeam newTeam = new RTeam(checkout.getTeam().getName(), checkout.getTeam().getNumber(), checkout.getTeam().getID());
                newTeam.setTabs(new ArrayList<RTab>());
                newTeam.getTabs().addAll(checkout.getTeam().getTabs());
                teams.add(newTeam);
            }
        }

        Log.d("RBS", "Created "+teams.size()+" teams");

        /*
         * Save teams
         * -Teams don't need to be verified since the form has also been pulled from the server
         */
        for(RTeam t : teams) {
            Collections.sort(t.getTabs());
            io.saveTeam(event.getID(), t);
        }

        // Remove all the other synced events
        REvent[] events = io.loadEvents();
        for(int i = 0; events != null && i < events.length; i++) {
            events[i].setCloudEnabled(events[i].getID() == event.getID());
            io.saveEvent(events[i]);
        }

        if(listener != null) {
            listener.success(event);
        }
        return null;
    }
}
