package com.cpjd.roblu.csv.csvSheets;

import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.ArrayList;

public class MatchList extends CSVSheet {
    @Override
    public void generateSheet(XSSFSheet sheet, REvent event, RForm form, RTeam[] teams, ArrayList<RCheckout> checkouts) {
        Row labels = createRow(sheet);
        createCell(labels, 0, "Match Number");
        createCell(labels, 1, "Red1");
        createCell(labels, 2, "Red2");
        createCell(labels, 3, "Red3");
        createCell(labels, 4, "Blue1");
        createCell(labels, 5, "Blue2");
        createCell(labels, 6, "Blue3");

        int teamNumber = io.loadSettings().getTeamNumber();

        /*
         * Start writing match numbers
         */
        int index = 0;
        String currentMatch = "";
        Row row = null;
        for(RCheckout checkout : checkouts) {
            if(checkout.getTeam().getTabs().get(0).getTitle().equalsIgnoreCase("PIT") || checkout.getTeam().getTabs().get(0).getTitle().equalsIgnoreCase("PREDICTIONS")) continue;

            if(!currentMatch.equals(checkout.getTeam().getTabs().get(0).getTitle())) row = createRow(sheet);

            try {
                Cell c = row.createCell(0);
                c.setCellValue(Integer.parseInt(checkout.getTeam().getTabs().get(0).getTitle().replace("Quals ", "")));
                c.setCellStyle(getStyle());
            } catch(NumberFormatException e) {
                Cell c = row.createCell(0);
                c.setCellValue(checkout.getTeam().getTabs().get(0).getTitle().replace("Quals ", ""));
                c.setCellStyle(getStyle());
            }

            int pos = checkout.getTeam().getTabs().get(0).getAlliancePosition();
            if(pos == -1) pos = index + 1;

            if(checkout.getTeam().getNumber() == teamNumber) getStyle().getFont().setBold(true);
            else getStyle().getFont().setBold(false);

            Cell cs = row.createCell(pos);
            cs.setCellValue(checkout.getTeam().getNumber());
            cs.setCellStyle(getStyle());

            currentMatch = checkout.getTeam().getTabs().get(0).getTitle();
            index++;
        }
    }

    @Override
    public String getSheetName() {
        return "MatchList";
    }

    @Override
    public int getColumnWidth() {
        return 2000;
    }
}
