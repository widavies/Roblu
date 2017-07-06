package com.cpjd.roblu.forms.elements;


import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Supports one value on a horizontal slider, with a max. Can be N/A
 *
 * @since 3.2.0
 * @author Will Davies
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class ESlider extends Element{

    private int max;
    private int current;

    public ESlider(String title, int max, int current) {
        super(title);
        this.max = max;
        this.current = current;
    }

    public String getSubtitle() {
        return "Type: Slider\nMax: "+max+"\nDefault value: "+current;
    }

}
