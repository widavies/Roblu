package com.cpjd.roblu.forms.elements;

// one line only, can be set to number input
public class ESTextfield extends Element {

    private final boolean numberOnly;

    public ESTextfield(String title, boolean numberOnly) {
        super(title);
        this.numberOnly = numberOnly;
        setModified(true);
    }

    public boolean isNumberOnly() {
        return numberOnly;
    }

    public String getSubtitle() {
        String suffix = "Mandatory field used for editing team ";
        if(numberOnly) suffix += "number.";
        else suffix += "name.";
        return "Type: Text field\n"+suffix;
    }

}
