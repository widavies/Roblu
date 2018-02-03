package com.cpjd.roblu.io;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cpjd.roblu.models.RBackup;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RCloudSettings;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.utils.Utils;

import org.apache.poi.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * IO manages interactions with the file system, mainly be serializing/de-serializing models
 *
 * Directories & files managed by IO:
 * -/PREFIX/events
 * -/PREFIX/events/[EVENT_ID]/teams
 * -/PREFIX/checkouts
 * -/PREFIX/pending
 * -/PREFIX/events[EVENT_ID]/form
 * -/PREFIX/settings.ser
 * -/PREFIX/master_form.ser
 *
 * Cache directory:
 * -/PREFIX/tempBackupImport.ser
 * -/PREFIX/tempBackupExport.ser
 * -/PREFIX/tempImage.jpg
 *
 *
 * @version 4
 * @since 1.0.0
 * @author Will Davies
 *
 */
@SuppressWarnings("unused")
public class IO {

    private Context context;
    public static final String PREFIX = "v14";

    public IO(Context context) {
        this.context = context;
    }

    /**
     * Must be called at application startup, ASAP
     *
     * Does the following:
     * -Makes sure settings file exists, if not, creates default
     * -Ensures /checkouts/ exists
     * -Removes old data (from older prefixes)
     *
     * @return true if this is first launch (based on if the settings file had to be created)
     */
    public static boolean init(Context context) {
        // Create prefix directory
        if(!new File(context.getFilesDir(), PREFIX).exists()) {
            if(new File(context.getFilesDir(), PREFIX).mkdir()) Log.d("RBS", "Prefix dir could not be created.");
        }

        /*
         * Create parent directories
         */
        File eventDir = new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator);
        File checkoutsDir = new File(context.getFilesDir(), PREFIX+File.separator+"checkouts"+File.separator);
        File pending = new File(context.getFilesDir(), PREFIX+File.separator+"pending"+File.separator);

        if(!eventDir.exists()) {
            if(eventDir.mkdir()) Log.d("RBS", "/events/ dir successfully created.");
        }
        if(!checkoutsDir.exists()) {
            if(checkoutsDir.mkdir()) Log.d("RBS", "/checkouts/ dir successfully created.");
        }
        if(!pending.exists()) {
            if(pending.mkdir()) Log.d("RBS", "/pending/ dir successfully created.");
        }

        // Purge old data
        File dir = new File(context.getFilesDir(), File.separator);
        if(dir.listFiles() != null && dir.listFiles().length > 0) for(File file : dir.listFiles()) if(!file.getName().equals(PREFIX)) delete(dir);

        // Check settings
        RSettings settings = new IO(context).loadSettings();
        if(settings == null) {
            settings = new RSettings();
            settings.setRui(new RUI());
            settings.setMaster(Utils.createEmpty());
            new IO(context).saveCloudSettings(new RCloudSettings());
            new IO(context).saveSettings(settings);
            return true;
        }
        return false;
    }

    /*
     * SETTINGS METHODS
     */
    /**
     * Save a settings reference to internal storage
     * @param settings RSettings object instance
     */
    public void saveSettings(RSettings settings) {
        serializeObject(settings, new File(context.getFilesDir(), PREFIX+File.separator+"settings.ser"));
    }

    /**
     * Load settings object from internal storage
     * @return RSettings object instance
     */
    public RSettings loadSettings() {
        return (RSettings) deserializeObject(new File(context.getFilesDir(), PREFIX+File.separator+"settings.ser"));
    }

    /**
     * Load a cloud settings object from internal storage
     * @return RCloudSettings object instance
     */
    public RCloudSettings loadCloudSettings() {
        return (RCloudSettings) deserializeObject(new File(context.getFilesDir(), PREFIX+File.separator+"cloudSettings.ser"));
    }

    /**
     * Save a cloud settings reference to internal storage
     * @param settings RCloudSettings object instance
     */
    public void saveCloudSettings(RCloudSettings settings) {
        serializeObject(settings, new File(context.getFilesDir(), PREFIX+File.separator+"cloudSettings.ser"));
    }

    // End settings methods

    /*
     * EVENTS METHODS
     */
    /**
     * Loads the specified event from the file system
     * @param ID the unique of ID of the event to load
     * @return REvent object instance
     */
    public REvent loadEvent(int ID) {
        return (REvent) deserializeObject(new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+ID+File.separator+"event.ser"));
    }

    /**
     * Returns an unused, new event ID that a new event can be saved under.
     * This method will take the HIGHEST ID it finds, and add one to it. It will
     * not just find the closest unused ID to 0.
     * @return unused ID to save this event's info to
     */
    public int getNewEventID() {
        File[] files = getChildFiles(new File(context.getFilesDir(), PREFIX+ File.separator+"events"+File.separator));
        if(files == null || files.length == 0) return 0;
        int topID = 0;
        for(File f : files) {
            int newID = Integer.parseInt(f.getName().replaceAll(".ser", ""));
            if(newID > topID) topID = newID;
        }
        return topID + 1;

    }

    /**
     * Saves the event to directory /events/ID/event.ser
     * dir /events/ID will be created if it hasn't been yet
     * @param event the event object to save
     */
    public void saveEvent(REvent event) {
        File file = new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+event.getID()+File.separator+"event.ser");
        if(!file.getParentFile().exists()) {
            if(file.getParentFile().mkdir()) Log.d("RBS", "Event directory successfully created for event with ID: "+event.getID());
        }
        serializeObject(event, file);
    }

    /**
     * Checks if the specified event exists
     * @param eventID the event ID that is being checked for existence
     * @return true if the event exists
     */
    public boolean doesEventExist(int eventID) {
        return new File(context.getFilesDir(), PREFIX+File.separator+"events"+ File.separator+eventID).exists();
    }

    /**
     * Deletes the event from the file system
     * @param eventID the event ID associated with the event that is to be deleted
     */
    public void deleteEvent(int eventID) {
        File dir = new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator);
        delete(dir);
    }

    /**
     * Loads all events in the /events/ dir
     * @return array of REvents found on the system
     */
    public REvent[] loadEvents() {
        File[] files = getChildFiles(new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator));
        if(files == null || files.length == 0) return null;
        REvent[] events = new REvent[files.length];
        for(int i = 0; i < events.length; i++) {
            events[i] = loadEvent(Integer.parseInt(files[i].getName()));
        }
        return events;
    }

    /**
     * Duplicates an event. The EVENT MUST BE RELOADED, never ever return the REvent
     * reference.
     * @param event the REvent to duplicate
     * @param keepScoutingData true if scouting data should be preserved
     * @return reference to the duplicated event
     */
    public REvent duplicateEvent(REvent event, boolean keepScoutingData) {
        int newID = getNewEventID();
        // New name
        event.setName("Copy of "+event.getName());
        // Set teams
        RTeam[] teams = loadTeams(event.getID());
        // Set forms
        RForm form = loadForm(event.getID());
        event.setID(newID);
        saveEvent(event);
        saveForm(newID, form);

        RTeam temp;
        for(int i = 0; teams != null && i < teams.length; i++) {
            temp = teams[i];
            if(!keepScoutingData) temp.setTabs(null);
            temp.setPage(1);
            saveTeam(newID, temp);
        }
        return event;
    }
    // End event methods

    /*
     * TEAMS METHODS
     */
    /**
     * Loads the specified team from /events/[EVENT-ID]/teams/
     * @param eventID the ID of the event that contains the desired team
     * @param teamID the ID of the team to load
     * @return RTeam object instance
     */
    public RTeam loadTeam(int eventID, int teamID) {
        return (RTeam) deserializeObject(new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator+teamID+".ser"));
    }

    /**
     * Returns an unused, new event ID that a new team can be saved under.
     * This method will take the HIGHEST ID it finds, and add one to it. It will
     * not just find the closest unused ID to 0.
     * @return unused ID to save this team's info to
     */
    public int getNewTeamID(int eventID) {
        File[] files = getChildFiles(new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator));
        if(files == null || files.length == 0) return 0;
        int topID = 0;
        for(File f : files) {
            int newID = Integer.parseInt(f.getName().replaceAll(".ser", ""));
            if(newID > topID) topID = newID;
        }
        return topID + 1;
    }

    /**
     * Deletes the specified team
     * @param eventID the event ID that contains the team to be deleted
     * @param teamID the ID of the team to delete
     */
    public void deleteTeam(int eventID, int teamID) {
        File file = new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator+teamID+".ser");
        delete(file);
    }

    /**
     * Saves the specified team
     *
     * This method will create the /teams/ sub-event-dir if necessary
     * @param eventID the event ID to save the team under
     * @param team the team to save
     */
    public void saveTeam(int eventID, RTeam team) {
        File file = new File(context.getFilesDir(), PREFIX+File.separator+"events"+ File.separator+eventID+ File.separator+"teams"+File.separator+team.getID()+".ser");
        if(!file.getParentFile().exists()) {
            if(file.getParentFile().mkdir()) Log.d("RBS", "Team directory successfully created for event with ID: "+team.getID());
        }
        serializeObject(team, new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator+team.getID()+".ser"));
    }

    /**
     * Gets the number of teams in the specified event
     * @param eventID the event ID to get the team count from
     * @return number of teams within the specified event
     */
    public int getNumberTeams(int eventID) {
        File[] files = getChildFiles(new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator));
        if(files == null || files.length == 0) return 0;
        return files.length;
    }

    /**
     * Gets the literal size of a team file, in Kilobytes
     * @param eventID the event ID the team is contained under
     * @param teamID the team ID to get the file size of
     * @return file size of team file, in kilobytes
     */
    public long getTeamSize(int eventID, int teamID) {
        File file = new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator+teamID+".ser");
        return file.length() / 1000;
    }

    /**
     * Deletes all the teams within a specified event
     * @param eventID the eventID to delete all teams from
     */
    public void deleteAllTeams(int eventID) {
        File dir = new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator);
        delete(dir);
    }

    /**
     * Loads all the teams contained within an event
     * @param eventID the eventID to load all the teams from
     * @return an RTeam object instance array
     */
    public RTeam[] loadTeams(int eventID) {
        File[] files = getChildFiles(new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"teams"+File.separator));
        if(files == null || files.length == 0) return null;
        RTeam[] teams = new RTeam[files.length];
        for(int i = 0; i < teams.length; i++) {
            teams[i] = loadTeam(eventID, Integer.parseInt(files[i].getName().replace(".ser", "")));
        }
        return teams;
    }
    // End team methods

    /*
     * FORM Methods
     */
    /**
     * Loads the specified form from the file system
     * @param eventID the ID of the event to load the form from, USE ID == -1 to load the master form
     * @return RForm object instance
     */
    public RForm loadForm(int eventID) {
        if(eventID == -1) return (RForm) deserializeObject(new File(context.getFilesDir(), PREFIX+File.separator+"master_form.ser"));

        return (RForm) deserializeObject(new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"form.ser"));
    }

    /**
     * Saves the form to the file system
     * @param eventID the ID of the event to save the form to, USE ID == -1 to save the master form
     * @param form the form to save
     */
    public void saveForm(int eventID, RForm form) {
        if(eventID == -1) serializeObject(form, new File(context.getFilesDir(), PREFIX+File.separator+"master_form.ser"));

        serializeObject(form, new File(context.getFilesDir(), PREFIX+File.separator+"events"+File.separator+eventID+File.separator+"form.ser"));
    }
    // End form methods

    /*
     * Backup methods
     */

    /**
     * Saves a backup file to the cache directory. It should be saved to an external location by the user
     * IMMEDIATELY, because the cache dir does not guarantee the existence of any files
     * @param backup the backup file to save to a temporary file
     * @return File reference to a temporary file location that can later be saved to an external location
     */
    public File saveBackup(RBackup backup) {
        File file = new File(context.getCacheDir(), PREFIX+File.separator+"backups"+ File.separator+"tempBackupExport.roblubackup");
        if(file.mkdirs()) Log.d("RBS", "Successfully created backup parent dirs");
        if(file.exists()) delete(file);
        serializeObject(backup, file);
        return file;
    }

    /**
     * Converts a backup file (external) into an RBackup object instance
     * @param toCopy the Uri of the external file the user selected
     * @return RBackup object instance
     */
    public RBackup convertBackupFile(Uri toCopy) {
        File file = new File(context.getCacheDir(), PREFIX+ File.separator+"tempBackupImport.roblubackup");
        if(file.mkdirs()) Log.d("RBS", "Successfully created backup parent dirs");
        if(file.exists()) {
            if(!file.delete()) Log.d("RBS", "Failed to delete old cached backup file.");
        }
        try {
            InputStream is = context.getContentResolver().openInputStream(toCopy);
            FileOutputStream out = new FileOutputStream(file);
            if(is != null) {
                IOUtils.copy(is, out);
                RBackup backup = (RBackup) deserializeObject(file);
                is.close();
                out.flush();
                out.close();
                return backup;
            }
            return null;
        } catch(Exception e) {
            return null;
        } finally {
            if(file.delete()) Log.d("RBS", "Cached backup file successfully deleted.");
        }
    }
    // End backup methods

    /*
     * CSV Methods
     */

    /**
     * Gets a temporary file for the CSV object that should be moved to external storage by the user
     * @return File reference to a temporary file location that can be saved to an external location
     */
    public File getNewCSVExportFile() {
        File f = new File(context.getCacheDir(), PREFIX+File.separator+"exports"+File.separator+"ScoutingData.xlsx");
        if(f.exists()) {
            if(!f.delete()) Log.d("RBS", "Failed to delete old cached csv export file.");
        }
        if(f.getParentFile().mkdirs()) Log.d("RBS", "Successfully created temporary .csv export directory.");
        try {
            if(f.createNewFile()) Log.d("RBS", "File created successfully.");
        } catch(IOException e) {
            e.printStackTrace();
        }
        return f;
    }
    // End CSV Methods

    /*
     * CHECKOUTS Methods
     */
    /**
     * Saves a checkout to the /checkouts/ directory, presumably because an import has occured
     * @param checkout the RCheckout object instance to save
     */
    public void saveCheckout(RCheckout checkout) {
        serializeObject(checkout, new File(context.getFilesDir(), PREFIX+File.separator+"checkouts"+File.separator+checkout.getID()+".ser"));
    }

    /**
     * Loads the RCheckout with the specified ID
     * @param checkoutID the checkout ID to laod
     * @return RCheckout object instance
     */
    private RCheckout loadCheckout(int checkoutID) {
        RCheckout checkout = (RCheckout) deserializeObject(new File(context.getFilesDir(), PREFIX+File.separator+"checkouts"+File.separator+checkoutID+".ser"));
        if(checkout != null) checkout.setID(checkoutID);
        return checkout;
    }

    /**
     * Loads all checkouts in the file system
     * @return Array of RCheckout object instances
     */
    public RCheckout[] loadCheckouts() {
        File[] files = getChildFiles(new File(context.getFilesDir(), PREFIX+File.separator+"checkouts"+File.separator));
        if(files == null || files.length == 0) return null;
        RCheckout[] checkouts = new RCheckout[files.length];
        for(int i = 0; i < checkouts.length; i++) {
            checkouts[i] = loadCheckout(Integer.parseInt(files[i].getName().replace(".ser", "")));
        }
        return checkouts;
    }

    /**
     * Returns an unused, new event ID that a new team can be saved under.
     * This method will take the HIGHEST ID it finds, and add one to it. It will
     * not just find the closest unused ID to 0.
     * @return unused ID to save this team's info to
     */
    public int getNewCheckoutID() {
        File[] files = getChildFiles(new File(context.getFilesDir(), PREFIX+ File.separator+"checkouts"+File.separator));
        if(files == null || files.length == 0) return 0;
        int topID = 0;
        for(File f : files) {
            int newID = Integer.parseInt(f.getName().replaceAll(".ser", ""));
            if(newID > topID) topID = newID;
        }
        return topID + 1;
    }
    // End checkouts methods

    /*
     * PENDING methods
     */

    /**
     * Saves a checkout to the /pending/ directory, presumably because an import has occurred
     * @param checkout the RCheckout instance to save
     */
    public void savePendingObject(RCheckout checkout) {
        serializeObject(checkout, new File(context.getFilesDir(), PREFIX+File.separator+"pending"+File.separator+checkout.getID()+".ser"));
    }

    /**
     * Loads the RCheckout with the specified ID
     * @param checkoutID the checkout ID to laod
     * @return RCheckout object instance
     */
    public RCheckout loadPendingCheckout(int checkoutID) {
        RCheckout checkout = (RCheckout) deserializeObject(new File(context.getFilesDir(), PREFIX+File.separator+"pending"+File.separator+checkoutID+".ser"));
        if(checkout != null) checkout.setID(checkoutID);
        return checkout;
    }

    /**
     * Loads all checkouts in the file system
     * @return Array of RCheckout object instances
     */
    public RCheckout[] loadPendingCheckouts() {
        File[] files = getChildFiles(new File(context.getFilesDir(), PREFIX+File.separator+"pending"+File.separator));
        if(files == null || files.length == 0) return null;
        RCheckout[] checkouts = new RCheckout[files.length];
        for(int i = 0; i < checkouts.length; i++) {
            checkouts[i] = loadCheckout(Integer.parseInt(files[i].getName().replace(".ser", "")));
        }
        return checkouts;
    }

    /**
     * Returns an unused, new event ID that a new team can be saved under.
     * This method will take the HIGHEST ID it finds, and add one to it. It will
     * not just find the closest unused ID to 0.
     * @return unused ID to save this team's info to
     */
    public int getNewPendingCheckoutID() {
        File[] files = getChildFiles(new File(context.getFilesDir(), PREFIX+ File.separator+"pending"+File.separator));
        if(files == null || files.length == 0) return 0;
        int topID = 0;
        for(File f : files) {
            int newID = Integer.parseInt(f.getName().replaceAll(".ser", ""));
            if(newID > topID) topID = newID;
        }
        return topID + 1;
    }

    /**
     * Deletes a checkout from /pending/, presumably because it was uploaded successfully
     * @param ID the ID of the checkout to delete
     */
    public void deletePendingCheckout(int ID) {
        delete(new File(context.getFilesDir(), PREFIX+File.separator+"pending"+File.separator+ID+".ser"));
    }
    // End pending methods

    /**
     * Deletes all checkouts and pending checkouts, presumably because the event has been flagged as in-active by the user
     */
    public void clearCheckouts() {
        File checkoutsDir = new File(context.getFilesDir(), PREFIX + File.separator+"checkouts"+File.separator);
        if(checkoutsDir != null && checkoutsDir.listFiles() != null) {
            for(File f : checkoutsDir.listFiles()) {
                delete(f);
            }
        }
        File pendingDir = new File(context.getFilesDir(), PREFIX+File.separator+"pending"+File.separator);
        if(pendingDir != null && pendingDir.listFiles() != null) {
            for(File f : pendingDir.listFiles()) {
                delete(f);
            }
        }
    }

    // ********************UTILITY METHODS**************************
    /**
     * Gets a temporary picture file for usage with the camera
     * @return returns file where the picture can be stored temporarily
     */
    public File getTempPictureFile() {
        File f = new File(context.getCacheDir(), PREFIX+File.separator+"images"+File.separator+"image.jpg");
        if(f.exists()) {
            if(!f.delete()) Log.d("RBS", "Failed to delete old cached image file.");
        }
        if(f.getParentFile().mkdirs()) Log.d("RBS", "Successfully created temporary image directory.");
        try {
            if(f.createNewFile()) Log.d("RBS", "File created successfully.");
        } catch(IOException e) {
            e.printStackTrace();
        }
        return f;
    }

    /**
     * Returns a list of the files within a folder
     * @param location the location of the folder to get children contents from
     * @return a list of the files in the target folder
     */
    private File[] getChildFiles(File location) {
        return location.listFiles();
    }

    /**
     * Convert an object instance into a file
     * @param object object instance to write
     * @param location location to write the file to
     */
    private void serializeObject(Object object, File location) {
        try {
            FileOutputStream fos = new FileOutputStream(location);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(object);
            out.close();
            fos.close();
        } catch(Exception e) {
            Log.d("RBS", "Failed to serialize object at location "+location+" err msg: "+e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Convert a file into an object instance
     * @param location location of the file
     * @return object instance with the corresponding type in /models/
     */
    private Object deserializeObject(File location) {
        try {
            FileInputStream fis = new FileInputStream(location);
            ObjectInputStream in = new ObjectInputStream(fis);
            Object o = in.readObject();
            in.close();
            fis.close();
            return o;
        } catch(Exception e) {
            Log.d("RBS", "Failed to deserialize object at location "+location+", err msg: "+e.getMessage());
            return null;
        }
    }

    /**
     * Recursively delete a folder and all of its contents
     * @param folder the folder to delete
     */
    private static void delete(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    delete(f);
                } else {
                    if(f.delete()) Log.d("RBS", f.getAbsolutePath()+" was deleted successfully.");
                }
            }
        }
        if(folder.delete()) Log.d("RBS", folder.getAbsolutePath() +" was deleted successfully.");
    }
}
