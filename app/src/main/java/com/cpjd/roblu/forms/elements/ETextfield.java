package com.cpjd.roblu.forms.elements;

public class ETextfield extends Element {

    private String text;

    public ETextfield(String title, String text) {
        super(title);
        this.text = text;

    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSubtitle() {
        return "Type: Text field\nDefault value: "+text;
    }

}
