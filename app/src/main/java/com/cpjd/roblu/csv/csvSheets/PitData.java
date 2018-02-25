package com.cpjd.roblu.csv.csvSheets;

import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RStopwatch;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.ArrayList;

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
    public void generateSheet(XSSFSheet sheet, REvent event, RForm form, RTeam[] teams, ArrayList<RCheckout> checkouts) {
        /*
         * Create some styles
         */
        XSSFCellStyle[] styles = {setCellStyle(BorderStyle.THIN, IndexedColors.CORAL, IndexedColors.BLACK, false), setCellStyle(BorderStyle.THIN, IndexedColors.LIGHT_GREEN, IndexedColors.BLACK, false),
                setCellStyle(BorderStyle.THIN, IndexedColors.GREY_50_PERCENT, IndexedColors.BLACK, false), setCellStyle(BorderStyle.THIN, IndexedColors.CORNFLOWER_BLUE, IndexedColors.BLACK, false)};

        /*
         * Create row 1 (predictions metric names)
         */
        Row metricNames = createRow(sheet, 60);
        setCellStyle(BorderStyle.THIN, IndexedColors.BLACK, IndexedColors.WHITE, true);
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
         * Add divider
         */
        setCellStyle(BorderStyle.THIN, IndexedColors.BLACK, IndexedColors.WHITE, true);
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
         * Start adding scouting data
         */
        for(RTeam team : teams) {
            Row row = createRow(sheet);

            // Team number column
            setCellStyle(BorderStyle.THIN, IndexedColors.BLACK, IndexedColors.WHITE, true);
            getStyle().setAlignment(HorizontalAlignment.RIGHT);
            createCell(row, 0, String.valueOf(team.getNumber()));

            /*
             * Add scouting data
             */

            // Loop through the form metrics array
            for(int i = 0; i < form.getMatch().size(); i++) {

                // Set style
                setStyle(styles[i % styles.length]);

                // If the team contains the match and the metric is modified, add it to the excel sheet
                // We found the match, let's quickly find the metric
                RMetric teamMetric = null;
                for(RMetric metric : team.getTabs().get(1).getMetrics()) {
                    if(metric.getID() == form.getMatch().get(i).getID()) {
                        teamMetric = metric;
                        break;
                    }
                }

                if(shouldWriteMetric(team, teamMetric)) {
                    if(teamMetric instanceof RStopwatch) createCell(row, 1 + i, ((RStopwatch) teamMetric).getLapsString());
                    else createCell(row, 1 + i, teamMetric.toString());
                }
                else createCell(row, 1 + i, "");
            }

            // Add divider column
            setCellStyle(BorderStyle.THIN, IndexedColors.BLACK, IndexedColors.WHITE, true);
            getStyle().setAlignment(HorizontalAlignment.RIGHT);
            createCell(row, 1 + form.getMatch().size(), "");

            // Loop through the pit metrics array
            for(int i = 0; i < form.getPit().size(); i++) {

                // Set style
                setStyle(styles[i % styles.length]);

                RMetric teamMetric = null;
                for(RMetric metric : team.getTabs().get(0).getMetrics()) {
                    if(metric.getID() == form.getPit().get(i).getID()) {
                        teamMetric = metric;
                        break;
                    }
                }

                // always add the team name and number fields, even if they aren't modified
                if(teamMetric.getID() == 0) {
                    createCell(row, 2 + form.getMatch().size() + i, team.getName());
                    continue;
                }
                if(teamMetric.getID() == 1) {
                    createCell(row, 2 + form.getMatch().size() + i, String.valueOf(team.getNumber()));
                    continue;
                }

                if(shouldWriteMetric(team, teamMetric)) {
                    if(teamMetric instanceof RStopwatch) createCell(row, 2 + i + form.getMatch().size(), ((RStopwatch) teamMetric).getLapsString());
                    else createCell(row, 2 + i + form.getMatch().size(), teamMetric.toString());
                }
                else createCell(row, 2 + i + form.getMatch().size(), "");
            }
        }

    }

    @Override
    public String getSheetName() {
        return "PitData";
    }

    @Override
    public int getColumnWidth() {
        return 2000;
    }
}
