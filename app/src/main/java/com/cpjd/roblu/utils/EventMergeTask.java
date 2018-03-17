package com.cpjd.roblu.utils;

import android.util.Log;

import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.models.metrics.RMetric;

import java.util.ArrayList;

/**
 * Event merge tasks will allow the user to merge two events into one event.
 *
 * @version 1
 * @since 4.4.3
 * @author Will Davies
 */
public class EventMergeTask extends Thread {

    private IO io;

    /**
     * The eventID of the event that data is being merged into
     */
    private int localEventID;

    /**
     * The eventID of the event to merge into this one
     */
    private int targetEventID;

    private EventMergeListener listener;

    public interface EventMergeListener {
        void error();
        void success();
    }

    public EventMergeTask(IO io, int localEventID, int targetEventID, EventMergeListener listener) {
        this.io = io;
        this.localEventID = localEventID;
        this.targetEventID = targetEventID;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            // Load teams for each event
            RTeam[] localTeams = io.loadTeams(localEventID);
            RTeam[] targetTeams = io.loadTeams(targetEventID);
            RForm localForm = io.loadForm(localEventID);
            RForm targetForm = io.loadForm(targetEventID);

            for(RTeam team : localTeams) team.verify(localForm);
            for(RTeam team : targetTeams) team.verify(targetForm);

            // Start merging
            for(RTeam local : localTeams) {
                // Find the team
                for(RTeam target : targetTeams) {
                    if(local.getNumber() == target.getNumber()) {
                        for(RTab localTab : local.getTabs()) {
                            // Find tab
                            for(RTab targetTab : target.getTabs()) {
                                if(targetTab.getTitle().equalsIgnoreCase(localTab.getTitle())) { // Match found, start merging metrics
                                    for(RMetric localMetric : localTab.getMetrics()) {
                                        // Find metric
                                        for(RMetric targetMetric : targetTab.getMetrics()) {
                                        /*
                                         * Okay, the forms might be different, so we need to run some checks before we override the localMetric:
                                         * -Same instanceof type (obviously)
                                         * -Same title (within reason, do a little trimming, and ignore caps)
                                         * -ID - ID should not be considered, they might not be equal
                                         *
                                         * If we find a good candidate for the metric, override the local. Conditions
                                         * -Target is not modified: do nothing
                                         * -Target is modified: overwrite if local not modified
                                         * -Both modified: Compare team last edit time stamp
                                         */
                                            if((localMetric.getClass().equals(targetMetric.getClass())) &&
                                                    localMetric.getTitle().toLowerCase().replaceAll(" ", "").equals(targetMetric.getTitle().toLowerCase().replaceAll(" ", ""))) {
                                                if(localMetric instanceof RGallery) {
                                                    // Just add the images
                                                    if(((RGallery) localMetric).getPictureIDs() == null) ((RGallery) localMetric).setPictureIDs(new ArrayList<Integer>());

                                                    if(((RGallery) localMetric).getImages() != null) {
                                                        // Add images to the current gallery
                                                        for(int i = 0; i < ((RGallery) targetMetric).getImages().size(); i++) {
                                                            ((RGallery) localMetric).getPictureIDs().add(io.savePicture(localEventID, ((RGallery) targetMetric).getImages().get(i)));
                                                        }
                                                    }
                                                    // Don't forget to clear the pictures from memory after they've been merged
                                                    ((RGallery) targetMetric).setImages(null);
                                                    local.setLastEdit(target.getLastEdit());
                                                }
                                                // Alright, looks like we can do the checks now
                                                else if(targetMetric.isModified() && !localMetric.isModified()) {
                                                    int tabIndex = local.getTabs().indexOf(localTab);
                                                    int metricIndex = local.getTabs().get(tabIndex).getMetrics().indexOf(localMetric);
                                                    local.getTabs().get(tabIndex).getMetrics().set(metricIndex, targetMetric);
                                                    local.setLastEdit(target.getLastEdit());
                                                } else if(targetMetric.isModified() && localMetric.isModified() && target.getLastEdit() > local.getLastEdit()) {
                                                    int tabIndex = local.getTabs().indexOf(localTab);
                                                    int metricIndex = local.getTabs().get(tabIndex).getMetrics().indexOf(localMetric);
                                                    local.getTabs().get(tabIndex).getMetrics().set(metricIndex, targetMetric);
                                                    local.setLastEdit(target.getLastEdit());
                                                }
                                                break;
                                            }
                                        }

                                    }

                                    break;
                                }
                            }

                        }
                        break;
                    }
                }

                io.saveTeam(localEventID, local);
            }
            listener.success();
        } catch(Exception e) {
            Log.d("RBS", "Error occurred in EventMergeTask: "+e.getMessage());

            listener.error();
        }
    }

}
