package com.cpjd.roblu.models.metrics;

import org.codehaus.jackson.annotate.JsonTypeName;

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
@JsonTypeName("RCheckbox")
public class RCheckbox extends RMetric {
    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;

    /**
     * Ordered HashMap containing a title and value for the specified number of elements.
     * The title is treated as the key, so duplicates aren't allowed.
     */
    @NonNull
    private LinkedHashMap<String, Boolean> values;

    /**
     * The empty constructor is required for de-serialization
     */
    @SuppressWarnings("unused")
    public RCheckbox() {}

    /**
     * Creates a RCheckbox model
     * @param ID the unique identifier for this object
     * @param title object title
     * @param values non-null, no duplicates map containing title and boolean key pairs
     */
    public RCheckbox(int ID, String title, LinkedHashMap<String, Boolean> values) {
        super(ID, title);
        this.values = values;
    }

    @Override
    public String getFormDescriptor() {
        StringBuilder descriptor = new StringBuilder("Type: Checkbox\nItems (key,defaultValue): ");
        descriptor.append("\n");
        return descriptor.append(toString()).toString();
    }

    @Override
    public RMetric clone() {
        RCheckbox checkbox = new RCheckbox(ID, title, new LinkedHashMap<>(values));
        return checkbox;
    }

    @Override
    public String toString() {
        StringBuilder descriptor = new StringBuilder();
        if(values != null) {
            for(Object o : values.entrySet()) {
                Map.Entry pair = (Map.Entry) o;
                descriptor.append("(").append(pair.getKey()).append(", ").append(pair.getValue()).append(")").append(", ");
            }
        }
        return descriptor.toString().substring(0, descriptor.toString().length() - 2); // make sure to remove trailing comma
    }
}
