package com.cpjd.roblu.csv;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v4.content.FileProvider;
import android.widget.RelativeLayout;

import com.cpjd.main.TBA;
import com.cpjd.models.Event;
import com.cpjd.models.Match;
import com.cpjd.roblu.BuildConfig;
import com.cpjd.roblu.forms.elements.EBoolean;
import com.cpjd.roblu.forms.elements.ECheckbox;
import com.cpjd.roblu.forms.elements.EChooser;
import com.cpjd.roblu.forms.elements.ECounter;
import com.cpjd.roblu.forms.elements.EGallery;
import com.cpjd.roblu.forms.elements.ESTextfield;
import com.cpjd.roblu.forms.elements.ESlider;
import com.cpjd.roblu.forms.elements.EStopwatch;
import com.cpjd.roblu.forms.elements.ETextfield;
import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.teams.TeamsView;
import com.cpjd.roblu.utils.Text;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/*
 * Exports a file. Behold the motherload of for loops!
 */
public class ExportCSV extends AsyncTask<Void, Void, File> {

    private final REvent event;
    private final RForm form;
    private final RTeam[] teams;

    /*
     * Formatting
     */
    private int elementCount;

    private final Activity activity;
    private final ProgressDialog d;

    private final boolean generateVerbose;

    // Notice! All teams must be verified before being exported.

    /*
     * Exports a .xlsx file that contains a lot of useful information about the event
     */
    public ExportCSV(RelativeLayout layout, REvent event, Activity activity, boolean generateVerbose) {
        this.activity = activity;
        this.event = event;
        this.generateVerbose = generateVerbose;

        if(!generateVerbose) d = ProgressDialog.show(activity, "Please wait...", "Generating .csv file...", true);
        else d = ProgressDialog.show(activity, "Please wait...", "Generating .csv file with verbose data... This could take up to several minutes");

        teams = new Loader(activity).getTeams(event.getID());
        form = new Loader(activity).loadForm(event.getID());

        if(teams == null || teams.length == 0) {
            Text.showSnackbar(layout, activity, "This event doesn't contain any teams.", true, 0);
            this.cancel(true);
            d.dismiss();
        }
    }

    public File doInBackground(Void... params) {
        return export();
    }

    public void onPostExecute(File file) {
        d.dismiss();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        Uri uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID, file);
        intent.setType("application/vnd.ms-excel");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        PackageManager pm = activity.getPackageManager();
        if(intent.resolveActivity(pm) != null) {
            activity.startActivity(Intent.createChooser(intent, "Export spreadsheet to..."));
        }
    }

    private File export() {
        // Verify all teams
        for(int i = 0; i < teams.length; i++) {
            RTeam team = teams[i];
            team.verify(form);
            teams[i] = team;
            new Loader(activity).saveTeam(team, event.getID());
        }

        // Sort teams by number
        for(RTeam team : teams) {
            team.setFilter(TeamsView.NUMERICAL);
        }
        Collections.sort(Arrays.asList(teams));

        // Create workbook
        XSSFWorkbook wb = new XSSFWorkbook();

        initStyles(wb);

        System.out.println("Generating sheet 1");
        genSheet1(wb, false, false);
        System.out.println("Generating sheet 2");
        genSheet2(wb);
        System.out.println("Generating sheet 3");
        genSheet3(wb);
        System.out.println("Generating sheet 4");
        if(generateVerbose) genSheet5(wb);
        System.out.println("Done generating.");

        File file = new Loader(activity).getNewExportFile();
        try {
            FileOutputStream out = new FileOutputStream(file);
            wb.write(out);
            out.close();
        } catch(IOException e) {
            System.out.println("Failed to save .xlsx file");
        }
        return file;
    }

    // Sheet one displays match data for every match of every team

    private void genSheet1(XSSFWorkbook wb, boolean includeNonModifiedMatches, boolean includeUnModified) {
        XSSFSheet matchData;
        if(includeNonModifiedMatches) matchData = wb.createSheet("VerboseMatchData");
        else matchData = wb.createSheet("MatchData");

        /*
         * We need to process the matches we need. We need these traits for the match to be displayed on the .csv file:
         *
         * -Match must exist in at least 1 team (example: quals 1)
         * -At least one team must contain the match and have one of the values been modified.
         */
        ArrayList<SortingHelper> sorting = new ArrayList<>();
        for(RTeam team : teams) {
            for(int i = 1; i < team.getTabs().size(); i++) {
                // alright, let's first check if the match has already been added to the array
                boolean found = false;
                for(int j = 0; j < sorting.size(); j++) {
                    if(sorting.get(j).getName().equalsIgnoreCase(team.getTabs().get(i).getTitle())) {
                        found = true;
                        break;
                    }
                }

                // if we didn't find it, let's add it
                if(!found) {
                    // first, make sure that at least one team, somewhere, has a value that has been modified for this mathc
                    boolean modifiedSomewhere = false;
                    teamLoop : for(RTeam temp : teams) {
                        for(int k = 1; k < temp.getTabs().size(); k++) {
                            if(temp.getTabs().get(k).getTitle().equalsIgnoreCase(team.getTabs().get(i).getTitle())) {
                                for(int l = 0; l < temp.getTabs().get(k).getElements().size(); l++) {
                                    if(temp.getTabs().get(k).getElements().get(l).isModified()) {
                                        modifiedSomewhere = true;
                                        break teamLoop;
                                    }
                                }
                            }
                        }
                    }
                    if(modifiedSomewhere || includeNonModifiedMatches) sorting.add(new SortingHelper(team.getTabs().get(i).getTitle()));

                }
            }
        }

        // Now, we must sort the array so that quals are first, quarters next, etc.
        Collections.sort(sorting);

        if(sorting.size() == 0) {
            matchData.createRow(0).createCell(0).setCellValue("No match data is available.");
            return;
        }

        /*
         * NEXT, we process the first two rows
         * -titles
         * -matches labels
         */

        // The first couple cells that are the same no matter what
        Row titles = matchData.createRow(0);
        titles.setHeightInPoints(30f);
        Row matchNums = matchData.createRow(1);

        titles.createCell(0);
        Cell teamNum = matchNums.createCell(0);
        teamNum.setCellValue("Team#");
        teamNum.setCellStyle(teamStyle);

        for(int i = 0; i < form.getMatch().size(); i++) {
            XSSFCellStyle style = getStyleForColumn();

            // set the match display token, examples "P, 1, Q1M3"
            for(int j = 0; j < sorting.size(); j++) {
                Cell num = matchNums.createCell(j + 1 + ((i) * sorting.size()));
                num.setCellValue(sorting.get(j).getAbbreviatedName());
                num.setCellStyle(style);

                matchData.setColumnWidth(j + 1 + ((i) * sorting.size()), 512);
            }

            /*
             * Calculate possible values for the element, to display under the title
             */
            String possibleValues = "";

            Element e = form.getMatch().get(i);
            if(e != null) {
                if(e instanceof EBoolean) possibleValues = "(Y/N)";
                else if(e instanceof ECheckbox) {
                    possibleValues = "(";
                    for(int check = 0; check < ((ECheckbox) e).getChecked().size(); check++) {
                        possibleValues +=" Y / N,";
                    }
                    possibleValues += ")";
                }
                else if(e instanceof EChooser) {
                    possibleValues = "(";
                    for(int item = 0; item < ((EChooser) e).getValues().size(); item++) {
                        if(item == ((EChooser) e).getValues().size() - 1) possibleValues += ((EChooser) e).getValues().get(item) + ")";
                        else possibleValues += ((EChooser) e).getValues().get(item) +"/";
                    }
                }
                else if(e instanceof ECounter) possibleValues = "(#)";
                else if(e instanceof ESlider) possibleValues = "(#)";
                else if(e instanceof EStopwatch) possibleValues = "(Sec)";
            }

            style.setAlignment(HorizontalAlignment.CENTER);
            Font f = wb.createFont();
            f.setBold(true);
            style.setFont(f);

            // Row one stuff - cells must be merged
            Cell title = titles.createCell(1 + ((i) * sorting.size()));
            title.setCellStyle(style);
            title.setCellValue(form.getMatch().get(i).getTitle() + "\n" + possibleValues);
            matchData.addMergedRegion(new CellRangeAddress(0,0,1 + ((i) * sorting.size()),sorting.size() + ((i) * sorting.size())));

        }

        /*
         * Now we process data!
         */
        for(int team = 0; team < teams.length; team++) {
            elementCount = 0;

            // Add the team number to the leftmost column
            Row row = matchData.createRow(team + 2);
            Cell c = row.createCell(0);
            c.setCellValue(teams[team].getNumber());
            c.setCellStyle(teamStyle);

            for (int element = 0; element < form.getMatch().size(); element++) {
                XSSFCellStyle style = getStyleForColumn();

                for (int match = 0; match < sorting.size(); match++) {
                    // Get the element from the team, it must exist
                    Element e = null;

                    // check if the team contains the match, if not, there is no data that the team could add
                    int matchFound = -1;
                    for(int exist = 1; exist < teams[team].getTabs().size(); exist++) if(teams[team].getTabs().get(exist).getTitle().equalsIgnoreCase(sorting.get(match).getName())) { matchFound = exist; break; }
                    if(matchFound == -1) {
                        row.createCell(match + 1 + ((element) * sorting.size())).setCellStyle(style);
                        continue;
                    }

                    for(Element el : teams[team].getItems(matchFound)) if(el.getID() == form.getMatch().get(element).getID()) { e = el; break; }

                    // Add the element's data only if the element has been modified
                    if(e != null && (e.isModified() || includeUnModified)) {
                        // Process value for element
                        String value = "";
                        if(e instanceof EBoolean) {
                            if(((EBoolean) e).getValue() == 1) value = "Y";
                            else value = "N";
                        }
                        else if(e instanceof ECheckbox) {
                            for(int check = 0; check < ((ECheckbox) e).getChecked().size(); check++) {
                                String shortValue;
                                if(((ECheckbox) e).getChecked().get(check)) shortValue = "Y";
                                else shortValue = "N";
                                value += shortValue + ", ";
                            }
                        }
                        else if(e instanceof EChooser) value = ((EChooser) e).getValues().get(((EChooser) e).getSelected());
                        else if(e instanceof ECounter) value = String.valueOf(((ECounter) e).getCurrent());
                        else if(e instanceof ESlider) value = String.valueOf(((ESlider) e).getCurrent());
                        else if(e instanceof ETextfield) value = ((ETextfield) e).getText();
                        else if(e instanceof EStopwatch) value = String.valueOf(((EStopwatch) e).getTime());
                        else if(e instanceof EGallery) value = String.valueOf(((EGallery)e).getImageIDs().size());

                        Cell cellValue = row.createCell(match + 1 + ((element) * sorting.size()));
                        cellValue.setCellValue(value);
                        cellValue.setCellStyle(style);

                        matchData.setColumnWidth(match + 1 + ((element) * sorting.size()), 512);
                    } else {
                       row.createCell(match + 1 + ((element) * sorting.size())).setCellStyle(style);
                    }

                }
            }
        }
    }

    // Sheet 2 displays predictions and pit data
    private void genSheet2(XSSFWorkbook wb) {
        XSSFSheet quals = wb.createSheet("QualData");

        /*
         *  Process rows at one and two
         *  Process each element horizontally
         */
        Row titles = quals.createRow(0);
        titles.setHeightInPoints(60f);

        titles.createCell(0);
        Cell teamNum = titles.createCell(0);
        teamNum.setCellValue("Team#");
        teamNum.setCellStyle(teamStyle);

        elementCount = 0;

        // Add titles
        for(int i = 0; i < form.getMatch().size(); i++) {
            XSSFCellStyle style = getStyleForColumn();

            String possibleValues = "";

            Element e = form.getMatch().get(i);
            if(e != null) {
                if(e instanceof EBoolean) possibleValues = "(Y/N)";
                else if(e instanceof ECheckbox) {
                    possibleValues = "(";
                    for(int check = 0; check < ((ECheckbox) e).getChecked().size(); check++) {
                        possibleValues +=" Y/N,";
                    }
                    possibleValues += ")";
                }
                else if(e instanceof EChooser) {
                    possibleValues = "(";
                    for(int item = 0; item < ((EChooser) e).getValues().size(); item++) {
                        if(item == ((EChooser) e).getValues().size() - 1) possibleValues += ((EChooser) e).getValues().get(item) + ")";
                        else possibleValues += ((EChooser) e).getValues().get(item) +"/";
                    }
                }
                else if(e instanceof ECounter) possibleValues = "(#)";
                else if(e instanceof ESlider) possibleValues = "(#)";
                else if(e instanceof EStopwatch) possibleValues = "(Sec)";
            }

            Cell c = titles.createCell(i + 1);
            c.setCellStyle(style);
            c.setCellValue(form.getMatch().get(i).getTitle()+"\n"+possibleValues);
        }

        // Process teams
        for(int i = 0; i < teams.length; i++) {
            elementCount = 0;
            Row r = quals.createRow(i + 1);
            Cell c = r.createCell(0);
            c.setCellStyle(teamStyle);
            c.setCellValue(teams[i].getNumber());

            // Loop through form predictions items
            for(int j = 0; j < form.getMatch().size(); j++) {
                XSSFCellStyle style = getStyleForColumn();

                // Find the right element
                Element e = null;
                for(Element el : teams[i].getItems(1)) if(el.getID() == form.getMatch().get(j).getID()) e = el;

                if(e != null && e.isModified()) {
                    // Process value for element
                    String value = "";
                    if(e instanceof EBoolean) {
                        if(((EBoolean) e).getValue() == 1) value = "Y";
                        else value = "N";
                    }
                    else if(e instanceof ECheckbox) {
                        for(int check = 0; check < ((ECheckbox) e).getChecked().size(); check++) {
                            String shortValue;
                            if(((ECheckbox) e).getChecked().get(check)) shortValue = "Y";
                            else shortValue = "N";
                            value += shortValue + ", ";
                        }
                    }
                    else if(e instanceof EChooser) value = ((EChooser) e).getValues().get(((EChooser) e).getSelected());
                    else if(e instanceof ECounter) value = String.valueOf(((ECounter) e).getCurrent());
                    else if(e instanceof ESlider) value = String.valueOf(((ESlider) e).getCurrent());
                    else if(e instanceof ETextfield) value = ((ETextfield) e).getText();
                    else if(e instanceof EStopwatch) value = String.valueOf(((EStopwatch) e).getTime());
                    else if(e instanceof EGallery) value = String.valueOf(((EGallery)e).getImageIDs().size());

                    Cell cellValue = r.createCell(j + 1);
                    cellValue.setCellValue(value);
                    cellValue.setCellStyle(style);
                } else {
                    r.createCell(j + 1).setCellStyle(style);
                }
            }
        }

        elementCount = 0;

        // Process pit data
        for(int i = 0; i < form.getPit().size() + 1; i++) {
            XSSFCellStyle style = getStyleForColumn();

            String possibleValues = "";

            Element e = null;
            if(i != 0) e = form.getPit().get(i - 1);

            if(e != null) {
                if(e instanceof EBoolean) possibleValues = "(Y/N)";
                else if(e instanceof ECheckbox) {
                    possibleValues = "(";
                    for(int check = 0; check < ((ECheckbox) e).getChecked().size(); check++) {
                        possibleValues +=" Y/N,";
                    }
                    possibleValues += ")";
                }
                else if(e instanceof EChooser) {
                    possibleValues = "(";
                    for(int item = 0; item < ((EChooser) e).getValues().size(); item++) {
                        if(item == ((EChooser) e).getValues().size() - 1) possibleValues += ((EChooser) e).getValues().get(item) + ")";
                        else possibleValues += ((EChooser) e).getValues().get(item) +"/";
                    }
                }
                else if(e instanceof ECounter) possibleValues = "(#)";
                else if(e instanceof ESlider) possibleValues = "(#)";
                else if(e instanceof EStopwatch) possibleValues = "(Sec)";
            }

            Cell title = titles.createCell(form.getMatch().size() + 1 + i);
            title.setCellStyle(style);
            if(i == 0) {
                title.setCellStyle(teamStyle);
                title.setCellValue("<--PREDICTIONS\nPIT-->");
            }
            else title.setCellValue(form.getPit().get(i - 1).getTitle() + "\n" + possibleValues);
        }

        // Process the data!
        for(int i = 0; i < teams.length; i++) {
            elementCount = 0;
            Row r = quals.getRow(i + 1);
            for(int j = 0; j < form.getPit().size() + 1; j++) {
                XSSFCellStyle style = getStyleForColumn();
                if(j == 0) {
                    r.createCell(form.getMatch().size() + 1).setCellStyle(teamStyle);
                    continue;
                }

                Element e = null;
                for(Element el : teams[i].getItems(0)) if(el.getID() == form.getPit().get(j - 1).getID())  e = el;

                if(e != null && e.isModified()) {
                    // Process value for element
                    String value = "";
                    if(e instanceof EBoolean) {
                        if(((EBoolean) e).getValue() == 1) value = "T";
                        else value = "F";
                    }
                    else if(e instanceof ECheckbox) {
                        for(int check = 0; check < ((ECheckbox) e).getChecked().size(); check++) {
                            String shortValue;
                            if(((ECheckbox) e).getChecked().get(check)) shortValue = "T";
                            else shortValue = "F";
                            value += shortValue + ", ";
                        }
                    }
                    else if(e instanceof EChooser) value = ((EChooser) e).getValues().get(((EChooser) e).getSelected());
                    else if(e instanceof ECounter) value = String.valueOf(((ECounter) e).getCurrent());
                    else if(e instanceof ESlider) value = String.valueOf(((ESlider) e).getCurrent());
                    else if(e instanceof ETextfield) value = ((ETextfield) e).getText();
                    else if(e instanceof EStopwatch) value = String.valueOf(((EStopwatch) e).getTime());
                    else if(e instanceof ESTextfield) {
                        if(((ESTextfield)e).isNumberOnly()) value = String.valueOf(teams[i].getNumber());
                        else value = String.valueOf(teams[i].getName());
                    }
                    else if(e instanceof EGallery) value = String.valueOf(((EGallery)e).getImageIDs().size());

                    Cell cellValue = r.createCell(form.getMatch().size() + j + 1);
                    cellValue.setCellValue(value);
                    cellValue.setCellStyle(style);
                } else {
                    r.createCell(form.getMatch().size() + j + 1).setCellStyle(style);
                }

            }

        }
    }

    /*
     * Sheet 3 generates a list that includes all the matches the team is in.
     * If no data can be found, then error is displayed.
     */
    private void genSheet3(XSSFWorkbook wb) {
        XSSFSheet matches = wb.createSheet("OurMatches");

        // We need event key and team key
        RSettings settings = new Loader(activity).loadSettings();
        int teamNumber = settings.getTeamNumber();
        boolean noTeamNumber = false;
        if(teamNumber == 0) {
            Row r = matches.createRow(0);
            Cell c = r.createCell(0);
            c.setCellValue("Your team number has not been set in Roblu settings. Please set it.");
            noTeamNumber = true;
        }
        boolean noEventKey = false;
        if(event.getKey() == null || event.getKey().equals("")) {
            Row r;
            if(noTeamNumber) r = matches.createRow(1);
            else r = matches.createRow(0);
            Cell c = r.createCell(0);
            c.setCellValue("This event does not contain an TBA-API key, please set it in Event settings.");
            noEventKey = true;
        }
        if(noEventKey || noTeamNumber) return;

        XSSFCellStyle redTeam = wb.createCellStyle();
        redTeam.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
        redTeam.setFillForegroundColor(IndexedColors.RED.index);
        setBorder(redTeam);

        XSSFCellStyle blueTeam = wb.createCellStyle();
        blueTeam.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
        blueTeam.setFillForegroundColor(IndexedColors.BLUE.index);
        setBorder(blueTeam);

        Row title = matches.createRow(0);
        title.createCell(0).setCellValue("Match #");
        Cell x;
        x = title.createCell(1); x.setCellValue("Team1"); x.setCellStyle(blueTeam);
        x = title.createCell(2); x.setCellValue("Team2"); x.setCellStyle(blueTeam);
        x = title.createCell(3); x.setCellValue("Team3"); x.setCellStyle(blueTeam);
        x = title.createCell(4); x.setCellValue("Team4"); x.setCellStyle(redTeam);
        x = title.createCell(5); x.setCellValue("Team5"); x.setCellStyle(redTeam);
        x = title.createCell(6); x.setCellValue("Team6"); x.setCellStyle(redTeam);

        // Load matches from TBA-API
        com.cpjd.main.Settings.FIND_TEAM_RANKINGS = false;
        com.cpjd.main.Settings.GET_EVENT_ALLIANCES = false;
        com.cpjd.main.Settings.GET_EVENT_AWARDS = false;
        com.cpjd.main.Settings.GET_EVENT_MATCHES = true;
        com.cpjd.main.Settings.GET_EVENT_STATS = false;
        com.cpjd.main.Settings.GET_EVENT_TEAMS = false;
        com.cpjd.main.Settings.GET_EVENT_WEBCASTS = false;

        TBA tba = new TBA();
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
            StrictMode.setThreadPolicy(policy);

            Event dl = tba.getEvent(event.getKey());
            Match[] dlm = dl.matches;
            int numMatches = 0;
            Cell c;
            for(Match m : dlm) {
                if(m.doesMatchContainTeam(teamNumber) > 0) {
                    Row r = matches.createRow(numMatches + 1);
                    c = r.createCell(0); c.setCellValue(m.match_number);
                    c = r.createCell(1); c.setCellValue(m.blueTeams[0].replace("frc", "")); c.setCellStyle(blueTeam);
                    c = r.createCell(2); c.setCellValue(m.blueTeams[1].replace("frc", "")); c.setCellStyle(blueTeam);
                    c = r.createCell(3); c.setCellValue(m.blueTeams[2].replace("frc", "")); c.setCellStyle(blueTeam);
                    c = r.createCell(4); c.setCellValue(m.redTeams[0].replace("frc", "")); c.setCellStyle(redTeam);
                    c = r.createCell(5); c.setCellValue(m.redTeams[1].replace("frc", "")); c.setCellStyle(redTeam);
                    c = r.createCell(6); c.setCellValue(m.redTeams[2].replace("frc", "")); c.setCellStyle(redTeam);
                    numMatches++;
                }
            }
        } catch(Exception e) {
            matches.getRow(0).getCell(0).setCellValue("Failed to pull data from TBA-API.");
            for(int i = 1; i < 7; i++) matches.getRow(0).getCell(i).setCellValue("");
        }

    }

    // Generates verbose workbook, contains all default values and everything
    private void genSheet5(XSSFWorkbook wb) {
        genSheet1(wb, true, true);
    }

    private XSSFCellStyle teamStyle;
    private XSSFCellStyle value1Style, value2Style, value3Style, value4Style;

    private XSSFCellStyle getStyleForColumn() {
        XSSFCellStyle temp = null;

        switch(elementCount) {
            case 0:
                temp = value1Style;
                break;
            case 1:
                temp = value2Style;
                break;
            case 2:
                temp = value3Style;
                break;
            case 3:
                temp = value4Style;
                break;
        }

        elementCount++;
        if(elementCount > 3) elementCount = 0;
        return temp;
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
