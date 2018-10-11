package com.cpjd.roblu.tba;

import android.os.StrictMode;

import com.cpjd.main.TBA;
import com.cpjd.models.ScoreBreakdown;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;


/**
 * Adds any newly available information on TheBlueAlliance.com to the target event
 *
 * @since 4.4.0
 * @author Will Davies
 */
public class SyncTBAEvent extends Thread {

    private Team[] eventTeams;
    private Match[] matches;
    private RForm form;
    private int eventID;
    private IO io;


    private SyncTBAEventListener listener;

    public interface SyncTBAEventListener {
        void done();
    }

    public SyncTBAEvent(Team[] teams, Match[] matches, int eventID, IO io, SyncTBAEventListener listener) {
        this.eventTeams = teams;
        this.matches = matches;
        this.eventID = eventID;
        this.io = io;
        this.listener = listener;
    }

    @Override
    public void run() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        // Set auth token
        TBA.setAuthToken(Constants.PUBLIC_TBA_READ_KEY);

        // Load all the eventTeams locally
        RTeam[] teams = io.loadTeams(eventID);
        form = io.loadForm(eventID);
        
        /*
         * Start processing!
         */
        for(int i = 0; i < eventTeams.length; i++) {
            // Check if the team already exists
            boolean found = false;
            if(teams != null) {
                for(RTeam team : teams) {
                    if(team.getName().equals(eventTeams[i].getNickname()) && team.getNumber() == eventTeams[i].getTeamNumber()) {
                        syncTeam(team);
                        found = true;
                        break;
                    }
                }
            }
            if(!found) {
                RTeam newTeam = new RTeam(eventTeams[i].getNickname(), (int) eventTeams[i].getTeamNumber(), io.getNewTeamID(eventID));
                syncTeam(newTeam);
            }
        }

        listener.done();
    }
    
    private void syncTeam(RTeam team) {
        team.verify(form);

        for(int i = 0; i < matches.length; i++) {
            if(!doesMatchContainTeam(matches[i], team.getNumber())) continue;

            RTab newTab = matchModelToTab(i, team.getNumber());

            if(newTab == null) continue;
            // Search for the match within the team
            boolean found = false;
            for(RTab t : team.getTabs()) {
                if(t.getTitle().equalsIgnoreCase(newTab.getTitle())) {
                    newTab = t;
                    found = true;
                    break;
                }
            }

            // Add the new match
            if(!found) {
                team.addTab(newTab);
                Collections.sort(team.getTabs());
            }

            /*
            * Process field data
            */
            if(team.getTabs() != null) {
                insertFieldData(i, newTab);
            }
        }

        // Save the team
        io.saveTeam(eventID, team);
    }
    
    private void insertFieldData(int index, RTab tab) {
        try {
            // Check for FieldData metrics
            if(tab.getMetrics() != null) {
                for(RMetric metric : tab.getMetrics()) {
                    if(metric instanceof RFieldData) {
                        if(((RFieldData) metric).getData() == null) ((RFieldData) metric).setData(new LinkedHashMap<String, ArrayList<RMetric>>());

                        ScoreBreakdown redScore = matches[index].getRedScoreBreakdown();
                        ScoreBreakdown blueScore = matches[index].getBlueScoreBreakdown();

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
        } catch(Exception e) {
            e.printStackTrace();
        }

    }
    
    private RTab matchModelToTab(int index, int teamNumber) {
        if(doesMatchContainTeam(matches[index], teamNumber)) {
            String name = "Match";
            // process the correct match name
            switch(matches[index].getCompLevel()) {
                case "qm":
                    name = "Quals " + matches[index].getMatchNumber();
                    break;
                case "qf":
                    name = "Quarters " + matches[index].getSetNumber() + " Match " + matches[index].getMatchNumber();
                    break;
                case "sf":
                    name = "Semis " + matches[index].getSetNumber() + " Match " + matches[index].getMatchNumber();
                    break;
                case "f":
                    name = "Finals " + matches[index].getMatchNumber();
            }
            boolean isRed = getAlliancePosition(matches[index], teamNumber) <= 3;
            // add the match to the team, make sure to multiple the Event model's matches times by 1000 (seconds to milliseconds, Roblu works with milliseconds!)
            RTab tab = new RTab(teamNumber, name, Utils.duplicateRMetricArray(form.getMatch()), isRed, isOnWinningAlliance(matches[index], teamNumber), matches[index].getTime() * 1000);
            // set the match position, if possible
            tab.setAlliancePosition(getAlliancePosition(matches[index], teamNumber));
            return tab;
        }
        return null;
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

}
