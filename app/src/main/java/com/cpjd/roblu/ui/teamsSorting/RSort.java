package com.cpjd.roblu.ui.teamsSorting;

import com.cpjd.roblu.models.metrics.RMetric;

/**
 * RSort IS NOT an official RMetric. It's a simple utility class that allows us (the developers) to
 * add custom sort methods that aren't just based of a RMetric from the form.
 *
 * THIS CLASS SHOULD NEVER BE SERIALIZED!
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class RSort extends RMetric {

    /**
     * The description here actually can be changed since RSort is a multi-use (as opposed to single use)
     * RMetric.
     */
    private String description;

    /**
     * The RSort ID here actually represents the TeamMetricProcessor.PROCESS_METHOD
     * @param title the title of this metric
     * @param method
     */
    public RSort(String title, int method, String description) {
        super(method, title);
        this.description = description;
    }

    @Override
    public String getFormDescriptor() {
        return description;
    }

    /**
     * Clone is not specified because this RSort class should NEVER be serialized
     * @return null
     */
    @Override
    public RMetric clone() {
        return null;
    }
}
