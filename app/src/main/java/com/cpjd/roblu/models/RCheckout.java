package com.cpjd.roblu.models;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;

import lombok.Data;

/**
 *
 * The newer model for transferring checkout data, a successor to RAssignment.
 *
 * @since 3.5.9
 * @author Will Davies
 */
@Data
public class RCheckout implements Serializable {

    private long ID;

    /**
     * More of a backend variable, but essentially, Roblu supports multiple events,
     * so we have to know the correct event to add this checkout to when it's received
     * from a scouter
     */
    private long eventID;

    /**
     * All the images that are associated with the RTab in this checkout
     * must be converted to a byte[] array so they can be transferred.
     * New ideas be will reassigned when the checkout is depackaged.
     */
    private ArrayList<byte[]> images;

    /**
     * If you look in the RTeam model, it contains an ArrayList of RTab.
     * This team should be filtered upon packaging to only include the tabs
     * required to this checkout. It might either include the PIT and PREDICTIONS tab, or one
     * match TAB. When merging, the correct tab will be merged back into the local RTeam model.
     */
    private RTeam team;

    /**
     * These variables are filled out ONLY when receiving a checkout.
     *
     */
    private String completedBy;
    private long completedTime;

    /**
     * The time that this checkout was merged into the master repo.
     */
    private long mergedTime;
    private String conflictType;

    /**
     * Constructor to use for packaging a checkout
     */
    public RCheckout(long eventID, RTeam team) {
        this.eventID = eventID;
        this.team = team;
    }

    /**
     * Constructor for de-packaging a checkout
     */
    public RCheckout(long eventID, RTeam team, String completedBy, long completedTime) {
        this.eventID = eventID;
        this.team = team;
        this.completedBy = completedBy;
        this.completedTime = completedTime;
    }

    /**
     * Converts the instance of this class into a string that can be
     * uploaded and processed by the server.
     * @return the instance of this class, as a .json string
     */
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }


}
