package com.cpjd.roblu.forms.elements;


import org.codehaus.jackson.annotate.JsonTypeName;

import java.util.ArrayList;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * EGallery supports the storing of images as byte[] arrays.
 * The old system used to use files containing bitmaps, but we're now going to use byte[] so that images
 * don't have to be saved and referenced with IDs. This always makes this model more consistent with
 * other models and easier to send over the Roblu Cloud server.
 *
 * @version 2
 * @since 3.4.0
 * @author Will Davies
 */

@Data
@EqualsAndHashCode(callSuper=false)
@JsonTypeName("EGallery")
public class EGallery extends Element {

    private ArrayList<byte[]> images;

    public EGallery() {}

    public EGallery(String title) {
        super(title);
    }

    public void addImage(byte[] image) {
        if(images == null) images = new ArrayList<>();

        images.add(image);
    }

    public void removeImage(int position) {
        if(images != null) images.remove(position);
    }

    public ArrayList<byte[]> getImages() {
        if(images == null) images = new ArrayList<>();
        return images;
    }

    @Override
    public String getSubtitle() {
        return "Type: Gallery";
    }
}
