package com.cpjd.roblu.models.metrics;

import org.codehaus.jackson.annotate.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a numerical, integer value with a min or a max.
 * @see RMetric for more information
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("RSlider")
public class RSlider extends RMetric {

    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;

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
     * The empty constructor is required for de-serialization
     */
    @SuppressWarnings("unused")
    public RSlider() {}

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

    /**
     * Sets the minimum value for this slider, and resets the current value if needed
     * @param min the value to set as the minimum
     */
    public void setMin(int min) {
        this.min = min;
        if(value < min) value = min;
        if(value > max) value = max;
    }

    /**
     * Sets the maximum value for this slider, and resets the current value if needed
     * @param max the value to set as the minimum
     */
    public void setMax(int max) {
        this.max = max;
        if(value > max) value = max;
        if(value < min) value = min;
    }

    @Override
    public RMetric clone() {
        RSlider slider = new RSlider(ID, title, min, max, value);
        return slider;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
