/*
 * ******************************************************
 *  * Copyright (C) 2016 Will Davies wdavies973@gmail.com
 *  *
 *  * This file is part of Roblu
 *  *
 *  * Roblu cannot be distributed for a price or to people outside of your local robotics team.
 *  ******************************************************
 */

package com.cpjd.roblu.ui.events;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.cpjd.http.Request;
import com.cpjd.models.Event;
import com.cpjd.requests.CloudTeamRequest;
import com.cpjd.roblu.R;
import com.cpjd.roblu.csv.CSVActivity;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RBackup;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.models.metrics.RFieldData;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.sync.cloud.InitPacker;
import com.cpjd.roblu.sync.qr.QrReader;
import com.cpjd.roblu.tba.ImportEvent;
import com.cpjd.roblu.tba.ManualScheduleImporter;
import com.cpjd.roblu.tba.SyncTBAEvent;
import com.cpjd.roblu.tba.TBALoadEventsTask;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.ui.dialogs.FastDialogBuilder;
import com.cpjd.roblu.ui.forms.FormViewer;
import com.cpjd.roblu.ui.settings.customPreferences.RUICheckPreference;
import com.cpjd.roblu.ui.teams.TeamsView;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.EventMergeTask;
import com.cpjd.roblu.utils.Utils;
import com.google.common.io.Files;

import java.io.File;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * EventSettings manages event specific settings
 *
 * @author Will Davies
 * @version 2
 * @since 3.0.0
 */
public class EventSettings extends AppCompatActivity {
    /**
     * The REvent reference whose settings are being modified
     */
    private static REvent event;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_settings);

        /*
         * Receive parameters
         */
        event = (REvent) getIntent().getSerializableExtra("event");

        /*
         * Setup UI
         */
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(event.getName());
            getSupportActionBar().setSubtitle("Event settings");
        }

        // Bind event to the UI
        getFragmentManager().beginTransaction()
                .replace(R.id.blankFragment, new SettingsFragment())
                .commit();

        // Bind general user UI preferences
        new UIHandler(this, toolbar).update();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            Intent intent = new Intent();
            intent.putExtra("eventID", event.getID());
            setResult(Constants.EVENT_SETTINGS_CHANGED, intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("eventID", event.getID());
        setResult(Constants.EVENT_SETTINGS_CHANGED, intent);
        finish();
    }

    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

        /**
         * Store an RUI reference here to give all dialogs below access to user color preferences
         */
        private RUI rui;

        /**
         * Helper variable for the Backup task
         */
        private File backupFile;

        private RUICheckPreference cloud;

        /**
         * The progress dialog for TBA match sync
         */
        private ProgressDialog tbaSyncDialog;

        private int fileChooserMode;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            rui = new IO(getActivity()).loadSettings().getRui();

            /*
             * Setup UI
             */
            addPreferencesFromResource(R.xml.event_preferences);

            /*
             * Set appropriate preference listeners to the preferences
             */
            findPreference("edit_form").setOnPreferenceClickListener(this);
            findPreference("export_csv").setOnPreferenceClickListener(this);
            findPreference("backup").setOnPreferenceClickListener(this);
            findPreference("duplicate").setOnPreferenceClickListener(this);
            findPreference("edit_event").setOnPreferenceClickListener(this);
            findPreference("delete_teams").setOnPreferenceClickListener(this);
            findPreference("delete_event").setOnPreferenceClickListener(this);
            findPreference("tba_sync").setOnPreferenceClickListener(this);
            findPreference("import_schedule").setOnPreferenceClickListener(this);
            findPreference("merge_events").setOnPreferenceClickListener(this);
            findPreference("qr").setOnPreferenceClickListener(this);
            RUICheckPreference bt = (RUICheckPreference) findPreference("bt_sync");
            bt.setChecked(event.isBluetoothEnabled());
            bt.setOnPreferenceChangeListener(this);

            /*
             * Obtain explicit preference references where an attribute of the preference
             * needs to be programmatically set
             */
            Preference teams = findPreference("delete_teams");
            Preference deleteEvent = findPreference("delete_event");
            cloud = (RUICheckPreference) findPreference("sync");
            cloud.setChecked(event.isCloudEnabled());
            cloud.setOnPreferenceChangeListener(this);

            /*
             * Set the info to the UI that needs to be
             */

            teams.setSummary("Delete " + new IO(getActivity()).getNumberTeams(event.getID()) + " teams");
            deleteEvent.setSummary("Delete [" + event.getName() + "] event");

        }

        /**
         * The user tapped on one of the available preferences
         *
         * @param preference the preference that was tapped
         * @return true if the event was consumed
         */
        @Override
        public boolean onPreferenceClick(final Preference preference) {
            /*
             * User clicked on the "Edit form" preference.
             * Keep in mind, we'll need to listen to the FormViewer activity's return
             */
            if(preference.getKey().equals("edit_form")) {
                Intent intent = new Intent(getActivity(), FormViewer.class);
                intent.putExtra("form", new IO(getActivity()).loadForm(event.getID()));
                intent.putExtra("editing", true); // editing will be true
                startActivityForResult(intent, Constants.GENERAL);
            } else if(preference.getKey().equals("qr")) { // import QR code checkout
                Intent qrScanIntent = new Intent(getActivity(), QrReader.class);
                qrScanIntent.putExtra("event", event);
                startActivityForResult(qrScanIntent, Constants.QR_REQUEST);
            }
            // Manual schedule importer
            else if(preference.getKey().equals("import_schedule")) {
                fileChooserMode = 1;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(
                            Intent.createChooser(intent, "Select a schedule file"),
                            Constants.FILE_CHOOSER);
                } catch (android.content.ActivityNotFoundException ex) {
                    Utils.showSnackbar(getActivity().findViewById(R.id.activity_create_event_picker), getActivity(), "No file manager found", true, 0);
                }
            }
            // User selected "merge events" option
            else if(preference.getKey().equals("merge_events")) {
                Utils.launchEventPickerWithExcludedEvent(getActivity(), event.getID(), new EventDrawerManager.EventSelectListener() {
                    @Override
                    public void eventSelected(REvent selected) {
                        final ProgressDialog pd = ProgressDialog.show(getActivity(), "Merging events...", "Please wait...", false);
                        new EventMergeTask(new IO(getActivity()), event.getID(), selected.getID(), new EventMergeTask.EventMergeListener() {
                            @Override
                            public void error() {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getActivity(), "An error occurred while merging events.", Toast.LENGTH_LONG).show();
                                    }
                                });
                                pd.dismiss();
                            }

                            @Override
                            public void success() {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getActivity(), "Successfully merged events.", Toast.LENGTH_LONG).show();
                                    }
                                });
                                pd.dismiss();
                            }
                        }).start();

                    }
                });
            }
            /*
             * User clicked the "Edit event info" preference.
             * Keep in mind, we'll need to listen to the EventEditor for changes
             */
            else if(preference.getKey().equals("edit_event")) {
                Intent intent = new Intent(getActivity(), EventEditor.class);
                intent.putExtra("editing", true);
                intent.putExtra("name", event.getName());
                intent.putExtra("key", event.getKey());
                startActivityForResult(intent, Constants.GENERAL);
            }
            /*
             * User clicked "Sync with TBA" option
             */
            else if(preference.getKey().equalsIgnoreCase("tba_sync")) {
                if(event.getKey() == null || event.getKey().equalsIgnoreCase("")) {
                    Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), "No TBA key found. Set it in this event's settings.", true, 0);
                    return true;
                }

                // Download the entire event
                tbaSyncDialog = ProgressDialog.show(getActivity(), "Syncing event with TheBlueAlliance...", "This may take several seconds...", false);
                tbaSyncDialog.setCancelable(false);
                new ImportEvent(new TBALoadEventsTask.LoadTBAEventsListener() {
                    @Override
                    public void errorOccurred(String errMsg) {
                        Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), "Error occurred while syncing event: " + errMsg, true, 0);
                    }

                    @Override
                    public void eventDownloaded(Event e) {
                        // Start the merge!
                        new SyncTBAEvent(e, event.getID(), new IO(getActivity()), new SyncTBAEvent.SyncTBAEventListener() {
                            @Override
                            public void done() {
                                tbaSyncDialog.dismiss();
                            }
                        }).start();
                    }

                    @Override
                    public void eventListDownloaded(ArrayList<Event> events) {
                        // not used, but required because of shared constructor
                    }
                }, event.getKey()).start();
            }
            /*
             * User clicked on "Duplicate event"
             */
            else if(preference.getKey().equals("duplicate")) {
                new FastDialogBuilder()
                        .setTitle("Keep scouting data?")
                        .setMessage("Would you like to copy scouting data to the duplicate event?")
                        .setPositiveButtonText("Yes")
                        .setNegativeButtonText("No")
                        .setNeutralButtonText("Cancel")
                        .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                            @Override
                            public void accepted() {
                                Intent intent = new Intent();
                                intent.putExtra("eventID", new IO(getActivity()).duplicateEvent(event, true).getID());
                                getActivity().setResult(Constants.NEW_EVENT_CREATED);
                                Toast.makeText(getActivity(), "Duplicate event created", Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void denied() {
                                Intent intent = new Intent();
                                intent.putExtra("eventID", new IO(getActivity()).duplicateEvent(event, false).getID());
                                getActivity().setResult(Constants.NEW_EVENT_CREATED);
                                Toast.makeText(getActivity(), "Duplicate event created", Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void neutral() {

                            }
                        }).build(getActivity());
            }
            /*
             * User clicked on "Backup event"
             */
            else if(preference.getKey().equals("backup")) {
                fileChooserMode = 2;

                if(EasyPermissions.hasPermissions(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Star the task
                    new BackupEventTask(new IO(getActivity()), new BackupEventTask.BackupEventTaskListener() {
                        @Override
                        public void eventBackupComplete(File backup) {
                            backupFile = backup;

                            // We have an internal file now, let's ask the user for where they want to put it in a location they can see it
                            try {
                                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                    intent.setType("application/*");
                                    intent.putExtra(Intent.EXTRA_TITLE, backupFile.getName());
                                    startActivityForResult(intent, Constants.FILE_CHOOSER);
                                } else Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), "Your phone doesn't support backup exporting", true, 0);
                            } catch(Exception e) {
                                Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), "No file manager found", true, 0);
                            }
                        }
                    }).execute();
                } else {
                    Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), "Storage permission is disabled. Please enable it.", true, rui.getPrimaryColor());
                }
            }
            /*
             * User clicked on "Delete teams"
             */
            else if(preference.getKey().equals("delete_teams")) {
                new FastDialogBuilder()
                        .setTitle("Delete all teams?")
                        .setMessage("Are you sure you want to delete ALL team profiles within this event? Cannot be undone!")
                        .setPositiveButtonText("Delete")
                        .setNegativeButtonText("Cancel")
                        .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                            @Override
                            public void accepted() {
                                new IO(getActivity()).deleteAllTeams(event.getID());
                                preference.setSummary("Delete 0 teams");
                                Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), "Teams successfully deleted", false, rui.getPrimaryColor());
                            }

                            @Override
                            public void denied() {
                            }

                            @Override
                            public void neutral() {
                            }
                        }).build(getActivity());
            }
            /*
             * User clicked on "Delete event"
             */
            else if(preference.getKey().equals("delete_event")) {
                new FastDialogBuilder()
                        .setTitle("Warning! Delete event?")
                        .setMessage("Are you sure you want to delete the event? This CANNOT be undone!!")
                        .setPositiveButtonText("Delete")
                        .setNegativeButtonText("Cancel")
                        .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                            @Override
                            public void accepted() {
                                IO io = new IO(getActivity());
                                io.clearCheckouts();
                                io.deleteEvent(event.getID());

                                if(event.isCloudEnabled()) {
                                    new IO(getActivity()).clearCheckouts();
                                }

                                startActivity(new Intent(getActivity(), TeamsView.class));
                                Toast.makeText(getActivity(), "Event successfully deleted", Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void denied() {
                            }

                            @Override
                            public void neutral() {
                            }
                        }).build(getActivity());
            }
            /*
             * User clicked on "Export to .csv"
             */
            else if(preference.getKey().equals("export_csv")) {
              /*  ProgressDialog d = ProgressDialog.show(getActivity(), "Freeze!", "Roblu is generating a spreadsheet file...", true);
                d.setCancelable(false);
                d.show();

                new ExportCSVTask(getActivity(), this, d, event).execute();*/
                Intent intent = new Intent(getActivity(), CSVActivity.class);
                intent.putExtra("event", event);
                startActivity(intent);

            }
            return true;
        }

        /**
         * This method is called whenever a preference is changed and has a value that needs to be saved
         *
         * @param preference the preference whose value was changed
         * @param o          the new value that the user just set
         * @return true if the event was consumed
         */
        @Override
        public boolean onPreferenceChange(final Preference preference, Object o) {
            /*
             * User selected the cloud sync button
             */
            if(preference.getKey().equals("sync")) {
                if((boolean) o) {
                    /*
                     * Check if an error message should be shown
                     */
                    RSettings settings = new IO(getActivity()).loadSettings();
                    Request r = new Request(settings.getServerIP());
                    CloudTeamRequest ctr = new CloudTeamRequest(r, settings.getCode());
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
                    StrictMode.setThreadPolicy(policy);

                    if(settings.getCode() == null || settings.getCode().equals("")) {
                        Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), "You must enter a team code in Roblu settings before syncing.", true, 0);
                        return false;
                    }

                    if(ctr.isActive()) {
                        new FastDialogBuilder()
                                .setTitle("Warning")
                                .setMessage("It looks like you already have some scouting data on the server. If you overwrite, you may lose some scouting data. Are you sure you want to overwrite?")
                                .setPositiveButtonText("Overwrite")
                                .setNegativeButtonText("Cancel")
                                .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                                    @Override
                                    public void accepted() {
                                        uploadEvent();
                                    }

                                    @Override
                                    public void denied() {
                                        ((CheckBoxPreference) preference).setChecked(false);
                                    }

                                    @Override
                                    public void neutral() {
                                    }
                                }).build(getActivity());

                    } else uploadEvent();
                } else {
                    event.setCloudEnabled(false);
                    new IO(getActivity()).saveEvent(event);
                    new IO(getActivity()).clearCheckouts();
                }
            }
            /*
             * User clicked the "Sync with Bluetooth" option
             */
            else if(preference.getKey().equals("bt_sync")) {
                REvent[] events = new IO(getActivity()).loadEvents();
                for(REvent ev : events) {
                    ev.setBluetoothEnabled(((Boolean) o && ev.getID() == event.getID()));
                    new IO(getActivity()).saveEvent(ev);
                }
                event.setBluetoothEnabled((Boolean) o);
                ((CheckBoxPreference) preference).setChecked(((Boolean) o));
            }
            return true;
        }

        /**
         * Uploads the active event to the server
         */
        private void uploadEvent() {
            final ProgressDialog pd = ProgressDialog.show(getActivity(), "Fasten your seatbelt!", "Launching packets into Roblu Cloud orbit...", false);
            pd.setCancelable(false);
            pd.show();

            InitPacker ip = new InitPacker(pd, cloud, new IO(getActivity()), event.getID());
            ip.setListener(new InitPacker.StatusListener() {
                @Override
                public void statusUpdate(String message) {
                    pd.dismiss();
                    Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), message, false, rui.getPrimaryColor());
                }
            });
            ip.execute();
        }

        /**
         * Receives various data from any activities started from this class
         *
         * @param requestCode the request code the child activities were started with
         * @param resultCode  the result code form child activites
         * @param data        any data included with the result
         */
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            /*
             * Received after the user selected a backup file location and data needs to be
             * copied to it
             */
            if(requestCode == Constants.FILE_CHOOSER && fileChooserMode == 2) {
                try {
                    /*
                     * This will copy the internal backup file to the external location the user selected
                     * (internal is app data, external is a location the user can see, but still on internal storage for the device)
                     */
                    OutputStream os = getActivity().getContentResolver().openOutputStream(data.getData());
                    if(os != null) {
                        Files.copy(backupFile, os);
                        os.flush();
                        os.close();
                        Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), "Successfully created backup file", false, rui.getPrimaryColor());
                    } else Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), "Error occurred while creating backup", true, 0);
                } catch(Exception e) {
                    Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), "Error occurred while creating backup", true, 0);
                }

                fileChooserMode = 0;
            }
            else if(requestCode == Constants.FILE_CHOOSER && fileChooserMode == 1) {
                // this means the user didn't select a file, no point in returning an error message
                if(data == null) return;

                new ManualScheduleImporter(getActivity(), data.getData(), event.getID(), new ManualScheduleImporter.ManualScheduleImporterListener() {
                    @Override
                    public void error(final String message) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "An error occurred: "+message, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void success() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "Successfully imported manual match schedule.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }).start();

                fileChooserMode = 0;
            }
            /*
             * Called when event info was edited
             */
            else if(resultCode == Constants.EVENT_INFO_EDITED) {
                event.setName(data.getStringExtra("name"));
                event.setKey(data.getStringExtra("key"));
                new IO(getActivity()).saveEvent(event);
                if(((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                    ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(event.getName());
                }
            }
            /*
             * Called when the user made changes to the form
             */
            else if(resultCode == Constants.FORM_CONFIRMED) {
                Bundle bundle = data.getExtras();
                RForm form = (RForm) bundle.getSerializable("form");
                form.setUploadRequired(true);
                new IO(getActivity()).saveForm(event.getID(), form);
            }
        }

        /**
         * BackupEvent backups up an REvent object into a internal file
         */
        public static class BackupEventTask extends AsyncTask<Void, Void, File> {

            private WeakReference<IO> ioWeakReference;

            public interface BackupEventTaskListener {
                void eventBackupComplete(File backupFile);
            }

            /**
             * Notified with a backupFile object when the backup is complete
             */
            private BackupEventTaskListener listener;

            public BackupEventTask(IO io, BackupEventTaskListener listener) {
                this.listener = listener;
                this.ioWeakReference = new WeakReference<>(io);
            }

            protected File doInBackground(Void... params) {
                RTeam[] teams = ioWeakReference.get().loadTeams(event.getID());
                IO io = ioWeakReference.get();

                /*
                 * Package images into the backup file
                 */
                for(RTeam team : teams) {
                    for(RTab tab : team.getTabs()) {
                        for(int i = 0; tab.getMetrics() != null && i < tab.getMetrics().size(); i++) {
                            if(tab.getMetrics().get(i) instanceof RFieldData) {
                                ((RFieldData) tab.getMetrics().get(i)).setData(null);
                            }

                            if(!(tab.getMetrics().get(i) instanceof RGallery)) continue;

                            // Make sure the array is not null.
                            ((RGallery)tab.getMetrics().get(i)).setImages(new ArrayList<byte[]>());

                            for(int j = 0; ((RGallery)tab.getMetrics().get(i)).getPictureIDs() != null && j < ((RGallery)tab.getMetrics().get(i)).getPictureIDs().size(); j++) {
                                ((RGallery)tab.getMetrics().get(i)).getImages().add(io.loadPicture(event.getID(), ((RGallery)tab.getMetrics().get(i)).getPictureIDs().get(j)));
                            }
                        }
                    }
                }

                RForm form = io.loadForm(event.getID());
                return io.saveBackup(new RBackup(event, teams, form), "event.roblubackup");
            }

            protected void onPostExecute(File file) {
                listener.eventBackupComplete(file);
            }
        }

        private static String getPath(Context context, Uri uri) {
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = { "_data" };
                Cursor cursor;

                try {
                    cursor = context.getContentResolver().query(uri, projection, null, null, null);
                    int column_index = cursor.getColumnIndexOrThrow("_data");
                    if (cursor.moveToFirst()) {
                        return cursor.getString(column_index);
                    }
                } catch (Exception e) {
                    // Eat it
                }
            }
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }

            return null;
        }
    }

}
