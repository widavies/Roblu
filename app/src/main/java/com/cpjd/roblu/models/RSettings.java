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
     * Whether the user is signed in with their Google account or not
     */
    private boolean isSignedIn;
    /**
     * Roblu Cloud API variables
     */
    private String auth; // the user's authentication token, links them with their Roblu Cloud API account
    private String teamCode; // the team's authentication code, links the code with the Roblu Cloud Team profile
    private String name, email;

    private boolean clearActiveRequested;

    /**
     * Sets the default values for the RSettings class
     */
    public RSettings() {
        teamNumber = 0;
        lastEventID = -1;
        master = null;
        updateLevel = 0;
        isSignedIn = false;
    }

    public void setRui(RUI rui) {
        rui.setModified(true);
        this.rui = rui;
    }



}
