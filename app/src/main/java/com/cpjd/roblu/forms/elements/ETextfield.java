package com.cpjd.roblu.forms.elements;

import lombok.Data;

/**
 * Stores text. Nuff said.
 *
 * @since 3.2.0
 * @author Will Davies
 */
@Data
public class ETextfield extends Element {

    private String text;

    public ETextfield(String title, String text) {
        super(title);
        this.text = text;
    }

    public String getSubtitle() {
        return "Type: Text field\nDefault value: "+text;
    }

}
