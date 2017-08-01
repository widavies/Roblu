package com.cpjd.roblu.models;

import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.utils.Text;

import java.io.Serializable;
import java.util.ArrayList;

import lombok.Data;

/**
 * Stores form data for the PIT and Match forms.
 *
 * @since 3.0.0
 * @author Will Davies
 */
@Data
public class RForm implements Serializable {

    /**
     * If this variable is set to true, then it will be re-uploaded to the cloud
     */
    private boolean modified;

    private ArrayList<Element> pit;
    private ArrayList<Element> match;

    public RForm() {

    }

    public RForm(ArrayList<Element> pit, ArrayList<Element> match) {
        this.pit = pit;
        this.match = match;
    }

    RForm duplicate() {
        return new RForm(Text.createNew(pit), Text.createNew(match));
    }
}
