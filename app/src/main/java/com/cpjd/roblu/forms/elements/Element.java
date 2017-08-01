package com.cpjd.roblu.forms.elements;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import java.io.Serializable;

import lombok.Data;

/**
 * The element model is the abstraction of what an element card should look like.
 * Elements can have a title, subtitle, modified, and a custom implementation of some value defined
 * in the child class. All the annotations all Jackson to correctly process children of this
 * abstraction when serializing.
 *
 * @since 3.2.0
 * @author Will Davies
 */

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EBoolean.class, name = "EBoolean"),
        @JsonSubTypes.Type(value = ECheckbox.class, name = "ECheckbox"),
        @JsonSubTypes.Type(value = EChooser.class, name = "EChooser"),
        @JsonSubTypes.Type(value = ECounter.class, name = "ECounter"),
        @JsonSubTypes.Type(value = EGallery.class, name = "EGallery"),
        @JsonSubTypes.Type(value = ESlider.class, name = "ESlider"),
        @JsonSubTypes.Type(value = ESTextfield.class, name = "ESTextfield"),
        @JsonSubTypes.Type(value = EStopwatch.class, name = "EStopwatch"),
        @JsonSubTypes.Type(value = ETextfield.class, name = "ETextfield")
})
public abstract class Element implements Serializable {

    private String title;
    private boolean modified; // if this is false, we can safely override the element's value with a form changes
    private int ID;

    public Element() {}

    public Element(String title) {
        this.title = title;
        modified = false;
    }

    public abstract String getSubtitle();
}
