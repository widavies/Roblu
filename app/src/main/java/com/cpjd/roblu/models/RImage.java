package com.cpjd.roblu.models;

import java.io.Serializable;

import lombok.Data;

/**
 * When images are converted to a byte[] to store in a file or send to the Roblu Cloud server,
 * they need to save a reference to the ID that the gallery is storing for them
 *
 * @since 3.5.5
 */
@Data
public class RImage implements Serializable {

    private long ID;
    private final byte[] bytes;

    public RImage(long ID, byte[] bytes) {
        this.bytes = bytes;
        this.ID = ID;
    }
}
