package com.cpjd.roblu.utils;

import android.util.Log;

import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RBoolean;
import com.cpjd.roblu.models.metrics.RCheckbox;
import com.cpjd.roblu.models.metrics.RChooser;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RDivider;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RSlider;
import com.cpjd.roblu.models.metrics.RStopwatch;
import com.cpjd.roblu.models.metrics.RTextfield;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * CheckoutsEncoder is an alternative "serialization" method to the Jackson JSON library.
 * This CheckoutEncoder actually has potential for helping with cross platform development and lower
 * data transfer rates, however, currently it's only be used for QR codes, since they have such a small data limit.
 * Jackson JSON serializations are a lot more versatile and maintainable, so this class would need to be researched a bit
 * more to determine it's effectiveness in production network or Bluetooth serialization.
 *
 * Basically, this class is currently a work around, but could be modified into nicer code.
 *
 * Now you might be thinking, Will, why didn't you just compressed the JSON. I TRIED okay? QR codes don't handle non-alphanumeric
 * compression well. So here I am.
 *
 * This encoder is a BARE minimum encoder. So PLEASE account for that.
 * -RGalleries and RFieldDiagrams are IGNORED. They are simply too big to fit in a QR code
 * -RDividers and RCalculation are IGNORED. They can't be modified by scouters, so yeah.
 * -Only essential meta-data is kept
 *
 * @since 4.3.0
 * @version 1
 * @author Will Davies
 */
public class CheckoutEncoder {

    /**
     * The character used to separate tokens in a line of text
     */
    private static char DELIMITER = '╡';

    /**
     * Checks to see if the checkout will fit within a QR code
     * @param checkout the checkout to check serialization size of
     * @return true if this checkout will fit in QR code, false if not. If false, display an error message
     */
    private boolean fitsQRCode(RCheckout checkout) {
        return encodeCheckout("", checkout).getBytes().length <= 3000;
    }
    
    /**
     * This method will encode a checkout with as little overhead as possible (as opposed to JSON
     * serialization). This method is used for QR serialization to minimize the data transfer, since
     * compression isn't supported well by QR. This could be expanded for the whole project, but isn't really versatile enough yet.
     * @return The encoded string
     */
    public String encodeCheckout(String nameTag, RCheckout checkout) {
        StringBuilder builder = new StringBuilder();
        // Checkout components
        builder.append(checkout.getID()).append("\n");
        builder.append(nameTag).append("\n");
        // Team meta
        builder.append(checkout.getTeam().getID()).append("\n");
        builder.append(checkout.getTeam().getLastEdit()).append("\n");
        // Tabs!
        for(RTab tab : checkout.getTeam().getTabs()) {
            builder.append("TAB").append(tab.getTitle()).append("\n");
            builder.append(tab.isWon()).append("\n");
            // Edits
            builder.append("EDITS");
            if(tab.getEdits() != null) {
                for(Object o : tab.getEdits().keySet()) {
                    builder.append(",");
                    if(o.toString().equals("")) builder.append("Unknown");
                    else builder.append(o.toString());
                    builder.append(",").append(tab.getEdits().get(o.toString())); //EDITS,will:120391823,john,12039123
                }
            }
            builder.append("\n");

            // METRICS
            // Note:
            // -RGallery and RFieldDiagram not encoded because there was no hope of them fitting in the app anyway
            // -RDivider and RCalculation don't need to be encoded since they don't contain any user information
            for(RMetric metric : tab.getMetrics()) {
                builder.append(getMetricType(metric)).append(DELIMITER).append(metric.getID()).append(DELIMITER).append(metric.getTitle()).append(DELIMITER).append(metric.isModified()).append(DELIMITER);
                if(metric instanceof RBoolean) builder.append(((RBoolean) metric).isValue());
                else if(metric instanceof RCheckbox) {
                    if(((RCheckbox) metric).getValues() != null) {
                        for(Object o : ((RCheckbox) metric).getValues().keySet()) {
                            builder.append("(").append(o.toString()).append(",").append(((RCheckbox) metric).getValues().get(o.toString())).append(")").append(DELIMITER); //:(title,value):(title,value):
                        }
                    }
                }
                else if(metric instanceof RChooser) {
                    builder.append(((RChooser) metric).getSelectedIndex()).append(DELIMITER);
                    if(((RChooser) metric).getValues() != null) {
                        for(String s : ((RChooser) metric).getValues()) {
                            builder.append(s).append(DELIMITER); // :1:option:option:option:
                        }
                    }
                }
                else if(metric instanceof RCounter) builder.append(((RCounter) metric).isVerboseInput()).append(DELIMITER).append(((RCounter) metric).getValue()).append(DELIMITER).append(((RCounter) metric).getIncrement());
                else if(metric instanceof RSlider) builder.append(((RSlider) metric).getValue()).append(DELIMITER).append(((RSlider) metric).getMin()).append(DELIMITER).append(((RSlider) metric).getMax());
                else if(metric instanceof RStopwatch) {
                    builder.append(((RStopwatch) metric).getTime()).append(DELIMITER);
                    if(((RStopwatch) metric).getTimes() != null) {
                        for(Double t : ((RStopwatch) metric).getTimes()) {
                            builder.append(t).append(DELIMITER); // :curr:1:2:3:
                        }
                    }
                }
                else if(metric instanceof RTextfield) builder.append(((RTextfield) metric).getText());
                builder.append("\n");
            }

        }
        return builder.toString();
    }

    /**
     * Decodes a checkout encoded in the CheckoutEncoder format
     * @param string the string to deserialize
     * @return an instantiated checkout from the string
     */
    public RCheckout decodeCheckout(String string) {
        try {
            String[] lines = string.split("\n");

            for(String s : lines) {
                Log.d("RBS", "Line: "+s);
            }

            // Meta
            RCheckout checkout = new RCheckout();
            checkout.setID(Integer.parseInt(lines[0]));
            checkout.setNameTag(lines[1]);
            RTeam team = new RTeam();
            team.setID(Integer.parseInt(lines[2]));
            team.setLastEdit(Long.parseLong(lines[3]));
            team.setTabs(new ArrayList<RTab>());

            // Tabs
            for(int i = 0; i < lines.length; i++) {
                if(!lines[i].startsWith("TAB")) continue;

                // Tab meta
                RTab tab = new RTab();
                tab.setTitle(lines[i].substring(3));
                tab.setWon(Boolean.parseBoolean(lines[i + 1]));
                tab.setMetrics(new ArrayList<RMetric>());
                String[] tokens = lines[i + 2].split(",");
                LinkedHashMap<String, Long> edits = new LinkedHashMap<>();
                for(int k = 1; k < tokens.length; k++) {
                    edits.put(tokens[1], Long.parseLong(tokens[2]));
                }
                tab.setEdits(edits);

                // Metrics
                for(int k = i + 1; k < lines.length; k++) {
                    if(lines[k].startsWith("TAB")) break;

                    if(lines[i].startsWith("null")) continue;

                    String[] mTokens = lines[k].split(String.valueOf(DELIMITER));

                    RMetric metric = null;

                    switch(mTokens[0]) {
                        case "B":  // boolean
                            metric = new RBoolean();
                            ((RBoolean) metric).setValue(Boolean.parseBoolean(mTokens[4]));
                            break;
                        case "CH": { // checkbox
                            metric = new RCheckbox();
                            LinkedHashMap<String, Boolean> values = new LinkedHashMap<>();
                            for(int l = 4; l < mTokens.length; l++) {
                                values.put(mTokens[l].split(",")[0].substring(1), Boolean.parseBoolean(mTokens[l].split(",")[1].replace(")", "")));
                            }
                            ((RCheckbox) metric).setValues(values);
                            break;
                        }
                        case "CO": { // chooser
                            metric = new RChooser();
                            ((RChooser) metric).setSelectedIndex(Integer.parseInt(mTokens[4]));
                            String[] values = new String[mTokens.length - 6]; // the amount of values, with the header info removed

                            for(int l = 5; l < mTokens.length; l++) {
                                if(!mTokens[l].equals("")) values[l - 5] = mTokens[l];
                            }
                            ((RChooser) metric).setValues(values);
                            break;
                        }
                        case "C":  // counter
                            metric = new RCounter();
                            ((RCounter) metric).setVerboseInput(Boolean.parseBoolean(mTokens[4]));
                            ((RCounter) metric).setValue(Double.parseDouble(mTokens[5]));
                            ((RCounter) metric).setIncrement(Double.parseDouble(mTokens[6]));
                            break;
                        case "S":  // slider
                            metric = new RSlider();
                            ((RSlider) metric).setValue(Integer.parseInt(mTokens[4]));
                            ((RSlider) metric).setMin(Integer.parseInt(mTokens[5]));
                            ((RSlider) metric).setMax(Integer.parseInt(mTokens[6]));
                            break;
                        case "ST":  // stopwatch
                            metric = new RStopwatch();
                            ((RStopwatch) metric).setTime(Double.parseDouble(mTokens[4]));
                            ((RStopwatch) metric).setTimes(new ArrayList<Double>());
                            for(int l = 5; l < mTokens.length; l++) {
                                if(!mTokens[l].equals("")) ((RStopwatch) metric).getTimes().add(Double.parseDouble(mTokens[5]));
                            }
                            break;
                        case "T":  // textfield
                            metric = new RTextfield();
                            metric.setTitle(mTokens[4]);
                            break;
                    }
                    if(metric != null) {
                        metric.setID(Integer.parseInt(mTokens[1]));
                        metric.setTitle(mTokens[2]);
                        metric.setModified(Boolean.parseBoolean(mTokens[3]));
                        tab.getMetrics().add(metric);
                        // Adding metric
                        Log.d("RBS", "Adding metric "+metric.toString());
                    }
                }
                team.getTabs().add(tab);
            }
            checkout.setTeam(team);
            return checkout;
        } catch(Exception e) {
            e.printStackTrace();
            Log.d("RBS", "An error occurred while decoding a checkout. "+e.getMessage());
            return null;
        }
    }

    /**
     * Shortens metric identifiers
     * @param metric the metric to shorten
     * @return the shortened metric identifier
     */
    private static String getMetricType(RMetric metric) {
        if(metric instanceof RBoolean) return "B";
        else if(metric instanceof RCheckbox) return "CH";
        else if(metric instanceof RChooser) return "CO";
        else if(metric instanceof RCounter) return "C";
        else if(metric instanceof RDivider) return "D";
        else if(metric instanceof RSlider) return "S";
        else if(metric instanceof RStopwatch) return "ST";
        else if(metric instanceof RTextfield) return "T";
        else return "null";
    }
}
