package com.cpjd.roblu.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.cpjd.roblu.utils.Text;

import org.apache.poi.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class Loader extends IO {

    private final Context context;
    public static final String PREFIX = "v11";

    public Loader(Context context) {
        super(context);
        this.context = context;
    }

    public File getNewExportFile() {
        File dir = new File(context.getFilesDir(), PREFIX+ File.separator+"exports"+File.separator);
        // delete the directory
        delete(dir);
        if(!dir.exists()) {
            if(!dir.mkdir()) System.out.println("Failed to create directory");
        }

        return new File(context.getFilesDir(), PREFIX+File.separator+"exports"+File.separator+"ScoutingData.xlsx");
    }

    /**
     *
     * @return true if settings didn't exist and had to be created
     */
    public boolean checkSettings() {
        RSettings settings = loadSettings();
        if(settings == null) {
            settings = new RSettings();
            new Loader(context).saveSettings(settings);
            return true;
        }
        return false;
    }

    public void verifyFileCompatibility() {
        // purge old PREFIXes
        File dir = new File(context.getFilesDir(), File.separator);
        if(dir.listFiles() != null && dir.listFiles().length > 0) for(File file : dir.listFiles()) if(!file.getName().equals(PREFIX)) delete(dir);
    }

    public void duplicate(REvent event, boolean keepScoutingData) {
        long newID = getNewEventID();
        event.setName("Copy of "+event.getName());
        RTeam[] teams = getTeams(event.getID());
        RForm form = loadForm(event.getID());
        event.setID(newID);
        saveEvent(event);
        saveForm(form.duplicate(), newID);
        RTeam temp;
        for(int i =0 ; teams != null && i < teams.length; i++) {
            temp = teams[i].duplicate();
            if(!keepScoutingData) temp.setTabs(null);
            temp.setPage(1);
            saveTeam(temp, newID);
        }
    }

    public int getNumberTeams(long eventID) {
        File[] files = getChildFiles(PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator);
        if(files == null || files.length == 0) return 0;
        return files.length;
    }
    public void saveTeam(RTeam team, long eventID) {
        team.resetSortRelevance(); // this need to be serializable for switching between activites, but we don't really care about saving them
        File file = new File(context.getFilesDir(), PREFIX+File.separator+"events"+ File.separator+eventID+ File.separator+"teams"+File.separator);
        if(!file.exists()) {
            if(!file.mkdir()) System.out.println("Failed to create directory for teams");
        }
        serializeObject(team, PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+ File.separator+team.getID()+".ser");
    }

    public RTeam loadTeam(long eventID, long teamID) {
        return (RTeam) deserializeObject(PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator+teamID+".ser");
    }

    public boolean doesEventExist(long eventID) {
        return new File(context.getFilesDir(), PREFIX+File.separator+"events"+ File.separator+eventID).exists();
    }
    public void deleteTeam(RTeam team, long eventID) {
        for(Long imageID : team.getAllImagesID()) deleteImage(eventID, imageID);
        File file = new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator+team.getID()+".ser");
        delete(file);
    }

    public RForm loadForm(long ID) {
        if(ID == -1) return (RForm) deserializeObject(PREFIX+File.separator+"master_form.ser");

        return (RForm) deserializeObject(PREFIX+File.separator+"events"+File.separator+ID+File.separator+"form.ser");
    }

    public void saveForm(RForm form, long eventID) {
        if(eventID == -1) serializeObject(form, PREFIX+File.separator+"master_form.ser");

        serializeObject(form, PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"form.ser");
    }

    public void saveEvent(REvent event) {
        if(!(new File(context.getFilesDir(), PREFIX+File.separator+"events"+ File.separator+event.getID()+ File.separator)).mkdir()) System.out.println("Failed to create event file");

        serializeObject(event, PREFIX+File.separator+"events"+File.separator+event.getID()+File.separator+"event.ser");
    }

    // Temp storage when exporting
    public File saveBackup(RBackup backup) {
        File dir = new File(context.getFilesDir(), PREFIX+File.separator+"backups"+ File.separator);
        if(!dir.exists()) {
            if(!dir.mkdirs()) System.out.println("Failed to create backups directory");
        }
        File bFile = new File(context.getFilesDir(), PREFIX + File.separator+"backups"+ File.separator+backup.getEvent().getName()+" ("+Text.convertTime(System.currentTimeMillis())+") Backup.roblubackup");
        if(bFile.exists()) delete(bFile);
        serializeObject(backup, bFile);
        return bFile;
    }

    public RBackup convertBackupFile(Uri toCopy) {
        File dir = new File(context.getFilesDir(), PREFIX+ File.separator+"receivedBackups"+File.separator);
        if(!dir.exists()) {
            if(!dir.mkdirs()) System.out.println("Failed to create receivedBackups directory");
        }
        File temp = new File(context.getFilesDir(), PREFIX + File.separator+"receivedBackups"+File.separator+"temp.roblubackup");
        if(temp.exists()) delete(temp);
        try {
            InputStream is = context.getContentResolver().openInputStream(toCopy);
            FileOutputStream out = new FileOutputStream(temp);
            if(is != null) {
                IOUtils.copy(is, out);
                RBackup backup = (RBackup) deserializeObject(temp);
                is.close();
                out.flush();
                out.close();
                return backup;
            }
            return null;
        } catch(Exception e) {
            return null;
        } finally {
            cleanBackupDirs();
        }
    }

    public void cleanBackupDirs() {
        File backups = new File(context.getFilesDir(), PREFIX+File.separator+"backups"+ File.separator);
        File receivedBackups = new File(context.getFilesDir(), PREFIX+ File.separator+"receivedBackups"+File.separator);
        delete(backups);
        delete(receivedBackups);
    }


    public REvent getEvent(long ID) {
        return (REvent) deserializeObject(PREFIX+File.separator+"events"+File.separator+ID+File.separator+"event.ser");
    }

    public void saveSettings(RSettings settings) {
        serializeObject(settings, PREFIX+File.separator+"settings.ser");
    }

    public RSettings loadSettings() {
        return (RSettings) deserializeObject(PREFIX+File.separator+"settings.ser");
    }

    public long getNewEventID() {
        File[] files = getChildFiles(PREFIX+ File.separator+"events"+File.separator);
        if(files == null || files.length == 0) return 0;
        long topID = 0;
        for(File f : files) {
            long newID = Long.parseLong(f.getName().replaceAll(".ser", ""));
            if(newID > topID) topID = newID;
        }
        return topID + 1;

    }

    public int getNewTeamID(long eventID) {
        File[] files = getChildFiles(PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator);
        if(files == null || files.length == 0) return 0;
        int topID = 0;
        for(File f : files) {
            int newID = Integer.parseInt(f.getName().replaceAll(".ser", ""));
            if(newID > topID) topID = newID;
        }

        return topID + 1;
    }
    public long getTeamSize(long eventID, long teamID) {
        File file = new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator+teamID+".ser");
        return file.length() / 1000;
    }

    public void deleteAllTeams(long eventID) {
        File dir = new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator);
        delete(dir);
    }


    public void deleteEvent(long eventID) {
        deleteImages(eventID);

        File dir = new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator);
        delete(dir);

        // Don't forget images!
        deleteImages(eventID);
    }

    public REvent[] getEvents() {
        File[] files = getChildFiles(PREFIX+File.separator+"events"+File.separator);
        if(files == null || files.length == 0) return null;
        REvent[] events = new REvent[files.length];
        for(int i = 0; i < events.length; i++) {
            events[i] = getEvent(Integer.parseInt(files[i].getName()));
        }
        return events;
    }

    public RTeam[] getTeams(long eventID) {
        File[] files = getChildFiles(PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator);
        if(files == null || files.length == 0) return null;
        RTeam[] teams = new RTeam[files.length];
        for(int i = 0; i < teams.length; i++) {
            teams[i] = loadTeam(eventID, Integer.parseInt(files[i].getName().replace(".ser", "")));
        }
        return teams;
    }

    /*
     * Image loading.
     *
     * Here's the jist:
     *
     * -It's not really convienent to save bitmaps within the RTeam model
     * here's where they will be stored: /v7/events/0/0/images/id
     * format: PREFIX/events/eventID/images
     *
     * That means that when we save the gallery element, we just need to save a list of IDs
     *
     *
     */

    public long newImage(Bitmap bitmap, long eventID) {
        // First, verify that we have the correct directory
        File directory = new File(context.getFilesDir(), PREFIX + File.separator+"events"+File.separator+eventID+File.separator+"images"+File.separator);
        if(!directory.exists()) {
            if(!directory.mkdir()) System.out.println("Failed to create images directory");
        }

        long ID = getNewImageID(eventID);

        // Get directory
        File toSave = new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"images"+File.separator+ID+".jpg");

        // Save the image
        FileOutputStream fos = null;
        try {
            RSettings settings = loadSettings();
            fos = new FileOutputStream(toSave);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch(Exception e) {
            System.out.println("Failed to save a newImage()");
        } finally {
            try {
                if(fos != null) fos.close();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return ID;
    }

    public void saveImage(Bitmap bitmap, long eventID, long imageID) {

        // First, verify that we have the correct directory
        File directory = new File(context.getFilesDir(), PREFIX + File.separator+"events"+File.separator+eventID+File.separator+"images"+File.separator);
        if(!directory.exists()) {
            if(!directory.mkdir()) System.out.println("Failed to create images directory");
        }

        // Get directory
        File toSave = new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"images"+File.separator+imageID+".jpg");

        System.out.println("Svaing image"+toSave.getAbsolutePath());

        // Save the image
        FileOutputStream fos = null;
        try {
            RSettings settings = loadSettings();
            fos = new FileOutputStream(toSave);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch(Exception e) {
            System.out.println("Failed to save a image from backup"+e.getMessage());
        } finally {
            try {
                if(fos != null) fos.close();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public File getImagePath(long eventID, long imageID) {
        return new File(context.getFilesDir(), PREFIX + File.separator + "events" + File.separator + eventID + File.separator + "images" + File.separator + imageID + ".jpg");
    }

    public ArrayList<RImage> getImages(long eventID) {
        File dir = new File(context.getFilesDir(), PREFIX + File.separator+"events"+File.separator+eventID+File.separator+"images"+File.separator);
        File[] children = dir.listFiles();
        if(children == null || children.length == 0) return null;
        RSettings settings = new Loader(context).loadSettings();
        ArrayList<RImage> temp = new ArrayList<>();
        for(File f : children) {
            Bitmap bmp = BitmapFactory.decodeFile(f.getPath());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            temp.add(new RImage(Long.parseLong(f.getName().replace(".jpg","")), stream.toByteArray()));
        }
        return temp;
    }

    public ArrayList<RImage> getImages(long eventID, ArrayList<Long> IDs) {
        if(IDs == null || IDs.size() == 0) return null;
        File[] children = new File[IDs.size()];
        for(int i = 0; i < IDs.size(); i++) children[i] = new File(context.getFilesDir(), PREFIX + File.separator+"events"+File.separator+eventID+File.separator+IDs.get(i)+File.separator+"images"+File.separator);
        RSettings settings = new Loader(context).loadSettings();
        ArrayList<RImage> temp = new ArrayList<>();
        for(File f : children) {
            Bitmap bmp = BitmapFactory.decodeFile(f.getPath());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            temp.add(new RImage(Long.parseLong(f.getName().replace(".jpg","")), stream.toByteArray()));
        }
        return temp;
    }

    public void saveImages(long eventID, ArrayList<RImage> images) {
        if(images == null || images.size() == 0) return;
        for(int i = 0; i < images.size(); i++) {
            saveImage(BitmapFactory.decodeByteArray(images.get(i).getBytes(), 0, images.get(i).getBytes().length), eventID, images.get(i).getID());
        }
    }

    private void deleteImage(long eventID, long imageID) {
        File directory = new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"images"+ File.separator+imageID+".jpg");
        delete(directory);
    }

    public long getNewImageID(long eventID) {
        File directory = new File(context.getFilesDir(), PREFIX + File.separator+"events"+File.separator+eventID+File.separator+"images"+File.separator);

        File[] children = directory.listFiles();

        if(children == null || children.length == 0) return 0;

        long maxID = 0;
        for(File f : children) {
            if(Integer.parseInt(f.getName().replace(".jpg", "")) > maxID) maxID = Integer.parseInt(f.getName().replace(".jpg" ,""));
        }

        return maxID + 1;
    }

    public int getNumberImages(long eventID) {
        File directory = new File(context.getFilesDir(), PREFIX + File.separator+"events"+File.separator+eventID+File.separator+"images"+File.separator);
        if(!directory.exists()) return 0;
        File[] children = directory.listFiles();
        if(children == null) return 0;
        return children.length;
    }

    public void deleteImages(long eventID) {
        File directory = new File(context.getFilesDir(), PREFIX + File.separator+"events"+File.separator+eventID+File.separator+"images"+File.separator);
        delete(directory);
    }

    public File getTempPictureFile() {
        File directory = new File(context.getFilesDir(), PREFIX + File.separator+"temp"+File.separator);
        if(!directory.exists())
            if(!directory.mkdir()) System.out.println("Failed to create temporary picture file");
        File path = new File(context.getFilesDir(), PREFIX + File.separator +"temp"+File.separator+"temp.jpg");
        if(path.exists()) {
            if(!path.delete()) System.out.println("Failed to delete temp picture file");
        }
        try {
            if(!path.createNewFile()) System.out.println("Failed to create new temp picture file");
        } catch(Exception e) { return null; }
        return path;
    }

    /**
     * CHECKOUT CONFLICTS
     */
    public void saveCheckoutConflict(RCheckout checkout) {
        serializeObject(checkout, PREFIX+File.separator+"checkoutsconflicts"+File.separator+checkout.getID()+".ser");
    }

    public RCheckout loadCheckoutConflict(long checkoutID) {
        return (RCheckout) deserializeObject(PREFIX+File.separator+"checkoutsconflicts"+File.separator+checkoutID+".ser");
    }

    public RCheckout[] loadCheckoutConflicts() {
        File[] files = getChildFiles(PREFIX+File.separator+"checkoutsconflicts"+File.separator);
        if(files == null || files.length == 0) return null;
        RCheckout[] checkouts = new RCheckout[files.length];
        for(int i = 0; i < checkouts.length; i++) {
            checkouts[i] = loadCheckoutConflict(Integer.parseInt(files[i].getName().replace(".ser", "")));
        }
        return checkouts;
    }

    public long getNewCheckoutConflictID() {
        File[] files = getChildFiles(PREFIX+ File.separator+"checkoutsconflicts"+File.separator);
        if(files == null || files.length == 0) return 0;
        long topID = 0;
        for(File f : files) {
            long newID = Long.parseLong(f.getName().replaceAll(".ser", ""));
            if(newID > topID) topID = newID;
        }
        return topID + 1;
    }

    /**
     * MERGED CHECKOUTS
     */
    public void saveCheckout(RCheckout checkout) {
        serializeObject(checkout, PREFIX+File.separator+"checkouts"+File.separator+checkout.getID()+".ser");
    }

    public RCheckout loadCheckout(long checkoutID) {
        return (RCheckout) deserializeObject(PREFIX+File.separator+"checkouts"+File.separator+checkoutID+".ser");
    }

    public RCheckout[] loadCheckouts() {
        File[] files = getChildFiles(PREFIX+File.separator+"checkouts"+File.separator);
        if(files == null || files.length == 0) return null;
        RCheckout[] checkouts = new RCheckout[files.length];
        for(int i = 0; i < checkouts.length; i++) {
            checkouts[i] = loadCheckout(Integer.parseInt(files[i].getName().replace(".ser", "")));
        }
        return checkouts;
    }

    public long getNewCheckoutID() {
        File[] files = getChildFiles(PREFIX+ File.separator+"checkouts"+File.separator);
        if(files == null || files.length == 0) return 0;
        long topID = 0;
        for(File f : files) {
            long newID = Long.parseLong(f.getName().replaceAll(".ser", ""));
            if(newID > topID) topID = newID;
        }
        return topID + 1;
    }
}
