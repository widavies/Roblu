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
    private int lastEventID;

    /**
     * Sets the last filter the user used so it can be saved when relaunching the app
     */
    private int lastFilter;

    /**
     * The master form, if the user has specified one
     */
    private RForm master;
    /**
     * Stores the look and feel of the UI, for premium users only
     */
    private RUI rui;

    /*
     * Roblu Cloud variables
     */

    /**
     * The team's authentication code, links the code with the Roblu Cloud Team profile
     */
    private String code;

    /**
     * The IP address of the server to connect to
     */
    private String serverIP;

    /**
     * Sets the default values for the RSettings class
     */
    public RSettings() {
        teamNumber = 0;
        lastEventID = -1;
        master = null;
        updateLevel = 0;
        setServerIPToDefault();
    }

    public void setRui(RUI rui) {
        rui.setUploadRequired(true);
        this.rui = rui;
    }
    public void setServerIPToDefault() {
        this.setServerIP("ec2-13-59-164-241.us-east-2.compute.amazonaws.com");
    }

}
