package com.cpjd.roblu.forms.elements;

import java.io.Serializable;

import lombok.Data;

/**
 * The element model is the abstraction of what an element card should look like.
 * Elements can have a title, subtitle, modified, and a custom implementation of some value.
 *
 * @since 3.2.0
 * @author Will Davies
 */

@Data
public abstract class Element implements Serializable {

    private String title;
    private int ID;
    private boolean modified; // if this is false, we can safely override the element's value

    Element(String title) {
        this.title = title; modified = false;
    }

    public abstract String getSubtitle();

}
