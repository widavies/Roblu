package com.cpjd.roblu.sync.bluetooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.notifications.Notify;
import com.cpjd.roblu.utils.HandoffStatus;
import com.cpjd.roblu.utils.Utils;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Manages a Bluetooth connection with a client device and data transfer over it.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class BTServer extends Thread implements Bluetooth.BluetoothListener {

    /**
     * Provides access to a context reference for accessing the file system
     */
    private Bluetooth bluetooth;

    /**
     * Used for deserializing and serializing objects to and from strings
     */
    private ObjectMapper mapper = new ObjectMapper().configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private REvent event;

    private ProgressDialog pd;

    /**
     * Creates a BTServer object for syncing to a Bluetooth device
     */
    public BTServer(ProgressDialog pd, Bluetooth bluetooth) {
        this.pd = pd;
        this.bluetooth = bluetooth;
        this.bluetooth.setListener(this);
    }

    /**
     * Starts the sync task
     */
    @Override
    public void run() {
        /*
         * Load the active Bluetooth event
         */
        IO io = new IO(bluetooth.getActivity());

        REvent[] events = io.loadEvents();

        for(REvent event : events) {
            if(event.isBluetoothEnabled()) {
                this.event = event;
            }
        }
        if(bluetooth.isEnabled()) {
            bluetooth.startServer();
        } else bluetooth.enable();
    }

    @Override
    public void deviceDiscovered(BluetoothDevice device) {

    }

    @Override
    public void messageReceived(String header, String message) {
        IO io = new IO(bluetooth.getActivity());

        if(header.equals("isActive")) {
            bluetooth.send("ACTIVE", String.valueOf(event == null));
        }
        if(event == null) {
            return;
        }

        switch(header) {
            case "SCOUTING_DATA":
                // Process scouting data
                try {
                    RForm form = io.loadForm(event.getID());

                    JSONParser parser = new JSONParser();
                    JSONArray array = (JSONArray) parser.parse(message);
                    for(int i = 0; i < array.size(); i++) {
                        // Deserialize the checkout
                        RCheckout checkout = mapper.readValue(array.get(i).toString(), RCheckout.class);

                        // Make sure to verify the checkout's team
                        checkout.getTeam().verify(form);

                    /*
                    * BEGIN MERGING
                    * -Let's check for possible conflicts
                    */
                        RTeam team = io.loadTeam(event.getID(), checkout.getTeam().getID());

                        // The team doesn't exist locally, so create it anew
                        if(team == null) {
                            RTeam newTeam = new RTeam(checkout.getTeam().getName(), checkout.getTeam().getNumber(), checkout.getTeam().getID());
                            newTeam.verify(form);

                            if(checkout.getTeam().getTabs().size() > 1) { // this means the downloaded team was a PIT tab, so override the new team's tabs
                                newTeam.setTabs(checkout.getTeam().getTabs());
                            } else { // otherwise just add them
                                newTeam.addTab(checkout.getTeam().getTabs().get(0));
                            }
                        }
                        // Data already exists, so do a 'smart' merge
                        else {
                            team.verify(form);

                            for(RTab downloadedTab : checkout.getTeam().getTabs()) {
                                boolean matchLocated = false;
                                for(RTab localTab : team.getTabs()) {
                                    localTab.setWon(downloadedTab.isWon());

                                    // Found the match, start merging
                                    if(localTab.getTitle().equalsIgnoreCase(downloadedTab.getTitle())) {
                                /*
                                 * Copy over the edit tabs
                                 */
                                        if(downloadedTab.getEdits() != null) localTab.setEdits(downloadedTab.getEdits());

                                        for(RMetric downloadedMetric : downloadedTab.getMetrics()) {
                                            for(RMetric localMetric : localTab.getMetrics()) {
                                                // Found the metric, determine if a merge needs to occur
                                                if(downloadedMetric.getID() == localMetric.getID()) {
                                            /*
                                             * We have to deal with one special case scenario - the gallery.
                                             * The gallery should never be overrided, just added to
                                             */
                                                    if(downloadedMetric instanceof RGallery && localMetric instanceof RGallery && ((RGallery) localMetric).getImages() != null && ((RGallery) downloadedMetric).getImages() != null) {
                                                        ((RGallery) localMetric).getImages().addAll(((RGallery) downloadedMetric).getImages());
                                                    }
                                                    // If the local metric is already edited, keep whichever data is newest
                                                    if(localMetric.isModified()) {
                                                        if(checkout.getTeam().getLastEdit() >= team.getLastEdit()) {
                                                            int replaceIndex = localTab.getMetrics().indexOf(localMetric);
                                                            localTab.getMetrics().set(replaceIndex, downloadedMetric);
                                                        }
                                                    }
                                                    // Otherwise, just do a straight override
                                                    else {
                                                        int replaceIndex = localTab.getMetrics().indexOf(localMetric);
                                                        localTab.getMetrics().set(replaceIndex, downloadedMetric);
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                        matchLocated = true;
                                        break;
                                    }
                                }
                                if(!matchLocated) {
                                    // Add as a new match if a merge wasn't performed
                                    team.addTab(checkout.getTeam().getTabs().get(0));
                                    Collections.sort(team.getTabs());
                                }
                            }

                        }

                        if(checkout.getTeam().getLastEdit() > team.getLastEdit()) team.setLastEdit(checkout.getTeam().getLastEdit());
                        io.saveTeam(event.getID(), team);

                        Log.d("RBS", "Bluetooth: Merged team: " + checkout.getTeam().getName());

                        Notify.notifyMerged(bluetooth.getActivity(), event.getID(), checkout);

                        // Notify the TeamViewer in case Roblu Master is viewing the data that was just modified
                        Utils.requestTeamViewerRefresh(bluetooth.getActivity(), team.getName());

                        Utils.requestUIRefresh(bluetooth.getActivity());
                    }
                } catch(Exception e) {
                    Log.d("RBS", "Failed to process checkouts received over Bluetooth.");
                }
                break;
            case "requestForm":
                try {
                    bluetooth.send("FORM", mapper.writeValueAsString(io.loadForm(event.getID())));
                } catch(Exception e) {
                    Log.d("RBS", "Failed to parse form as JSON.");
                }
                break;
            case "requestUI":
                try {
                    bluetooth.send("UI", mapper.writeValueAsString(io.loadSettings().getRui()));
                } catch(Exception e) {
                    Log.d("RBS", "Failed to parse form as JSON.");
                }
                break;
            case "requestCheckouts":
                // Get the timestamp
                long time = Long.parseLong(message.split(":")[1]);

                // Package all the checkouts locally
                RTeam[] teams = io.loadTeams(event.getID());
                RForm form = io.loadForm(event.getID());
            /*
             * Start packaging
             * Important note: We have to clone each team so that they don't have to be re-loaded
             */

            /*
             * Verify everything
             */
                for(RTeam team : teams) {
                    team.verify(form);
                    io.saveTeam(event.getID(), team);
                    // Remove all these, since the scouter won't use them
                    team.setImage(null);
                    team.setTbaInfo(null);
                    team.setWebsite(null);
                }
                ArrayList<RCheckout> checkouts = new ArrayList<>();
                int id = 0;
                // Package PIT & Predictions checkouts first
                for(RTeam team : teams) {
                    RTeam temp = team.clone();
                    temp.removeAllTabsButPIT();
                    RCheckout newCheckout = new RCheckout(temp);
                    newCheckout.setID(id);
                    newCheckout.setStatus(HandoffStatus.AVAILABLE);

                    if(newCheckout.getTeam().getLastEdit() >= time) checkouts.add(newCheckout);
                    id++;
                }
                // Package matches checkouts

            /*
             * Next, add an assignment for every match, for every team
             */
                for(RTeam team : teams) {
                    if(team.getTabs() == null || team.getTabs().size() == 0) continue;
                    for(int i = 2; i < team.getTabs().size(); i++) {
                        RTeam temp = team.clone();
                        temp.setPage(0);
                        temp.removeAllTabsBut(i);
                        RCheckout check = new RCheckout(temp);
                        check.setID(id);
                        check.setStatus(HandoffStatus.AVAILABLE);
                        if(check.getTeam().getLastEdit() >= time) checkouts.add(check);
                        id++;
                    }
                }

                try {
                    Log.d("RBS", "Generated "+checkouts.size()+" checkouts. Here's what is should look like: "+mapper.writeValueAsString(checkouts));

                    bluetooth.send("CHECKOUTS", mapper.writeValueAsString(checkouts));
                } catch(Exception e) {
                    Log.d("RBS", "Failed to map checkouts to Bluetooth output stream.");
                }
                break;
            case "requestNumber":
                bluetooth.send("NUMBER", String.valueOf(io.loadSettings().getTeamNumber()));
                break;
            case "requestEventName":
                bluetooth.send("EVENT_NAME", event.getName());
                break;
            case "DONE":
                bluetooth.send("DONE", "noParams");

                bluetooth.disconnect();
                if(bluetooth.isEnabled()) {
                    bluetooth.startServer();
                } else bluetooth.enable();
                break;
        }
    }

    @Override
    public void deviceConnected(final BluetoothDevice device) {
        Log.d("RBS", "Connected to device: "+device.getName());

        bluetooth.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pd.setMessage("Connected to device: "+device.getName()+". Syncing...");
            }
        });
    }

    @Override
    public void deviceDisconnected(BluetoothDevice device, String reason) {

    }

    @Override
    public void errorOccurred(String message) {
        Log.d("RBS", "Error occurred: "+message);
    }

    @Override
    public void stateChanged(int state) {
        if(state == BluetoothAdapter.STATE_ON) {
            bluetooth.startServer();
        }
    }

    @Override
    public void discoveryStopped() {

    }
}
