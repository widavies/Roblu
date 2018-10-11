package com.cpjd.roblu.tba;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;

import com.cpjd.models.ScoreBreakdown;
import com.cpjd.models.standard.Event;
import com.cpjd.models.standard.Match;
import com.cpjd.models.standard.Team;
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
    private Match[] matches;
    private Team[] teams;
    private int eventID;

    private WeakReference<Activity> activityWeakReference;
    private WeakReference<ProgressDialog> progressDialogWeakReference;

    @Setter
    private boolean randomize;

    public UnpackTBAEvent(Event e, Team[] teams, Match[] matches, int eventID, Activity activity, ProgressDialog d) {
        this.eventID = eventID;
        this.event = e;
        this.teams = teams;
        this.matches = matches;
        this.activityWeakReference = new WeakReference<>(activity);
        this.progressDialogWeakReference = new WeakReference<>(d);
    }

    protected Void doInBackground(Void... params) {

        /*
         * No teams were contained within the event, so exit, nothing here is relevant
         * to a TBA event that doesn't contain any team models
         */
        if(teams == null || teams.length == 0) return null;

        /*
         * Create an array of team models from the ones contained in the event
         */
        ArrayList<RTeam> teams = new ArrayList<>();
        for(int i = 0; i < this.teams.length; i++) {
            // i can be used as the ID because we are creating a fresh event, io.getNewTeamID is irrelevant
            teams.add(new RTeam(this.teams[i].getNickname(), (int) this.teams[i].getTeamNumber(), i));
        }

        /*
         * Sort the matches in the event
         */
        Collections.sort(Arrays.asList(this.matches));

        /*
         * Add the matches to the respective team models
         */
        IO io = new IO(activityWeakReference.get());
        RForm form = io.loadForm(eventID);

        for(RTeam t : teams) {
            t.verify(form);
            for(int j = 0; j < matches.length; j++) {
                if(doesMatchContainTeam(matches[j], t.getNumber())) {
                    String name = "Match";
                    // process the correct match name
                    switch(matches[j].getCompLevel()) {
                        case "qm":
                            name = "Quals " + matches[j].getMatchNumber();
                            break;
                        case "qf":
                            name = "Quarters " + matches[j].getSetNumber() + " Match " + matches[j].getMatchNumber();
                            break;
                        case "sf":
                            name = "Semis " + matches[j].getSetNumber() + " Match " + matches[j].getMatchNumber();
                            break;
                        case "f":
                            name = "Finals " + matches[j].getMatchNumber();
                    }
                    boolean isRed = getAlliancePosition(matches[j], t.getNumber()) <= 3;
                    // add the match to the team, make sure to multiple the Event model's matches times by 1000 (seconds to milliseconds, Roblu works with milliseconds!)
                    RTab tab = new RTab(t.getNumber(), name, Utils.duplicateRMetricArray(form.getMatch()), isRed, isOnWinningAlliance(matches[j], t.getNumber()), matches[j].getTime() * 1000);
                    // set the match position, if possible
                    tab.setAlliancePosition(getAlliancePosition(matches[j], t.getNumber()));

                    // Check for FieldData metrics
                    if(tab.getMetrics() != null) {
                        for(RMetric metric : tab.getMetrics()) {
                            if(metric instanceof RFieldData) {
                                if(((RFieldData) metric).getData() == null) ((RFieldData) metric).setData(new LinkedHashMap<String, ArrayList<RMetric>>());

                                ScoreBreakdown redScore = matches[j].getRedScoreBreakdown();
                                ScoreBreakdown blueScore = matches[j].getBlueScoreBreakdown();

                                String[] keyValues = new String[redScore.getValues().size()];
                                keyValues = redScore.getValues().keySet().toArray(keyValues);

                                for(String keyValue : keyValues) {
                                    ArrayList<RMetric> metrics = new ArrayList<>();
                                    try {
                                        metrics.add(new RCounter(0, "", 0, Double.parseDouble(String.valueOf(redScore.getValues().get(keyValue)))));
                                    } catch(Exception e) {
                                        metrics.add(new RTextfield(0, "", (String.valueOf(redScore.getValues().get(keyValue)))));
                                    }
                                    try {
                                        metrics.add(new RCounter(0, "", 0, Double.parseDouble(String.valueOf(blueScore.getValues().get(keyValue)))));
                                    } catch(Exception e) {
                                        metrics.add(new RTextfield(0, "", (String.valueOf(blueScore.getValues().get(keyValue)))));
                                    }

                                    if(keyValues != null && metrics.size() > 0) ((RFieldData) metric).getData().put(keyValue, metrics);
                                }

                                // Sort it
                                if(((RFieldData) metric).getData() != null) {
                                    ArrayList<String> keys = new ArrayList<>(((RFieldData) metric).getData().keySet());
                                    Collections.sort(keys);
                                    LinkedHashMap<String, ArrayList<RMetric>> sorted = new LinkedHashMap<>();
                                    for(String s : keys) {
                                        sorted.put(s, ((RFieldData) metric).getData().get(s));
                                    }
                                    ((RFieldData) metric).setData(sorted);
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

    private int getAlliancePosition(Match m, int teamNumber) {
        for(int i = 0; i < m.getBlue().getTeamKeys().length; i++) {
            if(m.getBlue().getTeamKeys()[i].equals("frc"+teamNumber)) return i + 4;
        }
        for(int i = 0; i < m.getRed().getTeamKeys().length; i++) {
            if(m.getRed().getTeamKeys()[i].equals("frc"+teamNumber)) return i + 1;
        }

        return -1;
    }

    private boolean isOnWinningAlliance(Match m, int teamNumber) {
        boolean redWinner = m.getWinningAlliance().toLowerCase().contains("red");
        return (redWinner && getAlliancePosition(m, teamNumber) <= 3) || (!redWinner && getAlliancePosition(m, teamNumber) >= 4);
    }

    private boolean doesMatchContainTeam(Match m, int teamNumber) {
        return getAlliancePosition(m, teamNumber) != -1;
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
