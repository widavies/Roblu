package com.cpjd.roblu.forms.elements;

import java.util.ArrayList;

public class EChooser extends Element {

    private ArrayList<String> values;
    private int selected;

    public EChooser(String title, ArrayList<String> values, int selected) {
        super(title);
        this.values = values;
        this.selected = selected;
    }

    public ArrayList<String> getValues() {
        return values;
    }

    public int getSelected() {
        return selected;
    }


    public void setSelected(int selected) {
        this.selected = selected;
    }

    public void setValues(ArrayList<String> values) {
        this.values = values;
    }

    public String getSubtitle() {
        String subtitle = "Type: Chooser\nItems: ";
        if(values.size() == 0) return subtitle + "\nDefault value: ";
        for(String s : values) subtitle += s + ", ";
        return subtitle.substring(0, subtitle.length() - 2) +"\nDefault value: "+ values.get(selected);
    }

}

