package com.cpjd.roblu.csv.csvSheets;

import android.util.Log;

import com.cpjd.roblu.csv.RMatch;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;

/**
 * MatchData writes scouting data to a spreadsheet.
 * Note:
 * -Matches are not included that don't contain data
 * -Scouting data that is "Not observed" (metric.isModified() == false) is not included, instead it's left blank, this means
 * that values that appear on this spreadsheet all have been recorded by one scouter or another
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class MatchData extends CSVSheet {

    @Override
    public void generateSheet(XSSFSheet sheet, REvent event, RForm form, RTeam[] teams, RMatch[] matches) {
        Log.d("RBS", "Matches: "+matches.length);

        /*
         * Create the first two rows of the match data sheet:
         * -Metric titles
         * -Match names
         */

        Row metricTitles = createRow(sheet, 0, 30f);
        Row matchNames = createRow(sheet, 1);
        setCellStyle(BorderStyle.THIN, IndexedColors.BLACK, IndexedColors.WHITE, false);
        createCell(metricTitles, 0, "Team#");
        /*
         * Create some styles
         */
        XSSFCellStyle[] styles = {setCellStyle(BorderStyle.THIN, IndexedColors.CORNFLOWER_BLUE, IndexedColors.BLACK, false), setCellStyle(BorderStyle.THIN, IndexedColors.GREEN, IndexedColors.BLACK, false),
                setCellStyle(BorderStyle.THIN, IndexedColors.GREY_50_PERCENT, IndexedColors.BLACK, false), setCellStyle(BorderStyle.THIN, IndexedColors.RED, IndexedColors.BLACK, false)};

        /*
         * Write metric titles
         */

        int styleCounter = 0;
        for(int i = 0; i < form.getMatch().size(); i++) {
            // decide the which style to use
            styleCounter++;
            if(styleCounter % matches.length == 0) {
                styleCounter = 0;
                setStyle(styles[styleCounter]);
            }

            // Set alignment and create cells
            getStyle().setAlignment(HorizontalAlignment.CENTER); // set alignment of text
            createCell(metricTitles, i * matches.length + 1, form.getMatch().get(i).getTitle()+getPossibleValuesForMetric(form.getMatch().get(i)));

            // merge the title cells into one cell
             sheet.addMergedRegion(new CellRangeAddress(0,0,1 + ((i) * matches.length),matches.length + ((i) * matches.length)));
        }
        //

        /*
         * Write match titles
         */
        styleCounter = 0;
        for(int i = 0, j = 0; i < form.getMatch().size() * matches.length; i++, j++) {
            if(j == matches.length) j = 0;

            // decide the which style to use
            styleCounter++;
            if(styleCounter % matches.length == 0) {
                styleCounter = 0;
                setStyle(styles[styleCounter]);
            }

            createCell(matchNames, i + 1, matches[j].getAbbreviatedName());
        }

        if(true) return;

        /*
         * Now, add scouting data to the cells
         * (Also add team numbers along the left side
         */
        for(int i = 0; i < teams.length; i++) {
            // Add the team number to the leftmost column
            setCellStyle(BorderStyle.THIN, IndexedColors.BLACK, IndexedColors.WHITE, true);
            Row teamsNumbers = sheet.createRow(i + 2);
            createCell(teamsNumbers, 0, String.valueOf(teams[i].getNumber()));

            // add scouting data
            for(int j = 0; j < form.getMatch().size(); j++) {
                // decide the which style to use
                if(styleCounter % 4 == 0) {
                    styleCounter = 0;
                    setStyle(styles[styleCounter]);
                }
                styleCounter++;

                for(int k = 0; k < matches.length; k++) {
                    // Search for the match within the team
                    boolean matchFound = false;
                    for(int l = 0; l < teams[i].getTabs().size(); l++) {
                        if(teams[i].getTabs().get(l).getTitle().equalsIgnoreCase(matches[k].getMatchName())) {
                            // Match found
                            if(teams[i].getTabs().get(l).getMetrics().get(k).isModified()) {
                                createCell(teamsNumbers, k + 1 + ((j) * matches.length), teams[i].getTabs().get(l).getMetrics().get(k).toString());
                                matchFound = true;
                                break;
                            }
                        }
                    }
                    // If match wasn't found, just add an empty cell
                    if(!matchFound) createCell(teamsNumbers,k + 1 + ((j) * matches.length), "");
                }

            }
        }
    }

    @Override
    public String getSheetName() {
        return "MatchData";
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
