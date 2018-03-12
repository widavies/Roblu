package com.cpjd.roblu.csv.csvSheets;

import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RCalculation;
import com.cpjd.roblu.models.metrics.RFieldData;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RStopwatch;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.ArrayList;

public class MatchData extends CSVSheet {

    @Override
    public void generateSheet(XSSFSheet sheet, REvent event, RForm form, RTeam[] teams, ArrayList<RCheckout> checkouts) {
        /*
         * Output the headers
         */

        Row row = createRow(sheet);
        createCell(row, 0, "Team Number");
        createCell(row, 1, "Match number");

        for(int i = 0; i < form.getMatch().size(); i++) {
            createCell(row, i + 2, form.getMatch().get(i).getTitle());
        }

        /*
         * Output the data
         */
        for(RCheckout checkout : checkouts) {
            if(checkout.getTeam().getTabs().get(0).getTitle().equalsIgnoreCase("PIT") || checkout.getTeam().getTabs().get(0).getTitle().equalsIgnoreCase("PREDICTIONS")) continue;

            Row data = createRow(sheet);
            Cell cs = data.createCell(0);
            cs.setCellValue(checkout.getTeam().getNumber());
            cs.setCellStyle(getStyle());

            createCell(data, 1, checkout.getTeam().getTabs().get(0).getTitle().replace("Quals ", ""));
            int index = 0;
            for(RMetric metric : checkout.getTeam().getTabs().get(0).getMetrics()) {
                if(shouldWriteMetric(checkout.getTeam(), metric)) {
                    if(metric instanceof RStopwatch) createCell(data, index + 2, ((RStopwatch) metric).getLapsString());
                    else if(metric instanceof RCalculation) createCell(data, index + 2, ((RCalculation) metric).getValue(checkout.getTeam().getTabs().get(0).getMetrics()));
                    else if(metric instanceof RFieldData) continue;
                    else createCell(data, index + 2, metric.toString());
                }
                else createCell(data, index + 2, "");
                index++;
            }
        }
    }

    @Override
    public String getSheetName() {
        return "MatchData";
    }

    @Override
    public int getColumnWidth() {
        return 2000;
    }
}
