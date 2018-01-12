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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cpjd.roblu.R;
import com.cpjd.roblu.csv.ExportCSV;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RBackup;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.ui.dialogs.FastDialogBuilder;
import com.cpjd.roblu.ui.forms.EditForm;
import com.cpjd.roblu.ui.settings.MyCheckPreference;
import com.cpjd.roblu.ui.teams.TeamsView;
import com.cpjd.roblu.utils.Constants;
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
 * @version 2
 * @since 3.0.0
 * @author Will Davies
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
        if (id == android.R.id.home) {
            Intent intent = new Intent();
            intent.putExtra("event", event);
            setResult(Constants.DATA_SETTINGS_CHANGED, intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("event", event);
        setResult(Constants.DATA_SETTINGS_CHANGED, intent);
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
            findPreference("cloud").setOnPreferenceClickListener(this);

            /*
             * Obtain explicit preference references where an attribute of the preference
             * needs to be programmatically set
             */
            Preference teams = findPreference("delete_teams");
            Preference deleteEvent = findPreference("delete_event");
            MyCheckPreference cloud = (MyCheckPreference) findPreference("sync");

            /*
             * Set the info to the UI that needs to be
             */

            teams.setSummary("Delete "+new IO(getActivity()).getNumberTeams(event.getID())+" teams");
            deleteEvent.setSummary("Delete ["+event.getName()+"] event");

            cloud.setChecked(event.isCloudEnabled());
        }

        /**
         * The user tapped on one of the available preferences
         * @param preference the preference that was tapped
         * @return true if the event was consumed
         */
        @Override
        public boolean onPreferenceClick(final Preference preference) {
            /*
             * User clicked on the "Edit form" preference.
             * Keep in mind, we'll need to listen to the EditForm activity's return
             */
            if(preference.getKey().equals("edit_form")) {
                Intent intent = new Intent(getActivity(), EditForm.class);
                intent.putExtra("form", new IO(getActivity()).loadForm(event.getID()));
                intent.putExtra("editing", true); // editing will be true
                startActivityForResult(intent, Constants.GENERAL);
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
             * User clicked on "Duplicate event"
             */
            else if(preference.getKey().equals("duplicate")) {
                new FastDialogBuilder()
                        .setTitle("Keep scouting data?")
                        .setMessage("Would you like to copy scouting data to the duplicate event?")
                        .setPositiveButtonText("Yes")
                        .setNegativeButtonText("No")
                        .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                            @Override
                            public void accepted() {
                                new IO(getActivity()).duplicateEvent(event, true);
                                Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), "Duplicate event created", false, rui.getPrimaryColor());
                            }

                            @Override
                            public void denied() {
                                new IO(getActivity()).duplicateEvent(event, false);
                                Utils.showSnackbar(getActivity().findViewById(R.id.event_settings), getActivity(), "Duplicate event created", false, rui.getPrimaryColor());
                            }

                            @Override
                            public void neutral() {}
                        }).build(getActivity());
            }
            /*
             * User clicked on "Backup event"
             */
            else if(preference.getKey().equals("backup")){
                if (EasyPermissions.hasPermissions(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Star the task
                    new BackupEventTask(new IO(getActivity()), new BackupEventTask.BackupEventTaskListener() {
                        @Override
                        public void eventBackupComplete(File backup) {
                            backupFile = backup;

                            // We have an internal file now, let's ask the user for where they want to put it in a location they can see it
                            try {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
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
            else if (preference.getKey().equals("delete_teams")) {
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
                            public void denied() {}

                            @Override
                            public void neutral() {}
                        }).build(getActivity());
            }
            /*
             * User clicked on "Delete event"
             */
            else if (preference.getKey().equals("delete_event")) {
                new FastDialogBuilder()
                        .setTitle("Warning! Delete event?")
                        .setMessage("Are you sure you want to delete the event? This CANNOT be undone!!")
                        .setPositiveButtonText("Delete")
                        .setNegativeButtonText("Cancel")
                        .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                            @Override
                            public void accepted() {
                                IO io = new IO(getActivity());
                                //TODO make sure syncing is force quit here
                                io.clearCheckouts();
                                io.deleteEvent(event.getID());

                                startActivity(new Intent(getActivity(), TeamsView.class));
                                Toast.makeText(getActivity(), "Event successfully deleted", Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void denied() {}

                            @Override
                            public void neutral() {}
                        }).build(getActivity());
            }
            /*
             * User clicked on "Export to .csv"
             */
            else if (preference.getKey().equals("export_csv")) {
                new FastDialogBuilder()
                        .setTitle("Generate verbose data?")
                        .setMessage("Would you like Roblu to generate verbose data? Verbose data contains all data, even unmodified data, and is useful if you plan on writing formulas to analyze data in Excel.")
                        .setPositiveButtonText("Yes")
                        .setNegativeButtonText("No")
                        .setNeutralButtonText("Cancel")
                        .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                            @Override
                            public void accepted() {
                                new ExportCSV((RelativeLayout)getActivity().findViewById(R.id.event_settings), event, getActivity(), true).execute();
                            }

                            @Override
                            public void denied() {
                                new ExportCSV((RelativeLayout)getActivity().findViewById(R.id.event_settings), event, getActivity(), false).execute();
                            }

                            @Override
                            public void neutral() {
                            }
                        }).build(getActivity());
            }
            return true;
        }

        /**
         * This method is called whenever a preference is changed and has a value that needs to be saved
         * @param preference the preference whose value was changed
         * @param o the new value that the user just set
         * @return true if the event was consumed
         */
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            /*
             * User selected the cloud sync button
             */
            if(preference.getKey().equals("sync")) {
                //Todo specify how this will work
            }
            return true;
        }

        /**
         * Receives various data from any activities started from this class
         * @param requestCode the request code the child activities were started with
         * @param resultCode the result code form child activites
         * @param data any data included with the result
         */
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            /*
             * Received after the user selected a backup file location and data needs to be
             * copied to it
             */
            if(requestCode == Constants.FILE_CHOOSER) {
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
            }
            /*
             * Called when event info was edited
             */
            else if(resultCode == Constants.EVENT_INFO_EDITED) {
                event.setName(data.getStringExtra("name"));
                event.setKey(data.getStringExtra("key"));
                new IO(getActivity()).saveEvent(event);
                getActivity().getActionBar().setTitle(event.getName());
            }
            /*
             * Called when the user made changes to the form
             */
            else if(resultCode == Constants.FORM_CONFIMRED) {
                Bundle bundle = data.getExtras();
                RForm form = new RForm((ArrayList<RMetric>) bundle.getSerializable("pit"), (ArrayList<RMetric>) bundle.getSerializable("match"));
                form.setUploadRequired(true);
                new IO(getActivity()).saveForm(event.getID(), form);
            }
        }

        /**
         * BackupEvent backups up an REvent object into a internal file
         */
        public static class BackupEventTask extends AsyncTask<Void, Void, RBackup> {

            private WeakReference<IO> ioWeakReference;

            interface BackupEventTaskListener {
                void eventBackupComplete(File backupFile);
            }

            /**
             * Notified with a backupFile object when the backup is complete
             */
            private BackupEventTaskListener listener;

            BackupEventTask(IO io, BackupEventTaskListener listener) {
                this.listener = listener;
                this.ioWeakReference = new WeakReference<>(io);
            }

            protected RBackup doInBackground(Void... params) {
                RTeam[] teams = ioWeakReference.get().loadTeams(event.getID());
                RForm form = ioWeakReference.get().loadForm(event.getID());
                return new RBackup(event, teams, form);
            }

            protected void onPostExecute(RBackup result) {
                listener.eventBackupComplete(ioWeakReference.get().saveBackup(result));
            }
        }
    }
}
