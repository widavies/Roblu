package com.cpjd.roblu.csv.csvSheets;


import android.os.StrictMode;

import com.cpjd.main.TBA;
import com.cpjd.models.standard.Match;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.ArrayList;

/**
 * Our matches displays a list of matches the team is with the help of TheBlueAlliance.com
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class OurMatches extends CSVSheet {

    @Override
    public void generateSheet(XSSFSheet sheet, REvent event, RForm form, RTeam[] teams, ArrayList<RCheckout> checkouts) {
        if(event.getKey() == null || event.getKey().equalsIgnoreCase("") || io.loadSettings().getTeamNumber() == 0) return;

        int teamNumber = io.loadSettings().getTeamNumber();

        // Allow this thread to access the internet
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        XSSFCellStyle blue = setCellStyle(BorderStyle.THIN, IndexedColors.CORNFLOWER_BLUE, IndexedColors.BLACK, false);
        XSSFCellStyle red = setCellStyle(BorderStyle.THIN, IndexedColors.CORAL, IndexedColors.BLACK, false);

        /*
         * Create header row
         */
        Row one = createRow(sheet, 0);
        setCellStyle(BorderStyle.THIN, IndexedColors.WHITE, IndexedColors.BLACK, true);
        createCell(one, 0, "Match#");
        setStyle(blue);
        createCell(one, 1, "Team1");
        createCell(one, 2, "Team2");
        createCell(one, 3, "Team3");
        setStyle(red);
        createCell(one, 4, "Team4");
        createCell(one, 5, "Team5");
        createCell(one, 6, "Team6");

        // Determine event year
        //Settings.disableAll();
        //Settings.GET_EVENT_MATCHES = true;
        try {
            Match[] matches = new TBA().getMatches(event.getKey());
            for(Match m : matches) {
                if(doesMatchContainTeam(m, teamNumber)) {
                    Row row = createRow(sheet);
                    setCellStyle(BorderStyle.THIN, IndexedColors.WHITE, IndexedColors.BLACK, true);
                    createCell(row, 0, String.valueOf(m.getMatchNumber()));
                    setStyle(blue);
                    createCell(row, 1, m.getBlue().getTeamKeys()[0].replace("frc", ""));
                    createCell(row, 2, m.getBlue().getTeamKeys()[0].replace("frc", ""));
                    createCell(row, 3, m.getBlue().getTeamKeys()[0].replace("frc", ""));
                    setStyle(red);
                    createCell(row, 4, m.getRed().getTeamKeys()[0].replace("frc", ""));
                    createCell(row, 5, m.getRed().getTeamKeys()[0].replace("frc", ""));
                    createCell(row, 6, m.getRed().getTeamKeys()[0].replace("frc", ""));
                }
            }
        } catch(Exception e) {
            Row r = createRow(sheet);
            createCell(r, 0, "Failed to pull data from TheBlueAlliance.com.");
        }
    }

    @Override
    public String getSheetName() {
        return "OurMatches";
    }

    @Override
    public int getColumnWidth() {
        return 2000;
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
