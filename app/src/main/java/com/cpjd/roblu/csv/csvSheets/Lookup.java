package com.cpjd.roblu.csv.csvSheets;

import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RMetric;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.ArrayList;

public class Lookup extends CSVSheet {

    @Override
    public void generateSheet(XSSFSheet sheet, REvent event, RForm form, RTeam[] teams, ArrayList<RCheckout> checkouts) {
        Row one = createRow(sheet);
        one.createCell(0).setCellValue("Match Number");
        Row two = createRow(sheet);
        two.createCell(0).setCellValue(1);

        int maxListLength = checkouts.size();

        // Team's row
        two.createCell(4).setCellFormula("VLOOKUP($A$2,MatchList!$A$2:$G$"+maxListLength+",2,FALSE)");
        two.createCell(5).setCellFormula("VLOOKUP($A$2,MatchList!$A$2:$G$"+maxListLength+",3,FALSE)");
        two.createCell(6).setCellFormula("VLOOKUP($A$2,MatchList!$A$2:$G$"+maxListLength+",4,FALSE)");
        two.createCell(7).setCellFormula("VLOOKUP($A$2,MatchList!$A$2:$G$"+maxListLength+",5,FALSE)");
        two.createCell(8).setCellFormula("VLOOKUP($A$2,MatchList!$A$2:$G$"+maxListLength+",6,FALSE)");
        two.createCell(9).setCellFormula("VLOOKUP($A$2,MatchList!$A$2:$G$"+maxListLength+",7,FALSE)");

        int maxWidth = form.getMatch().size() + 2;
        String columnLetter = CellReference.convertNumToColString(maxWidth);

        // Load metrics
        int index = 0;
        for(RMetric metric : form.getMatch()) {
            Row row = createRow(sheet);
            createCell(row, 3, metric.getTitle());
            row.createCell(4).setCellFormula("VLOOKUP(E$2,'MatchData'!$A$2:$"+columnLetter+"$"+maxListLength+","+(index + 3)+",FALSE)");
            row.createCell(5).setCellFormula("VLOOKUP(F$2,'MatchData'!$A$2:$"+columnLetter+"$"+maxListLength+","+(index + 3)+",FALSE)");
            row.createCell(6).setCellFormula("VLOOKUP(G$2,'MatchData'!$A$2:$"+columnLetter+"$"+maxListLength+","+(index + 3)+",FALSE)");
            row.createCell(7).setCellFormula("VLOOKUP(H$2,'MatchData'!$A$2:$"+columnLetter+"$"+maxListLength+","+(index + 3)+",FALSE)");
            row.createCell(8).setCellFormula("VLOOKUP(I$2,'MatchData'!$A$2:$"+columnLetter+"$"+maxListLength+","+(index + 3)+",FALSE)");
            row.createCell(9).setCellFormula("VLOOKUP(J$2,'MatchData'!$A$2:$"+columnLetter+"$"+maxListLength+","+(index + 3)+",FALSE)");
            index++;
        }
    }

    @Override
    public String getSheetName() {
        return "Lookup";
    }
    @Override
    public int getColumnWidth() {
        return 3000;
    }
}
