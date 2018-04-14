package com.cpjd.roblu.models;

import java.io.Serializable;
import java.util.ArrayList;

import lombok.Data;

@Data
public class RPickLists implements Serializable {

    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;

    private ArrayList<RPickList> pickLists;

    public RPickLists(ArrayList<RPickList> pickLists) {
        this.pickLists = pickLists;
    }

    public ArrayList<RPickList> getPickLists() {
        if(pickLists == null) pickLists = new ArrayList<>();
        return pickLists;
    }
}
