package com.cpjd.roblu.tba;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;

import com.cpjd.models.Event;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.HandoffStatus;
import com.cpjd.roblu.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

/**
 * UnpackTBAEvent takes the data within an Event model (teams, matches, wins, etc.) and converts
 * it into the Roblu format. This task will save a whole bunch of RTeams to the file system,
 * so the EventEditor really isn't dependent on it.
 *
 * Note: An AsyncTask will continue to run even if the original activity is destroyed
 */
public class UnpackTBAEvent extends AsyncTask<Void, Void, Void> {

    private Event event;
    private int eventID;

    /**
     * If true, UnpackTBAEvent will act as if the event already exists and attempt
     * to merge only new data
     */
    private boolean merge;

    private WeakReference<Activity> activityWeakReference;
    private WeakReference<ProgressDialog> progressDialogWeakReference;

    public UnpackTBAEvent(Event e, int eventID, boolean merge, Activity activity, ProgressDialog d) {
        this.eventID = eventID;
        this.event = e;
        this.activityWeakReference = new WeakReference<>(activity);
        this.progressDialogWeakReference = new WeakReference<>(d);
        this.merge = merge;
    }

    protected Void doInBackground(Void... params) {
        /*
         * No teams were contained within the event, so exit, nothing here is relevant
         * to a TBA event that doesn't contain any team models
         */
        if(event.teams == null || event.teams.length == 0) return null;

        /*
         * Create an array of team models from the ones contained in the event
         */
        ArrayList<RTeam> teams = new ArrayList<>();
        for(int i = 0; i < event.teams.length; i++) {
            // i can be used as the ID because we are creating a fresh event, io.getNewTeamID is irrelevant
            teams.add(new RTeam(event.teams[i].nickname, (int) event.teams[i].team_number, i));
        }

        /*
         * Sort the matches in the event
         */
        Collections.sort(Arrays.asList(event.matches));

        /*
         * Add the matches to the respective team models
         */
        IO io = new IO(activityWeakReference.get());
        RForm form = io.loadForm(eventID);

        // For merge mode
        RTeam[] localTeams = null;
        if(merge) localTeams = io.loadTeams(eventID);

        int result;
        for(RTeam t : teams) {
            t.verify(form);
            for(int j = 0; j < event.matches.length; j++) {
                result = event.matches[j].doesMatchContainTeam(t.getNumber());
                if(result > 0) {
                    String name = "Match";
                    // process the correct match name
                    switch(event.matches[j].comp_level) {
                        case "qm":
                            name = "Quals " + event.matches[j].match_number;
                            break;
                        case "qf":
                            name = "Quarters " + event.matches[j].set_number + " Match " + event.matches[j].match_number;
                            break;
                        case "sf":
                            name = "Semis " + event.matches[j].set_number + " Match " + event.matches[j].match_number;
                            break;
                        case "f":
                            name = "Finals " + event.matches[j].match_number;
                    }
                    boolean isRed = result == com.cpjd.main.Constants.CONTAINS_TEAM_RED;
                    // add the match to the team, make sure to multiple the Event model's matches times by 1000 (seconds to milliseconds, Roblu works with milliseconds!)
                    RTab tab = new RTab(t.getNumber(), name, Utils.duplicateRMetricArray(form.getMatch()), isRed, event.matches[j].isOnWinningAlliance(t.getNumber()), event.matches[j].time * 1000);
                    // set the match position, if possible
                    tab.setAlliancePosition(event.matches[j].getTeamPosition(t.getNumber()));
                    t.addTab(tab);
                }
            }

            /*
             * This is where the merge decision comes into play
             */
            if(!merge) {
                io.saveTeam(eventID, t);
            } else {
                REvent localEvent = io.loadEvent(eventID);

                /*
                 * User wants to merge with an event, we need to do a team merge.
                 * This involves two things:
                 * 1) If team doesn't exist locally, just write it new (check for existence with name + number equivalence)
                 * 2) If a team does exist, add any matches from the team model here that aren't there
                 */
                if(localTeams != null && localTeams.length > 0) {
                    boolean found = false;
                    RTeam localRef = null;
                    for(RTeam team : localTeams) {
                        if(team.getName().equals(t.getName()) && team.getNumber() == t.getNumber()) {
                            found = true;
                            localRef = team;
                        }
                    }
                    // Team wasn't found locally, so ignore it
                    if(!found) {
                        t.setID(io.getNewTeamID(eventID));
                        io.saveTeam(eventID, t);
                    } else { // team was found locally, so do a match merge (only add matches if they're new!)
                        for(RTab tab : t.getTabs()) {
                            if(!doesExist(localRef, tab.getTitle())) {
                                localRef.addTab(tab);
                                // If these event is cloud synced, a new checkout needs to be packaged
                                if(localEvent.isCloudEnabled()) {
                                    RTeam newTeam = new RTeam(localRef.getName(), localRef.getNumber(), localRef.getID());
                                    newTeam.addTab(tab);
                                    RCheckout checkout = new RCheckout(newTeam);
                                    /*
                                     * It would require a lot more code to check all devices and be sure that a new ID is
                                     * valid, so generate a random one. The chances of an error occurring are so low, this is acceptable (somewhat)
                                     */
                                    checkout.setID(new Random().nextInt(Integer.MAX_VALUE - 50_000) + 20_000);
                                    checkout.setStatus(HandoffStatus.AVAILABLE);
                                    io.savePendingObject(checkout);
                                }

                            }

                            // Update the match wins
                            for(RTab tab1 : localRef.getTabs()) {
                                if(tab1.getTitle().equalsIgnoreCase(tab.getTitle())) tab1.setWon(tab.isWon());
                            }
                        }
                        Collections.sort(localRef.getTabs());
                        io.saveTeam(eventID, localRef);
                    }
                } else {
                    io.saveTeam(eventID, t);
                }
            }

        }
        return null;
    }

    /**
     * Returns true if the match exists
     * @param name the match title
     * @param team the team to analyze
     * @return true if it exists
     */
    private boolean doesExist(RTeam team, String name) {
        for(int i = 0 ; i < team.getTabs().size(); i++) if(team.getTabs().get(i).getTitle().equalsIgnoreCase(name)) return true;
        return false;
    }

    @Override
    protected void onPostExecute(Void params) {
        Intent result = new Intent();
        result.putExtra("eventID", eventID);
        activityWeakReference.get().setResult(Constants.NEW_EVENT_CREATED, result);
        activityWeakReference.get().finish();
        progressDialogWeakReference.get().dismiss();
    }
}
