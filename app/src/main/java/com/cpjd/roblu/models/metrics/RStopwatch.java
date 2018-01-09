package com.cpjd.roblu.models.metrics;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a time value, in seconds.
 * @see RMetric for more information
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RStopwatch extends RMetric {
    /**
     * The time, in seconds, currently on this stopwatch
     */
    private double time;

    /**
     * Instantiates a stopwatch object
     * @param ID the unique identifier for this object
     * @param title object title
     * @param time the time, in seconds
     */
    private RStopwatch(int ID, String title, double time) {
        super(ID, title);
        this.time = time;
    }

    @Override
    public String getFormDescriptor() {
        return "Type: Stopwatch\nDefault value: "+time;
    }

    @Override
    public RMetric clone() {
        RStopwatch stopwach = new RStopwatch(ID, title, time);
        stopwach.setRequired(required);
        return stopwach;
    }
}
