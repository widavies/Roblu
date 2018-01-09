package com.cpjd.roblu.models;

import android.support.annotation.NonNull;

import java.io.Serializable;

import lombok.Data;

/**
 * The model for an REvent, stores some general information about an event.
 *
 * @since 3.0.0
 * @author Will Davies
 */
@Data
public class REvent implements Serializable, Comparable<REvent> {

    /**
     * The event's ID, this is what differentiates duplicate events and is
     * used for saving the event to the file system
     */
    private int ID;

    /**
     * Attributes of the event
     */
    private String name;
    private long startTime;
    private long endTime;

    /**
     * This is the TBA key of the event, used for automatically syncing stuff
     */
    private String key;
    /**
     * The last filter that was used to sort this team, so when the user relaunches
     * the app, they're still sorting in the most recently used sort scheme. This sorting
     * filter applies to the team list
     */
    private int lastFilter;
    /**
     * Whether this event is currently being managed by Roblu Cloud
     */
    private boolean cloudEnabled;

    public REvent(int ID, String name, long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.ID = ID;
        this.name = name;
    }

    @Override
    public int compareTo(@NonNull REvent event) {
        if(ID > event.getID())  return 0;
        else return 1;
    }
}
