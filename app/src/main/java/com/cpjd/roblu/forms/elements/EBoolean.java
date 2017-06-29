package com.cpjd.roblu.forms.elements;

import java.io.Serializable;

import lombok.Data;

@Data
public class EBoolean extends Element implements Serializable {

    private boolean value;

    public EBoolean(String title, boolean value) {
        super(title);
        this.value = value;
    }
    public boolean getValue() {
        return value;
    }
    public String getSubtitle() {
        return "Type: Boolean\nDefault value: "+value;
    }

}
