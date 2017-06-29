package com.cpjd.roblu.forms.elements;

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
