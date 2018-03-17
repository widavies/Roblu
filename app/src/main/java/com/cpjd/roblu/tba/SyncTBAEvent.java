package com.cpjd.roblu.tba;

import com.cpjd.models.Event;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RFieldData;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RTextfield;
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

    private Event event;
    private RForm form;
    private int eventID;
    private IO io;

    private SyncTBAEventListener listener;

    public interface SyncTBAEventListener {
        void done();
    }

    public SyncTBAEvent(Event event, int eventID, IO io, SyncTBAEventListener listener) {
        this.event = event;
        this.eventID = eventID;
        this.io = io;
        this.listener = listener;
    }

    @Override
    public void run() {
        // Load all the teams locally
        RTeam[] teams = io.loadTeams(eventID);
        form = io.loadForm(eventID);

        /*
         * Start processing!
         */
        for(int i = 0; i < event.teams.length; i++) {
            // Check if the team already exists
            boolean found = false;
            if(teams != null) {
                for(RTeam team : teams) {
                    if(team.getName().equals(event.teams[i].nickname) && team.getNumber() == event.teams[i].team_number) {
                        syncTeam(team);
                        found = true;
                        break;
                    }
                }
            }
            if(!found) {
                RTeam newTeam = new RTeam(event.teams[i].nickname, (int) event.teams[i].team_number, io.getNewTeamID(eventID));
                syncTeam(newTeam);
            }
        }

        listener.done();
    }
    
    private void syncTeam(RTeam team) {
        team.verify(form);

        for(int i = 0; i < event.matches.length; i++) {
            if(event.matches[i].doesMatchContainTeam(team.getNumber()) == -1) continue;

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

                        for(int i = 0; i < event.matches[index].scorableItems.length; i++) {

                            ArrayList<RMetric> metrics = new ArrayList<>();
                            try {
                                metrics.add(new RCounter(0, "", 0, Double.parseDouble(event.matches[index].redValues[i])));
                            } catch(Exception e) {
                                metrics.add(new RTextfield(0, "", (event.matches[index].redValues[i])));
                            }
                            try {
                                metrics.add(new RCounter(0, "", 0, Double.parseDouble(event.matches[index].blueValues[i])));
                            } catch(Exception e) {
                                metrics.add(new RTextfield(0, "", (event.matches[index].blueValues[i])));
                            }

                            if(event.matches[index].scorableItems[i] != null && metrics.size() > 0) ((RFieldData) metric).getData().put(event.matches[index].scorableItems[i], metrics);
                        }
                    }
                }
            }
        } catch(Exception e) {}

    }
    
    private RTab matchModelToTab(int index, int teamNumber) {
        int result = event.matches[index].doesMatchContainTeam(teamNumber);
        if(result > 0) {
            String name = "Match";
            // process the correct match name
            switch(event.matches[index].comp_level) {
                case "qm":
                    name = "Quals " + event.matches[index].match_number;
                    break;
                case "qf":
                    name = "Quarters " + event.matches[index].set_number + " Match " + event.matches[index].match_number;
                    break;
                case "sf":
                    name = "Semis " + event.matches[index].set_number + " Match " + event.matches[index].match_number;
                    break;
                case "f":
                    name = "Finals " + event.matches[index].match_number;
            }
            boolean isRed = result == com.cpjd.main.Constants.CONTAINS_TEAM_RED;
            // add the match to the team, make sure to multiple the Event model's matches times by 1000 (seconds to milliseconds, Roblu works with milliseconds!)
            RTab tab = new RTab(teamNumber, name, Utils.duplicateRMetricArray(form.getMatch()), isRed, event.matches[index].isOnWinningAlliance(teamNumber), event.matches[index].time * 1000);
            // set the match position, if possible
            tab.setAlliancePosition(event.matches[index].getTeamPosition(teamNumber));
            return tab;
        }
        return null;
    }
    

}
