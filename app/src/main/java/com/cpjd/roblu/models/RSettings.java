package com.cpjd.roblu.models;

import java.io.Serializable;

import lombok.Data;

/**
 * Stores application level data.
 *
 * @since 3.0.0
 * @author Will Davies
 */
@Data
public class RSettings implements Serializable {
    /**
     * Used for determining if we need to show a changelist
     */
    private int updateLevel;
    /**
     * Local team number used for some TBA calls
     */
    private int teamNumber;
    /**
     * The last event the user was looking at, for convenience, we can
     * reload that event at app startup
     */
    private long lastEventID;
    /**
     * The master form, if the user has specified one
     */
    private RForm master;
    /**
     * Stores the look and feel of the UI, for premium users only
     */
    private RUI rui;
    /**
     * If the account is premium, we should verify this at startup
     * (Check if user has already purchased app or not)
     */
    private boolean premium;
    /**
     * The team code, a top secret code using for connecting with the Roblu API
     */
    private String teamCode;

    /**
     * Whether the user is signed in with their Google account or not
     */
    private boolean isSignedIn;

    /**
     * Sets the default values for the RSettings class
     */
    public RSettings() {
        teamNumber = 0;
        lastEventID = -1;
        master = null;
        premium = false;
        updateLevel = 0;
        isSignedIn = false;
    }
}
