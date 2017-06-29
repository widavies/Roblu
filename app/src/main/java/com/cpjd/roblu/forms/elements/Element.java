package com.cpjd.roblu.forms.elements;

import java.io.Serializable;

import lombok.Data;

@Data
public abstract class Element implements Serializable {

    private static final long serialVersionUID = 8213540372141126243L;
    private String title;
    private int ID;
    private boolean modified; // if this is false, we can safely override the element's value

    Element(String title) {
        this.title = title; modified = false;
    }

    public abstract String getSubtitle();

}
