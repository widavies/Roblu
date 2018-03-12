package com.cpjd.roblu.tba;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Manually import a schedule using the schedule import format from Roblu
 */
public class ManualScheduleImporter extends Thread {

    /**
     * Used for accessing file referencing and IO
     */
    private IO io;

    /**
     * The file to load a manual schedule from, should be a text file separated by commas (.CSV)
     */
    private Uri uri;

    /**
     * The eventID of the event to merge
     */
    private int eventID;

    private Context context;

    /**
     * This listener will receive notifications about the status of the schedule importing
     */
    public ManualScheduleImporterListener listener;

    public interface ManualScheduleImporterListener {
        void error(String message);
        void success();
    }

    /**
     * Attempts to merge the file into an active event
     * @param uri The file to read in with custom match schedule information
     * @param eventID The event ID to merge teams and matches in
     */
    public ManualScheduleImporter(Context context, Uri uri, int eventID, ManualScheduleImporterListener listener) {
        this.eventID = eventID;
        this.io = new IO(context);
        this.context = context;
        this.uri = uri;
        this.listener = listener;
    }

    @Override
    public void run() {
        RForm form = io.loadForm(eventID);
        RTeam[] teams = io.loadTeams(eventID);

        // Load the schedule
        ArrayList<String> lines = new ArrayList<>();
        try {
            InputStream fis = context.getContentResolver().openInputStream(uri);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while((line = br.readLine()) != null) {
                Log.d("RBS", "Line: "+line);
                lines.add(line);
            }
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            listener.error("File was not found on the system.");
            return;
        } catch(IOException e) {
            e.printStackTrace();
            listener.error("File contains syntax errors. Please double check it for accurate syntax.");
            return;
        }

        if(lines.size() == 0) {
            listener.error("File contains no readable data. Please double check syntax.");
            return;
        }

        /*
         * Process the lines, they'll be in a format of
         * teamName,teamNumber,Q1R,Q1M1B,S2M3R,F4B, etc.
         */

        for(int i = 0; i < lines.size(); i++) {
            try {
                String[] tokens = lines.get(i).split(",");
                RTeam team = new RTeam(tokens[0], Integer.parseInt(tokens[1]), io.getNewTeamID(eventID));
                /*
                 * Only add the team if it hasn't been found already
                 */
                if(teams != null) {
                    for(RTeam local : teams) {
                        // Compare name and number, since IDs will be different
                        if(local.getName().equalsIgnoreCase(team.getName()) && local.getNumber() == team.getNumber()) {
                            team = local;
                            break;
                        }
                    }
                }

                // Verify the team against the form
                team.verify(form);

                // The team has been added (or found locally), start processing matches
                for(int j = 2; j < tokens.length; j++) { // use j = 2 to ignore name and number
                    String name = expandMatchName(tokens[j]);
                    boolean isRedAlliance = name.contains("R");
                    name = name.replaceAll("B", "").replaceAll("R", "");

                    RTab tab = new RTab(team.getNumber(), name, form.getMatch(), isRedAlliance, false, 0);

                    // Search for it
                    if(team.getTabs() != null) {
                        boolean found = false;
                        for(RTab local : team.getTabs()) {
                            if(local.getTitle().equalsIgnoreCase(tab.getTitle())) {
                                tab = local;
                                found = true;
                                break;
                            }
                        }

                        if(!found) {
                            team.addTab(tab);
                            Collections.sort(team.getTabs());
                        }
                    }
                }

                io.saveTeam(eventID, team);
            } catch(Exception e ) {
                listener.error("A syntax error occurred one line #"+(i + 1)+". Please double check syntax.");
                return;
            }
        }
        listener.success();
    }

    // Expands the match code, NOTE: this doesn't remove the R or B tag
    private String expandMatchName(String name) {
        return name.replaceAll("QU", "Quarters ").replaceAll("Q", "Quals ")
                .replaceAll("S", "Semis ").replaceAll("M", " Match ").replaceAll("F", "Finals ");
    }

}
