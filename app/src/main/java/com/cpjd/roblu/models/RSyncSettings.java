package com.cpjd.roblu.models;

import java.io.Serializable;
import java.util.LinkedHashMap;

import lombok.Data;

/**
 * RSyncSettings is a subset settings model separate from RSettings.
 * Essentially, the background service needs to store and save some data.
 * The problem is, if the user makes a change to RSettings, data can get
 * over-written. So anything that needs to be saved from the background service
 * should be saved here.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
@Data
public class RSyncSettings implements Serializable {
    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;
    /**
     * If true, keep sending the /checkouts/purge request to the server
     */
    private boolean purgeRequested;
    /**
     * Used for authenticating read only events
     */
    private int publicTeamNumber;
    /**
     * Just stores a local copy of the optedIn status to keep the UI correct
     */
    private boolean optedIn;
    /**
     * Team timestamp, if this time stamp doesn't match the one received from the server,
     * pull the team.
     */
    private int teamSyncID;
    /**
     * Stores the sync ids for each checkout, if the IDs don't match, the checkouts should be synced.
     * The key represents the checkout ID, and the value represents that checkout's corresponding sync
     * ID.
     */
    private LinkedHashMap<Integer, Long> checkoutSyncIDs;

    public LinkedHashMap<Integer, Long> getCheckoutSyncIDs() {
        if(checkoutSyncIDs == null) this.checkoutSyncIDs = new LinkedHashMap<>();
        return checkoutSyncIDs;
    }
}
