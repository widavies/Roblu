package com.cpjd.roblu.models;

import java.io.Serializable;
import java.util.ArrayList;

import lombok.Data;

@Data
public class RPickList implements Serializable {
    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;

    private String title;
    private int eventID;
    // In order of sorting
    private ArrayList<Integer> teamIDs;

    public RPickList(int eventID, String title) {
        this.title = title;
        this.eventID = eventID;
        this.teamIDs = new ArrayList<>();
    }
}
