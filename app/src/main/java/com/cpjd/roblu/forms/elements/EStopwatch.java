package com.cpjd.roblu.forms.elements;

/**
 * A stopwatch that supports a decimal time value. Supports N/A
 *
 * @since 3.2.0
 * @author Will Davies
 */

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class EStopwatch extends Element {

    private double time;

    public EStopwatch(String title, double time) {
        super(title);
        this.time = time;

    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public String getSubtitle() {
        return "Type: Stopwatch\n"+"Default value: "+time+"s";
    }

}
