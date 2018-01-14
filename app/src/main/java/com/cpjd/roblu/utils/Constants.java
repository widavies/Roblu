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

    public static final String UPDATE_MESSAGE = "";

    public static final int VERSION = 11; // used for updating the changelist

    /*
     * v4.0.0 cross activity codes
     */
    /**
     * Code used to identify to TeamsView that a new event was created and the events list need to be refreshed
     */
    public static final int NEW_EVENT_CREATED = 123;
    /**
     * Code used to identify to TeamsView that a user was creating a new event and discarded it (also called if user
     * was editing an existing event and discarded it)
     */
    public static final int EVENT_DISCARDED = 1238;
    /**
     * Called when a user discards or exits from activity without confirming some action
     */
    public static final int CANCELLED = 1298754;
    /**
     * Called when the user saved changes to an existing or new metric
     */
    public static final int METRIC_CONFIRMED = 120398;
    /**
     * User saved changes to the form
     */
    public static final int FORM_CONFIRMED = 3; // edit form, user tapped confirm
    /**
     * Request to start a NEW METRIC event
     */
    public static final int NEW_METRIC_REQUEST = 1293;

    public static final int EDIT_METRIC_REQUEST = 14;

    public static final int PREDEFINED_FORM_SELECTED = 12;




    // Drawer identifier codes
    public static final int CREATE_EVENT = 1;
    public static final int SCOUT = 2;
    public static final int EVENT_SETTINGS = 3;
    public static final int EDIT_MASTER_FORM = 4;
    public static final int SETTINGS = 5;
    public static final int HEADER = 6;
    public static final int TUTORIALS = 7;
    public static final int MAILBOX = 8;

    // Intent request and result codes
    public static final int GENERAL = 1; // when the request code doesn't matter
    public static final int MASTER_FORM = 2; // request edit of master form
    public static final int FORM_DISCARDED = 4; // edit form, user tapped home or back
    public static final int NEW_CONFIRMED = 6; // new element was added
    public static final int NEW_DISCARDED = 7; // new element was discarded
    public static final int EDIT_CONFIRMED = 8; // add element - editing element was confirmed
    public static final int EDIT_DISCARDED = 9; // add element - editing element was discarded
    public static final int CREATE_EVENT_PICKER = 10; // start event picker
    public static final int PICKER_CANCELLED = 11; // event picker home or back pressed
    public static final int MANUAL_CREATED = 13; // manual event was create
    public static final int MANUAL_DISCARDED = 14; // manual event discarded
    public static final int EVENT_IMPORTED = 16; // event was imported
    public static final int EVENT_IMPORT_CANCELLED = 17; // import was cancelled
    public static final int REQUEST_IMPORT_FORM = 18; // imported event w/edit form
    public static final int REQUEST_IMPORTED_PREDEFINED = 19; // import event w/predefined form
    public static final int CAMERA_REQUEST = 21; // request to take a picture
    public static final int GALLERY_EXIT = 22;
    public static final int CUSTOM_SORT_CONFIRMED = 23;
    public static final int IMAGE_DELETED = 24;
    public static final int TEAM_EDITED = 25;
    public static final int FILE_CHOOSER = 26;
    public static final int DATA_SETTINGS_CHANGED = 27;
    public static final int EVENT_INFO_EDITED = 28;
    public static final int SETTINGS_CHANGED = 29;
    public static final int PREMIUM_PURCHASED = 30;
    public static final int CLOUD_SIGN_IN = 31;
    public static final int VIEW_EVENT_CONFIRMED = 32;
    public static final int IMAGE_EDITED = 33;
    public static final int MAILBOX_EXITED = 34;

    // Calendar
    static final String[] daysOfWeek = {
            "Sat","Sun","Mon","Tue","Wed","Thu","Fri","Sat"
    };
    static final String[] monthsOfYear = {
            "Jun","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec",
    };
}
