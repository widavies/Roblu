package com.cpjd.roblu.utils;

/**
 * Represents the status of a checkout. This IDs much match the output IDs or Roblu Master.
 *
 * @since 4.0.0
 */
@SuppressWarnings("unused")
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
     * The handoff has been completed
     */
    public static final int COMPLETED = 2;

}
