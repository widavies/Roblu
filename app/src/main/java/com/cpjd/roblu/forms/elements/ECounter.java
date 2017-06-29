package com.cpjd.roblu.forms.elements;

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

    public void setMin(int min) {
        this.min = min;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public void setIncrement(int increment) {
        this.increment = increment;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getIncrement() {
        return increment;
    }

    public int getCurrent() {
        return current;
    }

    public String getSubtitle() {
        return "Type: Counter\nMin: "+min+"\nMax: "+max+"\nIncrement: "+increment+"\nDefault value: "+current;
    }

}
