package com.cpjd.roblu.models.metrics;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents an element with value either TRUE or FALSE.
 * @see RMetric for more information
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RBoolean extends RMetric {

    private boolean value;

    /**
     * Instantiates a boolean model
     * @param ID the unique identifier for this object
     * @param title object title
     * @param value boolean value to store
     */
    public RBoolean(int ID, String title, boolean value) {
        super(ID, title);
        this.value = value;
    }

    @Override
    public String getFormDescriptor() {
        return "Type: Boolean\nDefault value: "+value;
    }

    @Override
    public RMetric clone() {
        RBoolean bool = new RBoolean(ID, title, value);
        bool.setRequired(required);
        return bool;
    }

    @Override
    public String toString() {
        if(value) return "Y";
        else return "N";
    }

}
