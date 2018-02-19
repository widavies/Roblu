package com.cpjd.roblu.csv;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.cpjd.roblu.csv.csvSheets.CSVSheet;
import com.cpjd.roblu.csv.csvSheets.MatchData;
import com.cpjd.roblu.csv.csvSheets.OurMatches;
import com.cpjd.roblu.csv.csvSheets.PitData;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.ui.teams.TeamsView;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * ExportCSVTask manages the exporting of a .CSV file. It's saved as an .XLSX file, meaning that it can be
 * opened in Excel or Sheets. Quick note: If you're looking to define your own sheet, head over to
 * @see CSVSheet
 * Extend that class, and code it to do what you want.
 * The only time you should mess with this class is to add it to the "CSVSheets" array below so this task loads it
 * when the user requests and export.
 *
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class ExportCSVTask extends AsyncTask<Void, Void, Void> {

    /**
     * Specify all the CSVSheets
     * !!ADD YOUR SHEET HERE!!
     */
    private CSVSheet[] CSVSheets = {new MatchData(), new PitData(), new OurMatches()};

    /**
     * Reference to the context object for file system access
     */
    private WeakReference<Context> contextWeakReference;

    /**
     * ExportCSVTask will close the progress bar dialog automatically when it finishes
     */
    private WeakReference<ProgressDialog> progressDialogWeakReference;

    /**
     * The event that is being exported
     */
    private REvent event;

    /**
     * An RForm reference, because every team must be verified before scouting data can be accessed
     */
    private RForm form;

    /**
     * The array of teams to search and process for scouting data
     */
    private RTeam[] teams;

    /**
     * Keeps track of how many sheet threads have completed, if this is equal to the amount of enabled CSVSheets,
     * then listener.csvFileGenerate(File file) will be called
     */
    private int threadsComplete;

    /**
     * Keeps track of how many of the CSVSheets are enabled so this task knows when to stop the thread
     */
    private int enabledSheets;

    public interface ExportCSVListener {
        void errorOccurred(String message);
        void csvFileGenerated(File file);
    }

    /**
     * This listener will be notified when the export csv task is completed
     */
    private ExportCSVListener listener;

    /**
     * Reference to the workbook file so it can be written in onPostExecute()
     */
    private final XSSFWorkbook workbook;

    /**
     * For some reason, sheet creation isn't thread safe and causes an exception.
     * Therefore, sheets should be generated in the constructor
     */
    private HashMap<String, XSSFSheet> sheets;

    /**
     * Initializes the ExportCSVTask.
     * @param context context object
     * @param listener the listener that should be notified when the task is completed
     * @param event the event that teams and scouting data should be loaded from
     */
    public ExportCSVTask(Context context, ExportCSVListener listener, ProgressDialog progressDialog, REvent event) {
      this.event = event;
      this.contextWeakReference = new WeakReference<>(context);
      this.listener = listener;
      this.progressDialogWeakReference = new WeakReference<>(progressDialog);

        /*
         *
         *  Create the workbook and start generating data
         *
         */
        this.workbook = new XSSFWorkbook();

        sheets = new LinkedHashMap<>();

        // Create the sheets here since it doesn't work in the thread.
        for(CSVSheet s : CSVSheets) {
            if(s.isEnabled()) {
                enabledSheets++;
                XSSFSheet sheet = this.workbook.createSheet(s.getSheetName());
                sheets.put(s.getSheetName(), sheet);
            }
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if(Build.VERSION.SDK_INT < 21) {
            listener.errorOccurred("Your device does not support CSV exporting.");
            return null;
        }

        /*
         * Check to see if the event is null, if it is, we must cancel this task
         */
        if(event == null) {
            listener.errorOccurred("Event could not be loaded.");
            this.cancel(true);
            return null;
        }
        /*
         * Load teams
         */
        teams = new IO(contextWeakReference.get()).loadTeams(event.getID());
        form = new IO(contextWeakReference.get()).loadForm(event.getID());
        /*
         * Check to see if the teams or forms are null, if they are, cancel the task
         */
        if(teams == null || teams.length == 0 || form == null) {
            listener.errorOccurred("This event doesn't contain any teams");
            this.cancel(true);
            return null;
        }

        /*
         * Verify all the teams
         */
        for(RTeam team : teams) {
            team.setFilter(TeamsView.SORT_TYPE.NUMERICAL); // also sneak in a line here to tell each team to sort by numerical if a sort request is made
            team.verify(form);
            new IO(contextWeakReference.get()).saveTeam(event.getID(), team);
        }

        /*
         * Sort the teams by number
         */
        Collections.sort(Arrays.asList(teams));

        /*
         * Sort matches
         *
         * This method provides a list of matches that satisfy the two following conditions:
         * -The match is contained within at least 1 team
         * -The match within a SPECIFIC team has at least 1 modified value
         */
        final ArrayList<RMatch> matches = new ArrayList<>();
        for(RTeam team : teams) {
            for(int i = 1; i < team.getTabs().size(); i++) {
                // alright, let's first check if the match has already been added to the array
                boolean found = false;
                for(int j = 0; j < matches.size(); j++) {
                    if(matches.get(j).getMatchName().equalsIgnoreCase(team.getTabs().get(i).getTitle())) {
                        found = true;
                        break;
                    }
                }

                // if we didn't find it, let's add it
                if(!found) {
                    // first, make sure that at least one team, somewhere, has a value that has been modified for this match
                    boolean modifiedSomewhere = false;
                    teamLoop : for(RTeam temp : teams) {
                        for(int k = 1; k < temp.getTabs().size(); k++) {
                            if(temp.getTabs().get(k).getTitle().equalsIgnoreCase(team.getTabs().get(i).getTitle())) {
                                for(int l = 0; l < temp.getTabs().get(k).getMetrics().size(); l++) {
                                    if(temp.getTabs().get(k).getMetrics().get(l).isModified()) {
                                        modifiedSomewhere = true;
                                        break teamLoop;
                                    }
                                }
                            }
                        }
                    }
                    if(modifiedSomewhere) matches.add(new RMatch(team.getTabs().get(i).getTitle()));
                }
            }
        }
        // Sort the matches by name
        Collections.sort(matches);
        // Convert to array instead of ArrayList
        final RMatch[] matches1 = new RMatch[matches.size()];
        for(int i = 0; i < matches.size(); i++) matches1[i] = matches.get(i);

        // Create an IO reference
        final IO io = new IO(contextWeakReference.get());

        /*
         * Start executing all the different CSVSheets generate commands
         */
        for(final CSVSheet s : CSVSheets) {
            new Thread() {
              public void run() {
                  if(s.isEnabled()) {
                     // try {
                          s.setIo(io);
                          s.setWorkbook(workbook);
                          Log.d("RBS", "ExportCSVTask: Generating sheet: "+s.getSheetName());
                          s.setCellStyle(BorderStyle.THIN, IndexedColors.WHITE, IndexedColors.BLACK, false); // sets the default, this may get overrided at any point in time by the user
                          s.generateSheet(sheets.get(s.getSheetName()), event, form, teams, matches1);
                          for(int i = 0; i < sheets.get(s.getSheetName()).getRow(0).getLastCellNum(); i++) sheets.get(s.getSheetName()).setColumnWidth(i, s.getColumnWidth());
                     // } catch(Exception e) {
                     //     listener.errorOccurred("Failed to execute "+s.getSheetName()+" sheet generation.");
                      //    Log.d("RBS", "Failed to execute "+s.getSheetName()+" sheet generation. Err: "+e.getMessage());
                     // }
                  }

                  threadCompleted(s.getSheetName());
              }
            }.start();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void params) {
        progressDialogWeakReference.get().dismiss();
    }

    private void threadCompleted(String name) {
        Log.d("RBS", "A CSV Thread completed: "+name);

        threadsComplete++;
        if(threadsComplete == enabledSheets) {
            File file = new IO(contextWeakReference.get()).getNewCSVExportFile();
            try {
                FileOutputStream out = new FileOutputStream(file);
                workbook.write(out);
                out.close();
                listener.csvFileGenerated(file);
            } catch(IOException e) {
                e.printStackTrace();
                Log.d("RBS", "ERR: "+e.getMessage());
                listener.errorOccurred("Failed to write file.");
            }
        }
    }
}
