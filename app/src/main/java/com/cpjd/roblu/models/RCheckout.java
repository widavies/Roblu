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
     * The Google email address of the user who has checked out this class, but not
     * completed it yet. This is used if another user would like to override the checkout.
     * When the user confirms and uploads the checkout, this variable should
     * be reset to null.
     *
     * How overriding works:
     * -User taps "show hidden checkouts", and checkouts this event & confirms the override.
     *  When uploading, the most recently edited checked out assignment will be merged.
     *  The person who overrided this does NOT need this variable converted to there name.
     */
    private String checkedOutTo;


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
