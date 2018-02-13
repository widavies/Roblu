package com.cpjd.roblu.csv.csvSheets;

import com.cpjd.roblu.csv.RMatch;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RStopwatch;

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

        if(matches == null || matches.length == 0) return;

        /*
         * Create the first two rows of the match data sheet:
         * -Metric titles
         * -Match names
         */

        Row metricTitles = createRow(sheet,  30f);
        Row matchNames = createRow(sheet, 1);
        setCellStyle(BorderStyle.THIN, IndexedColors.BLACK, IndexedColors.WHITE, false);
        createCell(metricTitles, 0, "");
        createCell(matchNames, 0, "Team#");
        /*
         * Create some styles to help the user distinguish columns
         */
        XSSFCellStyle[] styles = {setCellStyle(BorderStyle.THIN, IndexedColors.CORAL, IndexedColors.BLACK, false), setCellStyle(BorderStyle.THIN, IndexedColors.LIGHT_GREEN, IndexedColors.BLACK, false),
                setCellStyle(BorderStyle.THIN, IndexedColors.GREY_50_PERCENT, IndexedColors.BLACK, false), setCellStyle(BorderStyle.THIN, IndexedColors.CORNFLOWER_BLUE, IndexedColors.BLACK, false)};

        /*
         * Write metric titles
         */
        for(int i = 0; i < form.getMatch().size(); i++) {
            setStyle(styles[i % styles.length]);

            // Set alignment and create cells
            getStyle().setAlignment(HorizontalAlignment.CENTER); // set alignment of text
            createCell(metricTitles, i * matches.length + 1, form.getMatch().get(i).getTitle()+getPossibleValuesForMetric(form.getMatch().get(i)));

            // merge the title cells into one cell
             sheet.addMergedRegion(new CellRangeAddress(0,0,1 + ((i) * matches.length),matches.length + ((i) * matches.length)));
        }

        /*
         * Write match titles
         */
        setStyle(styles[0]); // set the starting style
        for(int i = 0, j = 0, k = 0, styleCounter = 1; i < form.getMatch().size() * matches.length; i++, j++, k++) {
            if(styleCounter == styles.length) styleCounter = 0;
            if(k == matches.length) {
                k = 0;
                setStyle(styles[styleCounter]);
                styleCounter++;
            }
            if(j == matches.length) j = 0;

            createCell(matchNames, i + 1, matches[j].getAbbreviatedName());
        }

        /*
         * Write scouting data to the spreadsheet
         * Also add the team number to the far side
         */
        for(RTeam team : teams) {
            // Add the team number
            setCellStyle(BorderStyle.THIN, IndexedColors.BLACK, IndexedColors.WHITE, true);
            getStyle().setAlignment(HorizontalAlignment.RIGHT);
            Row row = createRow(sheet);
            createCell(row, 0, String.valueOf(team.getNumber()));

            /*
             * Add scouting data
             */

            // Loop through the form metrics array
            for(int i = 0; i < form.getMatch().size(); i++) {

                // Set style
                setStyle(styles[i % styles.length]);

                // Loop through the matches array
                for(int j = 0; j < matches.length; j++) {
                    /*
                     * Alright, we have a requested match and metric,
                     * this is enough information to determine if:
                     * A) This team contains the match
                     * B) This team contains a modified metric within the match
                     */
                    for(int k = 0; k < team.getTabs().size(); k++) {
                        // If the team contains the match and the metric is modified, add it to the excel sheet
                        if(team.getTabs().get(k).getTitle().equalsIgnoreCase(matches[j].getMatchName())) {
                            // We found the match, let's quickly find the metric
                            RMetric teamMetric = null;
                            for(RMetric metric : team.getTabs().get(k).getMetrics()) {
                                if(metric.getID() == form.getMatch().get(i).getID()) {
                                    teamMetric = metric;
                                    break;
                                }
                            }

                            if(teamMetric.isModified()) {
                                /*
                                 * Stopwatch is a special case, check for that first
                                 */
                                if(teamMetric instanceof RStopwatch) {
                                    StringBuilder data = new StringBuilder("(");
                                    if(((RStopwatch) teamMetric).getTimes() != null) {

                                        for(int l = 0; l < ((RStopwatch) teamMetric).getTimes().size(); l++) {
                                            if(l != ((RStopwatch) teamMetric).getTimes().size() - 1) data.append("s, ");
                                            else data.append("s)");
                                        }
                                        createCell(row, 1 + (matches.length * i) + j, data.toString());
                                    }
                                }

                                createCell(row, 1 + (matches.length * i) + j, teamMetric.toString());
                            } else createCell(row, 1 + (matches.length * i) + j, ""); // Still add a cell so the formatting applies
                            break;
                        } else {
                            createCell(row, 1 + (matches.length * i) + j, ""); // Still add a cell so the formatting applies
                        }
                    }
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
        return 2000;
    }
}
