package com.cpjd.roblu.models.metrics;

import org.codehaus.jackson.annotate.JsonTypeName;

import java.util.ArrayList;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents an array of images
 * @see RMetric for more information
 *
 * Note: Modern camera's take high quality pictures. Every picture taken has to be synced across
 * a lot of devices. Pictures are heavy on the server and device data networks. To keep Roblu open
 * and fit for user's needs, we'll allow unlimited pictures. However, all server and client bound
 * traffic will be compressed.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("RGallery")
public class RGallery extends RMetric {
    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;

    /**
     * This array stores the local ID of the pictures in this gallery, upon a
     * a sync, local pictures should be loaded into the byte[] images array below. This should IMMEDIATELY be declared
     * merged into local files, and then set to null.
     */
    private ArrayList<Integer> pictureIDs;

    /**
     * An ArrayList is used here because it's a bit easier to manage.
     * Each byte[] represents one image. This array should only be populated when this gallery is
     * being synced, otherwise, it needlessly uses up memory to keep multiple teams' images in memory when the
     * gallery isn't being used. After a merge, this should be overwritten to null.
     */
    private ArrayList<byte[]> images;

    /**
     * The empty constructor is required for de-serialization
     */
    @SuppressWarnings("unused")
    public RGallery() {}

    /**
     * Instantiates a gallery element
     * @param ID the unique identifier for this object
     * @param title object title
     */
    public RGallery(int ID, String title) {
        super(ID, title);
    }

    /**
     * Returns the image array, ensuring that it's not null
     * @return array containing all images
     */
    public ArrayList<byte[]> getImages() {
        if(images == null) images = new ArrayList<>();
        return images;
    }

    @Override
    public String getFormDescriptor() {
        return "Type: Gallery";
    }

    @Override
    public RMetric clone() {
        RGallery gallery = new RGallery(ID, title);
        return gallery;
    }

    @Override
    public String toString() {
        if(images == null) return "";
        return String.valueOf(images.size());
    }
}
