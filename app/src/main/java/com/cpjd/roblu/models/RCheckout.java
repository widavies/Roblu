package com.cpjd.roblu.models;

import android.support.annotation.NonNull;

import java.io.Serializable;

import lombok.Data;

/**
 * A model that stores data that will be transferred to the Roblu Cloud server, a hosted server, Bluetooth, or QR codes.
 *
 * This is the 3rd hand-off model iteration, successor to RCheckout and RAssignment
 *
 * More on hand-off:
 * RCheckout is a packaging utility for Roblu. Any time data goes to the server, all data to be transported will be
 * set into the RCheckout class and serialized as a string. However, the ID number should be sent to the server
 * as a SEPARATE PARAMETER, and NOT IN THE SERIALIZED CONTENT
 *
 * Important steps:
 * 1) To flag this handoff as ready for upload, save the handoff to directory /pending/, this will notify the background service that
 * we're ready to upload. After a successful upload attempt, the service will delete the handoff from /pending/. Note: flagging for upload
 * should save twice, once to /checkouts/ and once to /pending/.
 *
 * RCheckout's will still be referred to as checkouts in many places, but they needed a different name for technical implementations.
 * Generally speaking, checkouts refer to the UI implementation, and handoffs refer to the backend.
 *
 * @version 3
 * @since 3.5.9
 * @author Will Davies
 */
@Data
public class RCheckout implements Serializable, Comparable<RCheckout> {

    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;

    /**
     * This number is a universal identification number for the handoff. It shouldn't be serialized when transporting, but sent
     * as a separate parameter. The internal ID should match the external ID on the server. Note: Roblu Master doesn't care about
     * the ID, it doesn't need to be synced there.
     */
    private int ID;

    /**
     * RCheckout is designed specifically for transferring a team object with some meta data. This is the all important,
     * team data object. Take good care of her!
     */
    private RTeam team;

    /*
     * Meta data
     * -Status tags-
     * These are non-critical variables that help describe this handoff to the client and Roblu master.
     * the status int ID is important to prevent some trivial UI problems, but nameTag and time are purely
     * optional additions. They can be flagged at any point in time to any value.
     * Think of these as meta data
     */
    /**
     * Represents the status of this handoff as specified by
     * @see com.cpjd.roblu.utils.HandoffStatus
     */
    private int status;
    /**
     * A name, if applicable, to associate with the handoff status
     */
    private String nameTag;

    /**
     * A time, if applicable to associate with handoff status
     */
    private long time;
    // End status tags

    /**
     * This shouldn't be serialized, but it's a sorting helper tool
     */
    private transient int customRelevance;

    /**
     * The empty constructor is required for de-serialization
     */
    @SuppressWarnings("unused")
    public RCheckout() {}

    /**
     * Creates a handoff object
     * @param team the team to package in this object.
     */
    public RCheckout(RTeam team) {
        this.team = team;
    }

    @Override
    public int compareTo(@NonNull RCheckout handoff) {
        return getTeam().getTabs().get(0).compareTo(handoff.getTeam().getTabs().get(0));
    }

}
