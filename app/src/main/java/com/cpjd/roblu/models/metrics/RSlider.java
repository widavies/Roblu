package com.cpjd.roblu.models.metrics;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a numerical, integer value with a min or a max.
 * @see RMetric for more information
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RSlider extends RMetric {

    /**
     * Stores the integer value
     */
    private int value;

    /**
     * Stores the minimum possible value. value MUST BE >=min
     */
    private int min;

    /**
     * Stores the maximum possible value. value MUST BE <=max
     */
    private int max;

    /**
     * Instantiates a slider object
     * @param ID the unique identifier for this object
     * @param title object title
     * @param min the minimum, integer value
     * @param max the maximum, integer value
     * @param value the current value
     */
    public RSlider(int ID, String title, int min, int max, int value) {
        super(ID, title);
        this.min = min;
        this.max = max;
        this.value = value;
    }

    @Override
    public String getFormDescriptor() {
        return "Type: Slider\nMin: "+min+" Max: "+max+" Default value: "+value;
    }

    @Override
    public RMetric clone() {
        RSlider slider = new RSlider(ID, title, min, max, value);
        slider.setRequired(required);
        return slider;
    }
}
