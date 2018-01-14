package com.cpjd.roblu.models.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * Represents a list of items that each contain a title and value, essentially a list of checkboxes
 * @see RMetric for more information
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RCheckbox extends RMetric {

    /**
     * Ordered HashMap containing a title and value for the specified number of elements.
     * The title is treated as the key, so duplicates aren't allowed.
     */
    @NonNull
    private LinkedHashMap<String, Boolean> values;

    /**
     * Creates a RCheckbox model
     * @param ID the unique identifier for this object
     * @param title object title
     * @param values non-null, no duplicates map containing title and boolean key pairs
     */
    public RCheckbox(int ID, String title, LinkedHashMap<String, Boolean> values) {
        super(ID, title);
        this.values = values;
        if(this.values == null || values.size() == 0) {
            throw new RuntimeException("RCheckbox must be instantiated with at least 1 item in the LinkedHashMap");
        }
    }

    @Override
    public String getFormDescriptor() {
        StringBuilder descriptor = new StringBuilder("Type: Checkbox\nItems: (key,defaultValue) ");
        for(Object o : values.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            descriptor.append("(").append(pair.getKey()).append(", ").append(pair.getValue()).append(")");
        }
        return descriptor.toString();
    }

    @Override
    public RMetric clone() {
        RCheckbox checkbox = new RCheckbox(ID, title, (LinkedHashMap<String, Boolean>)values.clone());
        checkbox.setRequired(required);
        return checkbox;
    }
}
