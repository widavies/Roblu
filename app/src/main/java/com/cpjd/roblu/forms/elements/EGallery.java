package com.cpjd.roblu.forms.elements;


import android.content.Context;

import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.REvent;

import java.io.File;
import java.util.ArrayList;

import lombok.Data;

/**
 * EGallery supports an array of images. Note, the gallery only stores the IDs, the IDs reference
 * images stored on the file system. Images have to be packaged before being uploaded to anything.
 *
 * @since 3.4.0
 * @author Will Davies
 */

@Data
public class EGallery extends Element {

    private ArrayList<Long> imageIDs;

    public EGallery(String title) {
        super(title);
    }

    public void addImageID(long ID) {
        if(imageIDs == null) imageIDs = new ArrayList<>();
        imageIDs.add(ID);
    }
    public ArrayList<Long> getImageIDs() {
        if(imageIDs == null) imageIDs = new ArrayList<>();
        return imageIDs;
    }

    public void removeAllIds() {
        if(imageIDs != null) imageIDs.clear();
        imageIDs = null;
    }

    public void removeID(long ID) {
        if(imageIDs == null) return;

        for(int i = 0; i < imageIDs.size(); i++) {
            if(imageIDs.get(i) == ID) {
                imageIDs.remove(i);
                break;
            }
        }
    }


    public ArrayList<File> getImagePaths(Context context, REvent event) {
        if(imageIDs == null || imageIDs.size() == 0) return null;

        Loader l = new Loader(context);
        ArrayList<File> files = new ArrayList<>();

        for(int i = 0; i < imageIDs.size(); i++) {
            files.add(l.getImagePath(event.getID(), imageIDs.get(i)));
        }

        return files;
    }

    @Override
    public String getSubtitle() {
        return "Type: Gallery";
    }
}
