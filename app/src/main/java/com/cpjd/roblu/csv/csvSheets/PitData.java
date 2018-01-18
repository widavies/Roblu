package com.cpjd.roblu.csv.csvSheets;

import com.cpjd.roblu.csv.RMatch;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;

/**
 * PitData generates an spreadsheet displaying PIT and Predictions data.
 * As in MatchData, only modified metrics will be displayed.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class PitData extends CSVSheet {
    @Override
    public void generateSheet(XSSFSheet sheet, REvent event, RForm form, RTeam[] teams, RMatch[] matches) {
        /*
         * Create some styles
         */
        XSSFCellStyle[] styles = {setCellStyle(BorderStyle.THIN, IndexedColors.CORNFLOWER_BLUE, IndexedColors.BLACK, false), setCellStyle(BorderStyle.THIN, IndexedColors.GREEN, IndexedColors.BLACK, false),
                setCellStyle(BorderStyle.THIN, IndexedColors.GREY_50_PERCENT, IndexedColors.BLACK, false), setCellStyle(BorderStyle.THIN, IndexedColors.RED, IndexedColors.BLACK, false)};


        /*
         * Create row one (metric names)
         */
        Row metricNames = createRow(sheet, 60);
        createCell(metricNames, 0, "Team#");
        int styleCounter = 0;
        for(int i = 0; i < form.getMatch().size(); i++) {
            // decide the which style to use
            if(styleCounter == 4) styleCounter = 0;
            setStyle(styles[styleCounter]);
            styleCounter++;

            createCell(metricNames, i + 1, form.getMatch().get(i).getTitle()+getPossibleValuesForMetric(form.getMatch().get(i)));
        }
        /*
         * Add divider indicator
         */
        createCell(metricNames, form.getMatch().size() + 1, "<--PREDICTIONS\nPIT-->");
        /*
         * Add pit metric names
         */
        styleCounter = 0;
        for(int i = 0; i < form.getPit().size(); i++) {
            // decide the which style to use
            if(styleCounter == 4) styleCounter = 0;
            setStyle(styles[styleCounter]);
            styleCounter++;

            createCell(metricNames, i + form.getMatch().size() + 2, form.getPit().get(i).getTitle()+getPossibleValuesForMetric(form.getPit().get(i)));
        }
        /*
         * Process predictions and pit scouting data
         */
        styleCounter = 0;
        for(RTeam team : teams) {
            // decide the which style to use
            if(styleCounter == 4) styleCounter = 0;
            setStyle(styles[styleCounter]);
            styleCounter++;

            Row teamsRow = createRow(sheet, 0);
            createCell(teamsRow, 0, team.getName());
            for(int j = 0; j < team.getTabs().get(1).getMetrics().size(); j++) { // use getTabs().get(1) for predictions tab
                if(team.getTabs().get(1).getMetrics().get(j).isModified()) createCell(teamsRow, j + 1, team.getTabs().get(1).getMetrics().get(j).toString());
                else createCell(teamsRow, j + 1, "");
            }
            // Create black divider cell
            setCellStyle(BorderStyle.THIN, IndexedColors.BLACK, IndexedColors.WHITE, false);
            createCell(teamsRow, team.getTabs().get(1).getMetrics().size() + 2, "");

            // Process pit
            for(int j = 0; j < team.getTabs().get(0).getMetrics().size(); j++) { // use getTabs().get(0) for PIT tab
                if(team.getTabs().get(0).getMetrics().get(j).isModified()) createCell(teamsRow, team.getTabs().get(1).getMetrics().size() + 3 + j, team.getTabs().get(0).getMetrics().get(j).toString());
                else createCell(teamsRow, team.getTabs().get(1).getMetrics().size() + 3 + j, "");
            }
        }

    }

    @Override
    public String getSheetName() {
        return "QualData";
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
