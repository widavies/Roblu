package com.cpjd.roblu.utils.searchCommands;

import android.util.Log;

import com.cpjd.roblu.models.metrics.RBoolean;
import com.cpjd.roblu.models.metrics.RCalculation;
import com.cpjd.roblu.models.metrics.RCheckbox;
import com.cpjd.roblu.models.metrics.RChooser;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RSlider;
import com.cpjd.roblu.models.metrics.RStopwatch;
import com.cpjd.roblu.models.metrics.RTextfield;

import java.util.ArrayList;
import java.util.regex.Pattern;

import lombok.Data;

@Data
public class CommandMetric {

    private String metricName;
    private String metricFilter;

    public CommandMetric(String metricName, String metricFilter) {
        this.metricName = metricName.replaceAll("\\s+", "");
        this.metricFilter = metricFilter.replaceAll("\\s+", "");

        Log.d("RBS", "Loading command metric for metricName: "+metricName+" and metricFilter: "+metricFilter);
    }

    public boolean metricPassesCriteria(RMetric metric) {
        if(!metric.getTitle().replaceAll("\\s+", "").equals(metricName)) return false;

        if(metric instanceof RCalculation) return passesNumerical(metric);
        else if(metric instanceof RCounter) return passesNumerical(metric);
        else if(metric instanceof RSlider) return passesNumerical(metric);
        else if(metric instanceof RStopwatch) return passesNumerical(metric);
        else if(metric instanceof RBoolean) return passesBoolean(((RBoolean) metric).isValue());
        else if(metric instanceof RCheckbox) {
            ArrayList<Boolean> b = new ArrayList<>();
            for(String s : ((RCheckbox) metric).getValues().keySet()) b.add(((RCheckbox) metric).getValues().get(s));
            return passesCheckbox(b);
        } else if(metric instanceof RChooser) {
            return ((RChooser) metric).getValues()[((RChooser) metric).getSelectedIndex()].equals(metricFilter);
        } else if(metric instanceof RTextfield) {
            return ((RTextfield) metric).getText().contains(metricFilter);
        }

        return false;
    }

    private boolean passesNumerical(RMetric metric) {
        if(metric instanceof RCalculation || metric instanceof RCounter || metric instanceof RSlider || metric instanceof RStopwatch) {
            Pattern greatOrEqual = Pattern.compile(">=\\d+");
            Pattern lessOrEqual = Pattern.compile("<=\\d+");
            Pattern great = Pattern.compile(">\\d+");
            Pattern less = Pattern.compile("<\\d+");
            Pattern equal = Pattern.compile("=\\d+");
            Pattern range = Pattern.compile("=\\d+-\\d+");

            double value;
            if(metric instanceof RCalculation) value = ((RCalculation) metric).getLastValue();
            else if(metric instanceof RCounter) value = ((RCounter) metric).getValue();
            else if(metric instanceof RSlider) value = ((RSlider) metric).getValue();
            else value = ((RStopwatch) metric).getTime();

            try {
                if(greatOrEqual.matcher(metricFilter).matches()) {
                    return Double.parseDouble(metricFilter.replace(">=", "")) >= value;
                }
                if(lessOrEqual.matcher(metricFilter).matches()) {
                    return Double.parseDouble(metricFilter.replace("<=", "")) <= value;
                }
                if(less.matcher(metricFilter).matches()) {
                    return Double.parseDouble(metricFilter.replace("<", "")) > value;
                }
                if(great.matcher(metricFilter).matches()) {
                    return Double.parseDouble(metricFilter.replace(">", "")) > value;
                }
                if(equal.matcher(metricFilter).matches()) {
                    return Double.parseDouble(metricFilter.replace("=", "")) == value;
                }
                if(range.matcher(metricFilter).matches()) {
                    return Double.parseDouble(metricFilter.split("-")[0]) <= value && value <= Double.parseDouble(metricFilter.split("-")[1]);
                }
            } catch(Exception e) {
                //
            }
        }

        return false;
    }

    private boolean passesBoolean(boolean value) {
        return value == metricFilter.toLowerCase().contains("t");
    }

    private boolean passesCheckbox(ArrayList<Boolean> values) {
        try {
            String[] split = metricFilter.split(":");
            for(int i = 0; i < split.length; i++) {
                split[i] = split[i].toLowerCase().replaceAll("t", "true").replaceAll("f", "false");
                if(split[i].equals("true") != values.get(i)) return false;
            }
            return true;
        } catch(Exception e) {
            //
        }
        return false;
    }
}
