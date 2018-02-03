package com.cpjd.roblu.models.metrics;

import org.codehaus.jackson.annotate.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * Represents a list of items from which only one item can be selected
 * @see RMetric for more information
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("RChooser")
public class RChooser extends RMetric {

    /**
     * Represents a list of possible items that can be selected
     */
    @NonNull
    private String[] values;
    /**
     * Represents the index of values[] which contains the currently selected item
     */
    private int selectedIndex;

    /**
     * The empty constructor is required for de-serialization
     */
    @SuppressWarnings("unused")
    public RChooser() {}

    /**
     * Instantiates a chooser object
     * @param ID the unique identifier for this object
     * @param title object title
     * @param values String[] containing possible item selections
     * @param selectedIndex the index of the currently selected value within values[]
     */
    public RChooser(int ID, String title, String[] values, int selectedIndex) {
        super(ID, title);
        this.values = values;
        this.selectedIndex = selectedIndex;
    }

    @Override
    public String getFormDescriptor() {
        if(values == null) return "Type: Chooser\nItems: ";
        StringBuilder descriptor = new StringBuilder("Type: Chooser\nItems: ");
        for(int i = 0; i < values.length; i++) {
            descriptor.append(values[i]);
            if(i != values.length - 1) descriptor.append(", ");
        }
        descriptor.append(" Default value: ").append(values[selectedIndex]);
        return descriptor.toString();
    }

    @Override
    public RMetric clone() {
        RChooser chooser = new RChooser(ID, title, values, selectedIndex);
        chooser.setRequired(required);
        return chooser;
    }

    @Override
    public String toString() {
        if(values == null) return "";
        return values[selectedIndex];
    }
}

