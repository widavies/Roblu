package com.cpjd.roblu.models;

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

    public RCheckout() {}

    /**
     * Constructor to use for packaging a checkout
     */
    public RCheckout(RTeam team) {
        this.team = team;
    }

    /**
     * Constructor for de-packaging a checkout
     */
    public RCheckout(RTeam team, String completedBy, long completedTime) {
        this.team = team;
        this.completedBy = completedBy;
        this.completedTime = completedTime;
    }

}
