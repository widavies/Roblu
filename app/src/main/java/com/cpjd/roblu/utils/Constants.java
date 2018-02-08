package com.cpjd.roblu.utils;

/*******************************************************
 * Copyright (C) 2016 Will Davies wdavies973@gmail.com
 *
 * This file is part of Roblu
 *
 * Roblu cannot be distributed for a price or to people outside of your local robotics team.
 *******************************************************/

// Some constants that are required in almost everything
public abstract class Constants {

    public static final String SERVICE_ID = "com.cpjd.roblu.service";
    public static final String RESTART_BROADCAST = "com.cpjd.roblu.service.RestartService";

    public static final String UPDATE_MESSAGE = "";

    public static final int VERSION = 11; // used for updating the changelist

    /*
     * v4.0.0 cross activity codes
     */
    /**
     * Code used to identify to TeamsView that a new event was created and the events list need to be refreshed
     */
    public static final int NEW_EVENT_CREATED = getNum();
    /**
     * Code used to identify to TeamsView that a user was creating a new event and discarded it (also called if user
     * was editing an existing event and discarded it)
     */
    public static final int EVENT_DISCARDED = getNum();
    /**
     * Called when a user discards or exits from activity without confirming some action
     */
    public static final int CANCELLED = getNum();
    /**
     * Called when the user saved changes to an existing or new metric
     */
    public static final int METRIC_CONFIRMED = getNum();
    /**
     * User saved changes to the form
     */
    public static final int FORM_CONFIRMED = getNum(); // edit form, user tapped confirm
    /**
     * Request to start a NEW METRIC event
     */
    public static final int NEW_METRIC_REQUEST = getNum();

    public static final int EDIT_METRIC_REQUEST = getNum();

    public static final int PREDEFINED_FORM_SELECTED = getNum();

    public static final int CUSTOM_SORT_CANCELLED = getNum();

    public static final int IMAGE_DELETED = getNum();

    public static final int IMAGE_EDITED = getNum();

    // Drawer identifier codes
    public static final int CREATE_EVENT = getNum();
    public static final int SCOUT = getNum();
    public static final int EVENT_SETTINGS = getNum();
    public static final int EDIT_MASTER_FORM = getNum();
    public static final int SETTINGS = getNum();
    public static final int HEADER = getNum();
    public static final int TUTORIALS = getNum();
    public static final int BLUETOOTH_SERVER = getNum();
    public static final int MY_MATCHES = getNum();
    public static final int SERVER_HEALTH = getNum();


    // Intent request and result codes
    public static final int GENERAL = getNum(); // when the request code doesn't matter
    public static final int MASTER_FORM = getNum(); // request edit of master form
    public static final int CREATE_EVENT_PICKER = getNum(); // start event picker
    public static final int GALLERY_EXIT = getNum();
    public static final int CUSTOM_SORT_CONFIRMED = getNum();
    public static final int TEAM_EDITED = getNum();
    public static final int FILE_CHOOSER = getNum();
    public static final int EVENT_INFO_EDITED = getNum();
    public static final int EVENT_SETTINGS_CHANGED = getNum();
    public static final int SETTINGS_CHANGED = getNum();
    public static final int MAILBOX_EXITED = getNum();

    private static int count = 0;
    private static int getNum() {
        return count++;
    }
}
