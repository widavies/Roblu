package com.cpjd.roblu.forms.elements;

public class ESlider extends Element{

    private int max;
    private int current;

    public ESlider(String title, int max, int current) {
        super(title);
        this.max = max;
        this.current = current;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getMax() {
        return max;
    }

    public int getCurrent() {
        return current;
    }

    public String getSubtitle() {
        return "Type: Slider\nMax: "+max+"\nDefault value: "+current;
    }

}
