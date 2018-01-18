package com.cpjd.roblu.csv.csvSheets;


import com.cpjd.main.Settings;
import com.cpjd.main.TBA;
import com.cpjd.models.Match;
import com.cpjd.roblu.csv.RMatch;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;

/**
 * Our matches displays a list of matches the team is with the help of TheBlueAlliance.com
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class OurMatches extends CSVSheet {

    @Override
    public void generateSheet(XSSFSheet sheet, REvent event, RForm form, RTeam[] teams, RMatch[] matches) {
        if(event.getKey() == null || event.getKey().equalsIgnoreCase("") || io.loadSettings().getTeamNumber() == 0) return;

        // Determine event year
        Settings.disableAll();
        int year = (int)new TBA().getEvent(event.getKey()).year;

        Match[] tbaMatches = new TBA().getTeamEventMatches(year, event.getKey(), io.loadSettings().getTeamNumber());

        /*
         * Create header row
         */
        Row one = createRow(sheet, 0);
        createCell(one, 0, "Match#");
        createCell(one, 1, "Team1");
        createCell(one, 2, "Team2");
        createCell(one, 3, "Team3");
        createCell(one, 4, "Team4");
        createCell(one, 5, "Team5");
        createCell(one, 6, "Team6");

        /*
         * Load match data
         */
        int teamNumber = io.loadSettings().getTeamNumber();
        for(Match tbaMatche : tbaMatches) {
            Row row = createRow(sheet, 1);
            setCellStyle(BorderStyle.THIN, IndexedColors.WHITE, IndexedColors.BLACK, true);
            createCell(row, 0, String.valueOf(tbaMatche.match_number));
            for(int j = 0; j < tbaMatche.blueTeams.length; j++) {
                setCellStyle(BorderStyle.THIN, IndexedColors.BLUE, IndexedColors.BLACK, tbaMatche.blueTeams[j].replace("frc", "").equals(String.valueOf(teamNumber)));
                createCell(row, j + 1, tbaMatche.blueTeams[j].replace("frc", ""));
            }
            for(int j = 0; j < tbaMatche.redTeams.length; j++) {
                setCellStyle(BorderStyle.THIN, IndexedColors.RED, IndexedColors.BLACK, tbaMatche.redTeams[j].replace("frc", "").equals(String.valueOf(teamNumber)));
                createCell(row, j + 4, tbaMatche.redTeams[j].replace("frc", ""));
            }
        }

    }

    @Override
    public String getSheetName() {
        return "OurMatches";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public int getColumnWidth() {
        return 512;
    }
}
