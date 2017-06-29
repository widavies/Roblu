package com.cpjd.roblu.forms.elements;

import lombok.Data;

/**
 * Stores a numeric value. Supports a min, max, increment, and default value. Can be N/A
 *
 * @since 3.2.0
 * @author Will Davies
 */
@Data
public class ECounter extends Element {

    private int min;
    private int max;
    private int increment;
    private int current;

    public ECounter(String title, int min, int max, int increment, int current) {
        super(title);
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.current = current;
    }

    public String getSubtitle() {
        return "Type: Counter\nMin: "+min+"\nMax: "+max+"\nIncrement: "+increment+"\nDefault value: "+current;
    }

}
