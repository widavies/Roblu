package com.cpjd.roblu.models;

import android.support.annotation.NonNull;

import java.io.Serializable;

import lombok.Data;

/**
 *
 * The newer model for transferring checkout data, a successor to RAssignment.
 *
 * Version 2 Features:
 * -Images are included within elements, instead of in a separate array
 * -Simplified status variables and conflict variables
 *
 * @version 2
 * @since 3.5.9
 * @author Will Davies
 */
@Data
public class RCheckout implements Serializable, Comparable<RCheckout> {

    private long ID;

    /**
     * If you look in the RTeam model, it contains an ArrayList of RTab.
     * This team should be filtered upon packaging to only include the tabs
     * required to this checkout. It might either include the PIT and PREDICTIONS tab, or one
     * match TAB. When merging, the correct tab will be merged back into the local RTeam model.
     */
    private RTeam team;

    /**
     * "Available", "Currently checked out to <name>", "Completed by <name>"
     */
    private String status;

    /**
     * If status = Completed by, then this is the time in milliseconds that it was completed
     */
    private long completedTime;

    /**
     * The time that this checkout was merged into the master repo., either set automatically
     * when auto-merged, or set explicitly when a conflict is resolved
     */
    private long mergedTime;
    /**
     * Either "no-local" or "edited-local"
     *
     * Essentially, we don't want to auto merge a checkout if the local copy has already been edited or if the local copy doesn't exist
     */
    private String conflictType;
    /**
     * Set to true if status or content of this class was changed,
     * if true, it will be uploaded to the server when a connection is available.
     */
    private boolean syncRequired;

    public RCheckout() {}

    /**
     * Constructor to use for packaging a checkout
     */
    public RCheckout(RTeam team) {
        this.team = team;
    }

    @Override
    public int compareTo(@NonNull RCheckout o) {
        return new Long(completedTime).compareTo(o.getCompletedTime());
    }

    public boolean equals(RCheckout checkout) {
        return ((team == null && checkout.getTeam() == null) || team.equals(checkout.getTeam()))
                && ID == checkout.getID() && completedTime == checkout.getCompletedTime() && mergedTime == checkout.getMergedTime() &&
                ((conflictType == null && checkout.getConflictType() == null) || conflictType.equals(checkout.getConflictType()));
    }
}
