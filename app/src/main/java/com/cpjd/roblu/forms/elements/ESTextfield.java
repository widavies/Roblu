package com.cpjd.roblu.forms.elements;

import lombok.Data;

/**
 * A more restrictive field, ONLY used for team number and team name in PIT.
 *
 * @since 3.2.0
 * @author Will Davies
 */
@Data

public class ESTextfield extends Element {

    private boolean numberOnly;

    public ESTextfield(String title, boolean numberOnly) {
        super(title);
        this.numberOnly = numberOnly;
        setModified(true);
    }

    public String getSubtitle() {
        String suffix = "Mandatory field used for editing team ";
        if(numberOnly) suffix += "number.";
        else suffix += "name.";
        return "Type: Text field\n"+suffix;
    }

}
