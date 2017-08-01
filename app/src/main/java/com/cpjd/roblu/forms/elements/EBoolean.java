package com.cpjd.roblu.forms.elements;

import org.codehaus.jackson.annotate.JsonTypeName;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Stores either a true or false value. Has the option for a not observed value as well.
 *
 * @since 3.2.0
 * @author Will Davies
 */
@Data
@JsonTypeName("EBoolean")
@EqualsAndHashCode(callSuper=false)
public class EBoolean extends Element implements Serializable {

    private int value;
    private boolean usingNA;

    public EBoolean() {}

    public EBoolean(String title, int value) {
        super(title);
        this.value = value;
        this.usingNA = value == -1;
    }
    public String getSubtitle() {
        String temp = "false";
        if(value == 1) temp = "true";
        return "Type: Boolean\nDefault value: "+temp;
    }

}
