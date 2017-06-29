package com.cpjd.roblu.forms.elements;

public class ESort extends Element {
    private final String subtitle;

    public ESort(String title, String subtitle, int ID) {
        super(title);
        this.subtitle = subtitle;
        this.setID(ID);
    }

    public String getSubtitle() {
        return subtitle;
    }

}
