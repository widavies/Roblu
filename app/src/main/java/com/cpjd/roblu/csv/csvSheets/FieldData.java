package com.cpjd.roblu.csv.csvSheets;

import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RFieldData;
import com.cpjd.roblu.models.metrics.RMetric;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.ArrayList;

/**
 * Exports all the field data metrics
 *
 * @author Will Davies
 * @version 1
 * @since 4.4.0
 */
public class FieldData extends CSVSheet {
    @Override
    public void generateSheet(XSSFSheet sheet, REvent event, RForm form, RTeam[] teams, ArrayList<RCheckout> checkouts) {
        Row one = createRow(sheet);
        createCell(one, 0, "Team#");
        createCell(one, 1, "Match#");

        // Find a random RFieldData reference
        RFieldData fieldData = null;
        try {
            mainLoop:
            for(RTab tab : teams[0].getTabs()) {
                if(tab.getTitle().equalsIgnoreCase("PIT") || tab.getTitle().equalsIgnoreCase("PREDICTIONS")) continue;
                for(RMetric metric2 : tab.getMetrics()) {
                    if(metric2 instanceof RFieldData) {
                        fieldData = (RFieldData) metric2;
                        break mainLoop;
                    }
                }
            }
        } catch(Exception e) {//}
        }

        // Copy the metrics over
        int index = 2;
        for(Object key : fieldData.getData().keySet()) {
            createCell(one, index, key.toString());
            index++;
        }

        // Start copying data
        for(RCheckout checkout : checkouts) {
            if(!checkout.getTeam().getTabs().get(0).getTitle().startsWith("Quals")) continue;

            Row row = createRow(sheet);

            createCell(row, 0, String.valueOf(checkout.getTeam().getNumber()));
            createCell(row, 1, checkout.getTeam().getTabs().get(0).getTitle());

            index = 0;
            mainLoop : for(RTab tab : checkout.getTeam().getTabs()) {
                for(RMetric metric2 : tab.getMetrics()) {
                    if(metric2 instanceof RFieldData) {
                        for(Object key : ((RFieldData)metric2).getData().keySet()) {
                            createCell(row, index + 2, ((RFieldData)metric2).getData().get(key).toString());
                            index++;
                        }
                        break mainLoop;
                    }
                }
            }

        }
    }

    @Override
    public String getSheetName() {
        return "FieldData";
    }

    @Override
    public int getColumnWidth() {
        return 3000;
    }
}
