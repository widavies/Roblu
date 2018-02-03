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
     * An ArrayList is used here because it's a bit easier to manage.
     * Each byte[] represents one image.
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
     * Adds a new image to the image array
     * @param image a byte[] representing the image, format must match that UI requirements
     */
    public void addImage(byte[] image) {
        if(images == null) images = new ArrayList<>();

        images.add(image);
    }

    /**
     * Removes an image from the image array
     * @param position the index position of the image to be removed
     */
    public void removeImage(int position) {
        if(images != null) images.remove(position);
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
        gallery.setRequired(required);
        return gallery;
    }

    @Override
    public String toString() {
        if(images == null) return "";
        return String.valueOf(images.size());
    }
}
