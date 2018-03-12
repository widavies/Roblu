package com.cpjd.roblu.sync.cloud;

import android.os.StrictMode;
import android.util.Log;

import com.cpjd.http.Request;
import com.cpjd.models.CloudCheckout;
import com.cpjd.models.CloudTeam;
import com.cpjd.requests.CloudCheckoutRequest;
import com.cpjd.requests.CloudTeamRequest;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RSyncSettings;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.models.metrics.RMetric;

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
public class EventDepacker extends Thread {

    private IO io;

    private int teamNumber;

    @Setter
    private EventDepackerListener listener;

    public interface EventDepackerListener {
        void errorOccurred(String message);
        void success(REvent event);
    }

    public EventDepacker(IO io, int teamNumber) {
        this.io = io;
        this.teamNumber = teamNumber;
    }

    @Override
    public void run() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        Log.d("RBS", "Executing EventDepacker task...");

        ObjectMapper mapper = new ObjectMapper().configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        RSettings settings = io.loadSettings();
        RSyncSettings cloudSettings = io.loadCloudSettings();
        cloudSettings.setTeamSyncID(0);
        cloudSettings.getCheckoutSyncIDs().clear();
        cloudSettings.setPurgeRequested(false);
        cloudSettings.setPublicTeamNumber(teamNumber);
        io.saveCloudSettings(cloudSettings);
        Request r = new Request(settings.getServerIP());
        CloudTeamRequest ctr = new CloudTeamRequest(r, settings.getCode());

        if(teamNumber != -1) {
            ctr.setCode("");
            ctr.setTeamNumber(teamNumber);
        }
        CloudCheckoutRequest ccr = new CloudCheckoutRequest(r, settings.getCode());
        if(teamNumber != -1) {
            ccr.setTeamCode("");
            ccr.setTeamNumber(teamNumber);
        }

        if(teamNumber == -1 && (settings.getCode() == null || settings.getCode().equals(""))) {
            if(listener != null) listener.errorOccurred("No team code found in settings. Unable to import event.");
            return;
        }

        // Ping
        if(!r.ping()) {
            if(listener != null) listener.errorOccurred("It appears as though the server is offline. Try again later.");
            return;
        }

        if(!ctr.isActive()) {
            if(listener != null) listener.errorOccurred("No event found on Roblu Cloud.");
            return;
        }

        /*
         * Download everything
         *
         */
        CloudTeam team = ctr.getTeam(-1);
        REvent event;
        try {
            // Create a new event
            event = new REvent(io.getNewEventID(), team.getActiveEventName());
            event.setKey(team.getTbaKey());
            event.setID(io.getNewEventID());
            event.setCloudEnabled(true);
            io.saveEvent(event);

            settings.setTeamNumber((int)team.getNumber());
            settings.setRui(mapper.readValue(team.getUi(), RUI.class));
            io.saveSettings(settings);

            RForm form = mapper.readValue(team.getForm(), RForm.class);
            io.saveForm(event.getID(), form);

        } catch(Exception e) {
            Log.d("RBS", "Failed to download event");
            listener.errorOccurred("Failed to import Roblu Cloud event.");
            return;
        }
        /*
         * Un-package checkouts into a teams array
         */
        ArrayList<RCheckout> checkouts = new ArrayList<>();
        try {
            CloudCheckout[] pulledCheckouts = ccr.pullCheckouts(null, true);
            for(CloudCheckout s : pulledCheckouts) checkouts.add(mapper.readValue(s.getContent(), RCheckout.class));
        } catch(IOException e) {
            Log.d("RBS", "Failed to de-package checkouts.");
            listener.errorOccurred("Failed to import Roblu Cloud event.");
            return;
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
                t.setLastEdit(checkout.getTime());
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
         * Unpack images
         */
        for(RCheckout checkout : checkouts) {
            for(RTab tab : checkout.getTeam().getTabs()) {
                for(RMetric metric : tab.getMetrics()) {
                    if(metric instanceof RGallery) {
                        for(int i = 0; ((RGallery) metric).getImages() != null && i < ((RGallery) metric).getImages().size(); i++) {
                            int picID = io.savePicture(event.getID(), ((RGallery) metric).getImages().get(i));
                            if(picID != -1) {
                                ((RGallery) metric).setPictureIDs(new ArrayList<Integer>());
                                ((RGallery) metric).getPictureIDs().add(picID);
                            }
                        }
                        if(((RGallery) metric).getImages() != null) ((RGallery) metric).getImages().clear();
                    }
                }
            }
        }

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

        /*
         * Add default sync ids
         */
        for(RCheckout checkout : checkouts) {
            cloudSettings.getCheckoutSyncIDs().put(checkout.getID(), 0L);
        }

        io.saveCloudSettings(cloudSettings);

        if(listener != null) {
            listener.success(event);
        }
    }
}
