package com.cpjd.roblu.csv.sheets;

import com.cpjd.roblu.csv.RMatch;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;

public class MatchData extends Sheet {

    @Override
    public void generateSheet(XSSFSheet sheet, RForm form, RTeam[] teams, RMatch[] matches) {
        /*
         * Create the first two rows of the match data sheet:
         * -Metric titles
         * -Match names
         */

        Row metricTitles = createRow(sheet, 30f);
        Row matchNames = createRow(sheet);
        createCell(metricTitles, 0, "Team#");

        /*
         * Create some styles
         */
        XSSFCellStyle[] styles = {setCellStyle(BorderStyle.THIN, IndexedColors.CORNFLOWER_BLUE, IndexedColors.BLACK, false), setCellStyle(BorderStyle.THIN, IndexedColors.GREEN, IndexedColors.BLACK, false),
                setCellStyle(BorderStyle.THIN, IndexedColors.GREY_50_PERCENT, IndexedColors.BLACK, false), setCellStyle(BorderStyle.THIN, IndexedColors.RED, IndexedColors.BLACK, false)};

        /*
         * Write metric titles to the array
         */
        int styleCounter = 0;
        for(int i = 0; i < matches.length; i++) {
            getStyle().setAlignment(HorizontalAlignment.CENTER); // set alignment of text
            createCell(metricTitles, i + 1, matches[i].getAbbreviatedName()+getPossibleValuesForMetric(form.getMatch().get(i)));

            // decide the which style to use
            if(styleCounter % 4 == 0) {
                styleCounter = 0;
                setStyle(styles[styleCounter]);
            }

            styleCounter++;
            // merge the title cells into one cell
            sheet.addMergedRegion(new CellRangeAddress(0,0,1 + ((i) * matches.length),matches.length + ((i) * matches.length)));
        }

        /*
         * Now, add metric data to the cells
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
                        else if(e instanceof EGallery) value = String.valueOf(((EGallery)e).getImages().size());

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

    @Override
    public String getSheetName() {
        return "MatchData";
    }

    @Override
    public boolean enabled() {
        return true;
    }
}
