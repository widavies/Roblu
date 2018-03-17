package com.cpjd.roblu.tba;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.cpjd.models.Event;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RFieldData;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RTextfield;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

import lombok.Setter;

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

    private WeakReference<Activity> activityWeakReference;
    private WeakReference<ProgressDialog> progressDialogWeakReference;

    @Setter
    private boolean randomize;

    public UnpackTBAEvent(Event e, int eventID, Activity activity, ProgressDialog d) {
        this.eventID = eventID;
        this.event = e;
        this.activityWeakReference = new WeakReference<>(activity);
        this.progressDialogWeakReference = new WeakReference<>(d);
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

                    // Check for FieldData metrics
                    if(tab.getMetrics() != null) {
                        for(RMetric metric : tab.getMetrics()) {
                            if(metric instanceof RFieldData) {
                                if(((RFieldData) metric).getData() == null) ((RFieldData) metric).setData(new LinkedHashMap<String, ArrayList<RMetric>>());

                                for(int i = 0; i < event.matches[j].scorableItems.length; i++) {

                                    Log.d("RBS", "Metric name: "+event.matches[j].scorableItems[i]+", "+"Red value: "+event.matches[j].redValues[i]+", Blue value: "+event.matches[j].blueValues[i]);

                                    ArrayList<RMetric> metrics = new ArrayList<>();
                                    try {
                                        metrics.add(new RCounter(0, "", 0, Double.parseDouble(event.matches[j].redValues[i])));
                                    } catch(Exception e) {
                                        metrics.add(new RTextfield(0, "", (event.matches[j].redValues[i])));
                                    }
                                    try {
                                        metrics.add(new RCounter(0, "", 0, Double.parseDouble(event.matches[j].blueValues[i])));
                                    } catch(Exception e) {
                                        metrics.add(new RTextfield(0, "", (event.matches[j].blueValues[i])));
                                    }

                                    if(event.matches[j].scorableItems[i] != null && metrics.size() > 0) ((RFieldData) metric).getData().put(event.matches[j].scorableItems[i], metrics);
                                }
                            }
                        }
                    }
                    t.addTab(tab);
                }
            }

            /*
             * This is where the merge decision comes into play
             */
            if(randomize) {
                t.setLastEdit(System.currentTimeMillis());
                Utils.randomizeTeamMetrics(t.getTabs());
            }

            io.saveTeam(eventID, t);
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
