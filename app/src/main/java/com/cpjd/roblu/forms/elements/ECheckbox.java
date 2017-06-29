package com.cpjd.roblu.forms.elements;

import java.util.ArrayList;

import lombok.Data;

/**
 * Supports an array of checkboxes, each can either be true or false.
 *
 * @since 3.2.0
 * @author Will Davies
 */
@Data
public class ECheckbox extends Element {

    private ArrayList<String> values;
    private ArrayList<Boolean> checked;

    public ECheckbox(String title, ArrayList<String> values, ArrayList<Boolean> checked) {
        super(title);
        this.values = values;
        this.checked = checked;
    }

    public String getSubtitle() {
        String subtitle = "Type: Checkbox\nItems: ";
        if(values.size() == 0) return subtitle + "\nDefault values: ";
        for(String s : values) subtitle += s + ", ";
        subtitle = subtitle.substring(0, subtitle.length() - 2) +"\nDefault values: ";
        for(boolean b : checked) subtitle += b + ", ";
        return subtitle.substring(0, subtitle.length() - 2);
    }

}
