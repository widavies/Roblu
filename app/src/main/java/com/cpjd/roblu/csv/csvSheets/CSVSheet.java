package com.cpjd.roblu.csv.csvSheets;

import com.cpjd.roblu.csv.RMatch;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RBoolean;
import com.cpjd.roblu.models.metrics.RCheckbox;
import com.cpjd.roblu.models.metrics.RChooser;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RSlider;
import com.cpjd.roblu.models.metrics.RStopwatch;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import lombok.Getter;
import lombok.Setter;

/**
 * If you'd like to add a custom Excel sheet to Roblu's export function, you've made it to the right place!
 * This abstract class defines how a new sheet should be mapped. Roblu uses the Apache POI library (https://poi.apache.org/)
 * to write data to Microsoft files. Google spreadsheets supports the import of Microsoft Excel files. So this library will
 * cover exports to just about anything. With Excel or Spreadsheets, you should easily be able to export a .CSV for more
 * functionality in other programs.
 *
 * Now, for more information on this class:
 * -Create a child class in the csv.sheets package, extend this package
 * -Write your mapping code in the generateSheet() method, use XSSFSheet sheet to map values to cells,
 *  you'll notice that you have access to the full RTeam[] teams array for scouting data and team data.
 *  This array is sorting and verified, so it will contain all metrics, even non-modified ones.
 * -If you need help, don't hesitate to check the other child sheets in this package, if help is still needed,
 *  send me an email at wdavies973@gmail.com
 *  -CSVSheet is automatically thrown in a Thread for you, each sheet subclass is generated in its own thread, for maximum performance.
 *  However, you don't need to worry about any of that. (Note: Only generateSheet is run within a thread)
 *
 *  @version 1
 *  @since 4.0.0
 *  @author Will Davies
 *
 */
public abstract class CSVSheet {

    /**
     * Reference to the loader object if needed
     */
    @Setter
    protected IO io;

    /**
     * Workbook access will be automatically provided by ExportCSVTask,
     * it's also used by createCellStyle();
     */
    @Setter
    private XSSFWorkbook workbook;

    /**
     * Stores a cell style
     */
    @Getter
    @Setter
    private XSSFCellStyle style;

    private int currentRowNum = -1;

    /**
     * Generates the sheet in its own thread
     * @param sheet add your data to this sheet with its sub methods, look at Apache POI API documentation if you need help learning how to map stuff
     * @param teams access to all the scouting data
     */
    public abstract void generateSheet(XSSFSheet sheet, REvent event, RForm form, RTeam[] teams, RMatch[] matches);

    /**
     * Returns the name of this sheet
     * @return the String name of this sheet
     */
    public abstract String getSheetName();

    /**
     * Specifies whether this sheet should be generated and added to the spreadsheets file
     * @return true to add this sheet to the .CSV file
     */
    public abstract boolean isEnabled();

    /**
     * Returns the uniform pixel width of the columns in the sheet
     * @return the pixel width to set to each column
     */
    public abstract int getColumnWidth();

    /*
     * Utility methods - call this from the sub class as you please
     */

    /**
     * Creates a new row
     * @param sheet a reference to the sheet you're working in
     * @return a new row at the bottom of your sheet
     */
    Row createRow(XSSFSheet sheet) {
        currentRowNum++;
        return sheet.createRow(currentRowNum);
    }

    /**
     * Creates a new row
     * @param sheet a reference to the sheet you're working in
     * @param height the height of the row
     * @return a new row at the bottom of your sheet
     */
    Row createRow(XSSFSheet sheet, float height) {
        currentRowNum++;
        Row row = sheet.createRow(currentRowNum);
        row.setHeightInPoints(height);
        return row;
    }

    /**
     * Creates a new cell within a row
     * @param row the row to create the cell in
     * @param cellIndex the horizontal index of the cell, with 0 being the far left most
     * @param cellText the value to set in the cell
     */
    void createCell(Row row, int cellIndex, String cellText) {
        Cell cell = row.createCell(cellIndex);
        cell.setCellStyle(style);
        cell.setCellValue(cellText);
    }

    /**
     * Sets the style for all cells. Whenever createCell(); is called, the attributes set by this
     * method will automatically be applied to the cell
     *
     * Note: If you never set this method, the default is white background, black text, thin border, and no bold.
     * @param borderStyle the style of the borders, BorderStyle.THIN is recommended
     * @param cellColor the background color the cell, example: IndexedColors.WHITE
     * @param fontColor the font color of the cell, example: IndexedColors.BLACK
     * @param bold true if the text should be bold
     * @return style this can be reset with setStyle() also if you don't want to continuously keep creating styles
     */
    public XSSFCellStyle setCellStyle(BorderStyle borderStyle, IndexedColors cellColor, IndexedColors fontColor, boolean bold) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setBorderRight(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setBorderBottom(borderStyle);
        style.setBorderTop(borderStyle);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(cellColor.index);
        Font font = workbook.createFont();
        font.setColor(fontColor.index);
        font.setBold(bold);
        style.setFont(font);
        this.style = style;
        return style;
    }

    /**
     * Gets the possible values for a metric, so the user can expect what values to look for
     * @param metric the metric to analyze
     * @return a short string identifying the types of data the metric contains, for example, RCounter would return (#) because it stores a number
     */
    String getPossibleValuesForMetric(RMetric metric) {
        if(metric == null) return "";

        if(metric instanceof RBoolean) return "\n(Y/N)";
        else if(metric instanceof RCheckbox) {
            StringBuilder builder = new StringBuilder("\n(");
            for(int i = 0; i < ((RCheckbox) metric).getValues().size(); i++) builder.append(" Y / N, ");
            builder.replace(builder.toString().length() -1, builder.toString().length()-1, ""); // get rid of the last comma
            builder.append(")");
            return builder.toString();
        }
        else if(metric instanceof RChooser) {
            StringBuilder builder = new StringBuilder("\n(");
            for(String s : ((RChooser) metric).getValues()) {
                builder.append(s).append(" / ");
            }
            builder.replace(builder.toString().length() -2, builder.toString().length()-2, ""); // get rid of the last comma
            builder.append(")");
            return builder.toString();
        }
        else if(metric instanceof RCounter) return  "\n(#)";
        else if(metric instanceof RSlider) return "\n(#)";
        else if(metric instanceof RStopwatch) return  "\n(Sec)";
        else return "";
    }

}
