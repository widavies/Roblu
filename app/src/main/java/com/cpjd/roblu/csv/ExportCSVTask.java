package com.cpjd.roblu.csv;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.cpjd.roblu.csv.sheets.MatchData;
import com.cpjd.roblu.csv.sheets.Sheet;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.ui.teams.TeamsView;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * ExportCSVTask manages the exporting of a .CSV file. It's saved as an .xslx file, meaning that it can be
 * opened in Excel or Sheets. This class will handle some formatting and what not. If you'd like to specify your
 * own export format:
 *
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class ExportCSVTask extends AsyncTask<Void, Void, Void> {

    /**
     * Specify all the sheets
     * !!ADD YOUR SHEET HERE!!
     */
    private Sheet[] sheets = {new MatchData()};

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
     * Specifies whether scouting metrics whose modified value is false should still be included in exports.
     * If true, this will add a few more seconds to the compute time
     * @see com.cpjd.roblu.models.metrics.RMetric
     */
    private boolean generateVerbose;

    /**
     * Helper variable for keeping track of how many metrics have been added to the .CSV file
     */
    private int metricCount;

    /**
     * Stores the styles for various spreadsheets cells
     */
    private XSSFCellStyle teamStyle, value1Style, value2Style, value3Style, value4Style;

    /**
     * Keeps track of how many sheet threads have completed, if this is equal to the amount of enabled sheets,
     * then listener.csvFileGenerate(File file) will be called
     */
    private int threadsComplete;

    /**
     * Keeps track of how many of the sheets are enabled so this task knows when to stop the thread
     */
    private int enabledSheets;

    interface ExportCSVListener {
        void errorOccurred(String message);
        void csvFileGenerated(File file);
    }

    /**
     * This listener will be notified when the export csv task is completed
     */
    private ExportCSVListener listener;

    /**
     * Initializes the ExportCSVTask.
     * @param context context object
     * @param listener the listener that should be notified when the task is completed
     * @param event the event that teams and scouting data should be loaded from
     * @param generateVerbose see generateVerbose variable documentation
     */
    public ExportCSVTask(Context context, ExportCSVListener listener, REvent event, boolean generateVerbose) {
      this.event = event;
      this.contextWeakReference = new WeakReference<>(context);
      this.listener = listener;
      this.generateVerbose = generateVerbose;

      for(Sheet s : sheets) if(s.isEnabled()) enabledSheets++;
    }

    @Override
    protected Void doInBackground(Void... voids) {
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

        /*
         *
         *  Create the workbook and start generating data
         *
         */
        final XSSFWorkbook workbook = new XSSFWorkbook();

        /*
         * Start executing all the different sheets generate commands
         */
        for(final Sheet s : sheets) {
            new Thread() {
              public void run() {
                  if(s.isEnabled()) {
                      XSSFSheet sheet = workbook.createSheet(s.getSheetName());
                      s.setCellStyle(BorderStyle.THIN, IndexedColors.WHITE, IndexedColors.BLACK, false); // sets the default, this may get overrided at any point in time by the user
                      s.generateSheet(sheet, form, teams, matches1);
                      for(int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) sheet.setColumnWidth(i, s.getColumnWidth());
                  }

                  threadCompleted();

                  try {
                      join();
                  } catch(InterruptedException e) {
                      Log.d("RBS", "Failed to stop a CSV sheet thread.");
                  }
              }
            }.start();
        }



        return null;
    }

    private void threadCompleted() {
        threadsComplete++;
        if(threadsComplete == enabledSheets) {
            listener.csvFileGenerated(null);
        }
    }

    private void initStyles(XSSFWorkbook wb) {
        teamStyle = wb.createCellStyle();
        setBorder(teamStyle);
        teamStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
        teamStyle.setFillForegroundColor(IndexedColors.BLACK.index);
        Font font = wb.createFont();
        font.setColor(IndexedColors.WHITE.index);
        font.setBold(true);
        teamStyle.setFont(font);


        value1Style = wb.createCellStyle(); setBorder(value1Style);
        value2Style = wb.createCellStyle(); setBorder(value2Style);
        value3Style = wb.createCellStyle(); setBorder(value3Style);
        value4Style = wb.createCellStyle(); setBorder(value4Style);

        value1Style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
        value1Style.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.index);

        value2Style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
        value2Style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.index);

        value3Style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
        value3Style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.index);

        value4Style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
        value4Style.setFillForegroundColor(IndexedColors.CORAL.index);
    }

    private void setBorder(XSSFCellStyle style) {
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
    }
}
