package com.cpjd.roblu.utils;

/**
 * Represents the status of a checkout. This IDs much match the output IDs or Roblu Master.
 *
 * @since 4.0.0
 */
public class HandoffStatus {
    /*
     * STATE TYPES
     */
    /**
     * Handoff hasn't been and is not checked out. It is available for scouting
     */
    @SuppressWarnings("unused")
    public static final int AVAILABLE = 0;
    /**
     * The handoff is currently checked out to somebody
     */
    public static final int CHECKED_OUT = 1;
    /**
     * The handoff is checked out to the local user
     */
    public static final int LOCALLY_CHECKED_OUT = 2;
    /**
     * The handoff has been completed
     */
    public static final int COMPLETED = 3;
    /*
     * UPLOAD TYPES
     */
    /**
     * Flags the background service to ONLY update meta level data for this handoff
     */
    public static final int STATUS_ONLY_UPLOAD = 1;
    /**
     * Flags the background service that the full model must be uploaded
     */
    public static final int FULL_UPLOAD = 2;

}
