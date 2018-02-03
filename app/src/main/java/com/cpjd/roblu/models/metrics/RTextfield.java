package com.cpjd.roblu.models.metrics;

import org.codehaus.jackson.annotate.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a text field that can store text.
 * @see RMetric for more information
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("RTextfield")
public class RTextfield extends RMetric {

    /**
     * The text contained in this text field
     */
    private String text;

    /**
     * If true, all UI interfaces should detect this flag and only insert numerical content into this field
     * This should really only be used for the team name or team number car.
     */
    private boolean numericalOnly;

    /**
     * If true, all UI interfaces should load all line breaks onto one, horizontally scrollable text input
     * This should really only be used for the team name or team number car.
     */
    private boolean oneLine;

    /**
     * The empty constructor is required for de-serialization
     */
    @SuppressWarnings("unused")
    public RTextfield() {}

    /**
     * Instantiates a text field object
     * @param ID the unique identifier for this object
     * @param title object title
     * @param text current text
     */
    public RTextfield(int ID, String title, String text) {
        super(ID, title);
        this.text = text;
        this.numericalOnly = false;
        this.oneLine = false;
    }

    /**
     * Instantiates a text field object, with more parameter options
     * @param ID the unique identifier for this object
     * @param title object title
     * @param numericalOnly true to setup a UI flag to only pass numeric input into this class
     * @param oneLine true to setup a UI flag to only allow one line of text
     * @param text current text
     */
    public RTextfield(int ID, String title, boolean numericalOnly, boolean oneLine, String text) {
        super(ID, title);
        this.text = text;
        this.numericalOnly = numericalOnly;
        this.oneLine = oneLine;
    }

    @Override
    public String getFormDescriptor() {
        /*
         * Determine if this is a special field
         * (numericalOnly and oneLine variables cannot be
         * created by the user, so we'll use those to determine
         * if this is a team name or team number metric)
         */
        if(oneLine && numericalOnly) return "Type: Text field\nMandatory field used for editing team number.";
        else if(oneLine) return "Type: Text field\nMandatory field used for editing team name.";
        return "Type: Text field\nDefault value: "+text;
    }

    @Override
    public RMetric clone() {
        RTextfield textfield = new RTextfield(ID, title, numericalOnly, oneLine, text);
        textfield.setRequired(required);
        return textfield;
    }

    @Override
    public String toString() {
        return text;
    }

}
