package com.cpjd.roblu.models;

import lombok.Data;

/**
 * RCloudSettings is a subset settings model separate from RSettings.
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
public class RCloudSettings {
    /**
     * Millisecond timestamp of last successful server form, ui, and team sync
     */
    private long lastTeamSync;
    /**
     * Millisecond timestamp of last successful server checkouts sync
     */
    private long lastCheckoutSync;
    /**
     * If true, keep sending the /checkouts/purge request to the server
     */
    private boolean purgeRequested;

}
