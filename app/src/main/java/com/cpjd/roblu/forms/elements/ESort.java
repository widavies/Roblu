package com.cpjd.roblu.forms.elements;

/**
 * This card is just a helper tool for the custom sort utility, allows the user to pick an element
 *
 * @since 3.5.0
 * @author Will Daviews
 */

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
