package com.cpjd.roblu.forms.elements;

import org.codehaus.jackson.annotate.JsonTypeName;

import java.util.ArrayList;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Allows the selection of one item from a list of item from spinner card.
 *
 * @since 3.2.0
 * @author Will Davies
 */
@Data
@EqualsAndHashCode(callSuper=false)
@JsonTypeName("EChooser")
public class EChooser extends Element {

    private ArrayList<String> values;
    private int selected;

    public EChooser() {}

    public EChooser(String title, ArrayList<String> values, int selected) {
        super(title);
        this.values = values;
        this.selected = selected;
    }
    @Override
    public String getSubtitle() {
        String subtitle = "Type: Chooser\nItems: ";
        if(values.size() == 0) return subtitle + "\nDefault value: ";
        for(String s : values) subtitle += s + ", ";
        return subtitle.substring(0, subtitle.length() - 2) +"\nDefault value: "+ values.get(selected);
    }

}

