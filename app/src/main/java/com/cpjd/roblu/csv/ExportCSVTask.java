package com.cpjd.roblu.csv;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.cpjd.roblu.csv.csvSheets.CSVSheet;
import com.cpjd.roblu.csv.csvSheets.FieldData;
import com.cpjd.roblu.csv.csvSheets.Lookup;
import com.cpjd.roblu.csv.csvSheets.MatchData;
import com.cpjd.roblu.csv.csvSheets.MatchList;
import com.cpjd.roblu.csv.csvSheets.OurMatches;
import com.cpjd.roblu.csv.csvSheets.PitData;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RCheckout;
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
public class ExportCSVTask extends Thread {

    /**
     * Specify all the CSVSheets
     *
     * The order of this array should match the order of the IDs in
     * @see SHEETS
     */
    private CSVSheet[] CSVSheets = {new MatchData(), new PitData(), new MatchList(), new Lookup(), new OurMatches(), new FieldData()};

    /**
     * Reference to the context object for file system access
     */
    private WeakReference<Context> contextWeakReference;

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

    public static class SHEETS {
        static int MATCH_DATA = 0;
        static int PIT_DATA = 1;
        static int MATCH_LIST = 2;
        static int MATCH_LOOKUP = 3;
        static int OUR_MATCHES = 4;
        static int FIELD_DATA = 5;
    }

    public static class VERBOSENESS {
        public static int ONLY_OBSERVED = 0;
        public static int NOT_OBSERVED_IF_EDITED = 1;
        public static int ALL_NOT_OBSERVED = 2;
    }

    /**
     * Specifies how many metrics should written to the sheet based off
     * @see VERBOSENESS
     */
    private int verboseness;

    /**
     * Specifies the file type
     */
    private boolean isXslx;

    /**
     * Specifies the file name of the file
     */
    private String fileName;

    /**
     * Initializes the ExportCSVTask.
     * @param context context object
     * @param listener the listener that should be notified when the task is completed
     * @param event the event that teams and scouting data should be loaded from
     */
    public ExportCSVTask(Context context, ExportCSVListener listener, REvent event, String fileName, boolean isXslx, ArrayList<Integer> sheetsToGenerate, int verboseness) {
      this.event = event;
      this.contextWeakReference = new WeakReference<>(context);
      this.listener = listener;
      this.verboseness = verboseness;
      this.isXslx = isXslx;
      this.fileName = fileName;

        /*
         *
         *  Create the workbook and start generating data
         *
         */
        this.workbook = new XSSFWorkbook();

        sheets = new LinkedHashMap<>();

        // Create the sheets here since it doesn't work in the thread.
        int index = 0;
        for(CSVSheet s : CSVSheets) {
            s.setEnabled(sheetsToGenerate.indexOf(index) != -1);

            if(s.isEnabled()) {
                Log.d("RBS", "Sheet "+s.getSheetName()+" is enabled.");

                enabledSheets++;
                XSSFSheet sheet = this.workbook.createSheet(s.getSheetName());
                sheets.put(s.getSheetName(), sheet);
            }
            index++;
        }
    }

    @Override
    public void run() {
        if(Build.VERSION.SDK_INT < 21) {
            listener.errorOccurred("Your device does not support CSV exporting.");
            return;
        }

        /*
         * Check to see if the event is null, if it is, we must cancel this task
         */
        if(event == null) {
            listener.errorOccurred("Event could not be loaded.");
            return;
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
            return;
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
         * Build checkouts array, nice way to store data
         */

        final ArrayList<RCheckout> checkouts = new ArrayList<>();
        for(RTeam team : teams) {
            RTeam temp = team.clone();
            temp.removeAllTabsButPIT();
            RCheckout newCheckout = new RCheckout(temp);
            checkouts.add(newCheckout);
        }
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
                checkouts.add(check);
            }
        }

        Collections.sort(checkouts);

        // Create an IO reference
        final IO io = new IO(contextWeakReference.get());

        /*
         * Start executing all the different CSVSheets generate commands
         */
        for(final CSVSheet s : CSVSheets) {
            new Thread() {
                public void run() {
                    if(s.isEnabled()) {
                         try {
                        s.setIo(io);
                        s.setVerboseness(verboseness);
                        s.setWorkbook(workbook);
                        Log.d("RBS", "ExportCSVTask: Generating sheet: "+s.getSheetName());
                        s.setCellStyle(BorderStyle.THIN, IndexedColors.WHITE, IndexedColors.BLACK, false); // sets the default, this may get overrided at any point in time by the user
                        s.generateSheet(sheets.get(s.getSheetName()), event, form, teams, checkouts);
                        for(int i = 0; i < sheets.get(s.getSheetName()).getRow(0).getLastCellNum(); i++) sheets.get(s.getSheetName()).setColumnWidth(i, s.getColumnWidth());
                         } catch(Exception e) {
                             listener.errorOccurred("Failed to execute "+s.getSheetName()+" sheet generation.");
                            Log.d("RBS", "Failed to execute "+s.getSheetName()+" sheet generation. Err: "+e.getMessage());
                         }
                        threadCompleted(s.getSheetName());
                    }

                }
            }.start();
        }

    }

    private void threadCompleted(String name) {
        Log.d("RBS", "A CSV Thread completed: "+name);

        threadsComplete++;
        if(threadsComplete == enabledSheets) {
            File file = new IO(contextWeakReference.get()).getNewCSVExportFile(fileName + ".xslx");

            try {
                FileOutputStream out = new FileOutputStream(file);
                workbook.write(out);

                Log.d("RBS", "Successfully generated .xslx file: "+file.getAbsolutePath());

                try {
                    if(!isXslx) {
                        new ToCSV().convertExcelToCSV(file.getPath(), file.getParentFile().getPath());
                        // Get the new file reference
                        file = new File(file.getParentFile()+File.separator+fileName+".csv");
                        Log.d("RBS", "Converted .xslx to .CSV: "+file.getAbsolutePath()+" Check: "+file.exists());
                    }
                } catch(Exception e) {
                    Log.d("RBS", "Failed to convert the file to .CSV");

                    listener.errorOccurred("Failed to generate ");
                }

                // List contents of file
                for(File f : file.getParentFile().listFiles()) {
                    Log.d("RBS", "Exports dir contains "+f.getAbsolutePath());
                }

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
