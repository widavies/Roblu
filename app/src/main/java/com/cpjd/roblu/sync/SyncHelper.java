package com.cpjd.roblu.sync;

import android.content.Context;
import android.util.Log;

import com.cpjd.models.CloudCheckout;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RSyncSettings;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

import lombok.Data;

/**
 * SyncHelper manages the packaging and depackaging of data received over one
 * of the three sync methods.
 *
 * @version 1
 * @since 4.3.5
 * @author Will Davies
 */
public class SyncHelper {

    private ObjectMapper mapper;
    private Context context;
    private REvent activeEvent;
    private RForm form;
    private IO io;

    private MODES mode;

    public enum MODES {
        NETWORK,BLUETOOTH,QR
    }

    public SyncHelper() {

    }

    public SyncHelper(Context context, REvent activeEvent, MODES mode) {
        this.context = context;
        this.activeEvent = activeEvent;
        this.mode = mode;
        io = new IO(context);
        form = io.loadForm(activeEvent.getID());
        mapper = new ObjectMapper().configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    public SyncHelper(IO io, REvent activeEvent, MODES mode) {
        this.activeEvent = activeEvent;
        this.mode = mode;
        this.io = io;
        form = io.loadForm(activeEvent.getID());
        mapper = new ObjectMapper().configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    /**
     * Packages a list of checkouts and converts them to a string
     * @param checkouts the checkouts to package
     * @throws Exception if no checkouts are received or a error occurred when serializing them
     * @return a string containing all checkout information
     */
    public String packCheckouts(ArrayList<RCheckout> checkouts) throws Exception {
        if(checkouts == null || checkouts.size() == 0) {
            throw new NullPointerException("No checkouts to package.");
        }

        if(activeEvent == null) {
            throw new NullPointerException("No active event found. Unable to package checkouts.");
        }

        /*
         * Pack images into the checkouts
         */
        for(RCheckout checkout : checkouts) {
            for(RTab tab : checkout.getTeam().getTabs()) {
                for(int i = 0; tab.getMetrics() != null && i < tab.getMetrics().size(); i++) {
                    if(!(tab.getMetrics().get(i) instanceof RGallery)) continue;

                    // Make sure the array is not null.
                    ((RGallery)tab.getMetrics().get(i)).setImages(new ArrayList<byte[]>());

                    for(int j = 0; ((RGallery)tab.getMetrics().get(i)).getPictureIDs() != null && j < ((RGallery)tab.getMetrics().get(i)).getPictureIDs().size(); j++) {
                        ((RGallery)tab.getMetrics().get(i)).getImages().add(io.loadPicture(activeEvent.getID(), ((RGallery)tab.getMetrics().get(i)).getPictureIDs().get(j)));
                    }
                }
            }
        }

        // Serialize the checkouts
        return mapper.writeValueAsString(checkouts);
    }

    /**
     * Deserialize and merges a list of checkouts into the active event
     * @param checkouts the array of checkouts to process
     */
    public void unpackCheckouts(CloudCheckout[] checkouts, RSyncSettings cloudSettings) {
        if(checkouts == null || checkouts.length == 0) {
            throw new NullPointerException("No checkouts to unpack.");
        }

        if(activeEvent == null) {
            throw new NullPointerException("No active event found. Unable to unpack checkouts.");
        }

        for(CloudCheckout serial : checkouts) {
            try {
                // Deserialize
                RCheckout checkout = mapper.readValue(serial.getContent(), RCheckout.class);

                // Merge the checkout
                mergeCheckout(checkout);

                // Send a notification if there's less than 6 checkouts
                if(checkouts.length < 6) Notify.notifyMerged(context, activeEvent.getID(), checkout);

                if(mode == MODES.NETWORK) {
                    // Update the sync IDs
                    cloudSettings.getCheckoutSyncIDs().put(checkout.getID(), serial.getSyncID());
                }

                else if(mode == MODES.BLUETOOTH) {
                    io.savePendingObject(checkout);
                }
            } catch(Exception e) {
                Log.d("RBS", "Failed to unpack checkout: "+serial);
            }
        }

        // Send a multi-notification instead of spamming the user if they received 6 or more checkouts at once
        if(checkouts.length >= 6) {
            Notify.notifyNoAction(context, "Merged scouting data", "Merged "+checkouts.length+" checkouts.");
        }

        // Request general UI refresh
        Utils.requestUIRefresh(context);
    }

    /**
     * Merges the checkout with the active event.
     * Creates new team / match if they don't already exist
     * @param checkout the checkout to merge
     */
    public void mergeCheckout(RCheckout checkout) {
        // Verify the target checkout, to make sure it's in sync with the local form
        checkout.getTeam().verify(form);

        // Check to see if there is a local team matching this checkout
        RTeam team = io.loadTeam(activeEvent.getID(), checkout.getTeam().getID());

        // The team was found, so do a merge
        if(team != null) {
            team.verify(form);
            team.setLastEdit(checkout.getTeam().getLastEdit());

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
                                    if(downloadedMetric instanceof RGallery && localMetric instanceof RGallery) {
                                        if(((RGallery) localMetric).getPictureIDs() == null) ((RGallery) localMetric).setPictureIDs(new ArrayList<Integer>());

                                        if(((RGallery) downloadedMetric).getImages() != null) {
                                            // Add images to the current gallery
                                            for(int i = 0; i < ((RGallery) downloadedMetric).getImages().size(); i++) {
                                                ((RGallery) localMetric).getPictureIDs().add(io.savePicture(activeEvent.getID(), ((RGallery) downloadedMetric).getImages().get(i)));
                                            }
                                        }
                                        // Don't forget to clear the pictures from memory after they've been merged
                                        ((RGallery) downloadedMetric).setImages(null);
                                        break;
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
        // The team was not found locally, create a new one
        else {
            team = new RTeam(checkout.getTeam().getName(), checkout.getTeam().getNumber(), checkout.getTeam().getID());
            team.setLastEdit(checkout.getTeam().getLastEdit());
            team.verify(form);

            if(checkout.getTeam().getTabs().size() > 1) { // this means the downloaded team was a PIT tab, so override the new team's tabs
                team.setTabs(checkout.getTeam().getTabs());
            } else { // otherwise just add them
                team.addTab(checkout.getTeam().getTabs().get(0));
            }
        }

        // Save the team
        io.saveTeam(activeEvent.getID(), team);

        // Request a UI refresh
        Utils.requestTeamViewerRefresh(context, team.getID());

        Log.d("RBS-Service", "Merged the team: "+checkout.getTeam().getName());
    }

    public ArrayList<RCheckout> generateCheckoutsFromEvent(RTeam[] teams, long time) {
        /*
         * Verify everything
         */
        for(RTeam team : teams) {
            team.verify(form);
            io.saveTeam(activeEvent.getID(), team);
            // Remove all these, since the scouter won't use them
            team.setImage(null);
            team.setTbaInfo(null);
            team.setWebsite(null);
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

            if(mode == MODES.BLUETOOTH && newCheckout.getTeam().getLastEdit() >= time) checkouts.add(newCheckout);
            else checkouts.add(newCheckout);
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

                if(mode == MODES.BLUETOOTH && check.getTeam().getLastEdit() >= time) checkouts.add(check);
                else checkouts.add(check);

                id++;
            }
        }

        Log.d("RBS", "Created: "+checkouts.size()+" checkouts");

        return checkouts;
    }

    public String packSyncIDs(LinkedHashMap<Integer, Long> checkoutSyncIDs) throws Exception {

        // This is how the current sync IDs will be packaged to the server
        @Data
        class CheckoutSyncID implements Serializable {
            private int checkoutID;
            private long syncID;

            private CheckoutSyncID(int checkoutID, long syncID) {
                this.checkoutID = checkoutID;
                this.syncID = syncID;
            }
        }

        ArrayList<CheckoutSyncID> checkoutSyncPacks = new ArrayList<>();
        for(Object key : checkoutSyncIDs.keySet()) {
            checkoutSyncPacks.add(new CheckoutSyncID(Integer.parseInt(key.toString()), checkoutSyncIDs.get(key)));
        }

        return mapper.writeValueAsString(checkoutSyncPacks);
    }

    public CloudCheckout[] convertStringSerialToCloudCheckouts(String[] serial) {
        if(serial == null || serial.length == 0) {
            throw new NullPointerException("No checkouts found to process");
        }

        CloudCheckout[] cloudCheckouts = new CloudCheckout[serial.length];
        for(int i = 0; i < serial.length; i++) cloudCheckouts[i] = new CloudCheckout(-1, serial[i]);
        return cloudCheckouts;
    }

}
