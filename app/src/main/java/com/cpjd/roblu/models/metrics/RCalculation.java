package com.cpjd.roblu.models.metrics;

import android.util.Log;

import com.cpjd.roblu.utils.Utils;
import com.fathzer.soft.javaluator.DoubleEvaluator;

import org.codehaus.jackson.annotate.JsonTypeName;

import java.util.ArrayList;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RCalculation supports custom calculations based off other custom metrics.
 * It only supports metrics based off the RSlider, RCounter, and RStopwatch metrics.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("RSlider")
public class RCalculation extends RMetric {
    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;

    /**
     * Stores the last value, call getValue() to update this.
     * toString() will return this
     */
    private double lastValue;

    /**
     * The equation string
     */
    private String calculation;

    /**
     * The empty constructor is required for de-serialization
     */
    @SuppressWarnings("unused")
    public RCalculation() {}

    /**
     * Instantiates a boolean model
     * @param ID the unique identifier for this object
     * @param title object title
     */
    public RCalculation(int ID, String title) {
        super(ID, title);
    }

    @Override
    public String getFormDescriptor() {
        return "Type: Calculation metric\nCalculation: "+calculation;
    }

    @Override
    public RMetric clone() {
        RCalculation metric = new RCalculation(ID, title);
        metric.setCalculation(calculation);
        return metric;
    }

    /**
     * Process the equation and returns a value
     * @param metrics the metric list to process
     * @return the value
     */
    public String getValue(ArrayList<RMetric> metrics) {
        if(calculation == null || calculation.equals("null")) return "Bad equation";

        try {
            String equation = calculation;

            // Substitute values in for the metric names
            for(RMetric metric : metrics) {
                // Skip the reference to "ourself"
                if(metric.getTitle().equals(title)) continue;

                if(metric instanceof RCounter || metric instanceof RStopwatch || metric instanceof RSlider) {
                    equation = equation.replaceAll(metric.getTitle(), metric.toString());
                }
                else if(metric instanceof RCalculation) {
                    // This condition is required or this will overflow recursively
                    if(equation.contains(metric.getTitle())) equation = equation.replaceAll(metric.getTitle(), ((RCalculation) metric).getValue(metrics));
                }
            }

            // Trim
            equation = equation.trim();
            equation = equation.replaceAll(" ", "");

            Log.d("RBS", "Evaluating "+equation+". Value: "+new DoubleEvaluator().evaluate(equation));

            // Process
            lastValue = Utils.round(new DoubleEvaluator().evaluate(equation), 2);
            return String.valueOf(Utils.round(new DoubleEvaluator().evaluate(equation), 2));
        } catch(Exception e) {
            Log.d("RBS", "bad equation: ", e);
            return "Bad equation";
        }
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public String toString() {
        return String.valueOf(lastValue);
    }
}
