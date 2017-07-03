/*
 * ******************************************************
 *  * Copyright (C) 2016 Will Davies wdavies973@gmail.com
 *  *
 *  * This file is part of Roblu
 *  *
 *  * Roblu cannot be distributed for a price or to people outside of your local robotics team.
 *  ******************************************************
 */

package com.cpjd.roblu.events;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cpjd.roblu.R;
import com.cpjd.roblu.csv.ExportCSV;
import com.cpjd.roblu.forms.EditForm;
import com.cpjd.roblu.forms.elements.EGallery;
import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RBackup;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.teams.TeamsView;
import com.cpjd.roblu.ui.MyCheckPreference;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Text;
import com.google.common.io.Files;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;

import pub.devrel.easypermissions.EasyPermissions;

public class EventSettings extends AppCompatActivity {
    private static REvent event;
    private static ActionBar actionbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_settings);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        event = (REvent) getIntent().getSerializableExtra("event");
        setTitle(event.getName());
        actionbar = getSupportActionBar();
        getSupportActionBar().setSubtitle("Event settings");
        getFragmentManager().beginTransaction()
                .replace(R.id.blankFragment, new SettingsFragment())
                .commit();

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

    @SuppressWarnings("WeakerAccess")
    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
        // for exporting backups
        private File backupFile;
        private RUI rui;
        private RelativeLayout layout;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (!(getActivity() instanceof EventSettings)) {
                getActivity().finish();
                return;
            }

            layout = (RelativeLayout) getActivity().findViewById(R.id.event_settings);

            rui = new Loader(getActivity()).loadSettings().getRui();

            addPreferencesFromResource(R.xml.event_preferences);

            Preference editForm = findPreference("edit_form");
            editForm.setOnPreferenceClickListener(this);

            Preference export = findPreference("export_csv");
            export.setOnPreferenceClickListener(this);

            findPreference("backup").setOnPreferenceClickListener(this);
            findPreference("duplicate").setOnPreferenceClickListener(this);
            findPreference("edit_event").setOnPreferenceClickListener(this);

            Preference teams = findPreference("delete_teams");
            teams.setSummary("Delete "+new Loader(getActivity()).getNumberTeams(event.getID())+" teams");
            teams.setOnPreferenceClickListener(this);
            Preference images = findPreference("delete_images");
            images.setSummary("Delete "+new Loader(getActivity()).getNumberImages(event.getID())+" images");
            images.setOnPreferenceClickListener(this);
            Preference deleteEvent = findPreference("delete_event");
            deleteEvent.setSummary("Delete ["+event.getName()+"] event");
            deleteEvent.setOnPreferenceClickListener(this);

            MyCheckPreference cloud = (MyCheckPreference) findPreference("sync");
            cloud.setDefaultValue(event.isCloudEnabled());
            cloud.setChecked(event.isCloudEnabled());
            cloud.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceClick(final Preference preference) {
            if(preference.getKey().equals("edit_form")) {
                Intent intent = new Intent(getActivity(), EditForm.class);
                intent.putExtra("form", new Loader(getActivity()).loadForm(event.getID()));
                intent.putExtra("editing", true);
                startActivityForResult(intent, 1);
                return true;
            }
            else if(preference.getKey().equals("edit_event")) {
                Intent intent = new Intent(getActivity(), CreateEvent.class);
                intent.putExtra("editing", true);
                intent.putExtra("name", event.getName());
                intent.putExtra("start", event.getStartTime());
                intent.putExtra("end", event.getEndTime());
                intent.putExtra("key", event.getKey());
                startActivityForResult(intent, Constants.GENERAL);
            }
            else if(preference.getKey().equals("duplicate")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                builder.setTitle("Keep scouting data?");
                builder.setMessage("Would you like to copy scouting data to the duplicate event?");

                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Loader(getActivity()).duplicate(event, true);
                        Text.showSnackbar(layout, getActivity(), "Duplicate event created", false, rui.getPrimaryColor());
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Loader(getActivity()).duplicate(event, false);
                        Text.showSnackbar(layout, getActivity(), "Duplicate event created", false, rui.getPrimaryColor());
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                if(dialog.getWindow() != null) dialog.getWindow().getAttributes().windowAnimations = rui.getAnimation();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
                dialog.show();
                dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(rui.getAccent());
                dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());
            }
            else {
                if (preference.getKey().equals("backup")) {
                    if (EasyPermissions.hasPermissions(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        new BackupEvent().execute();
                    } else {
                        Text.showSnackbar(layout, getActivity(), "Storage permission is disabled. Please enable it.", true, rui.getPrimaryColor());
                    }

                } else if (preference.getKey().equals("delete_teams")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    builder.setTitle("Delete all teams?");
                    builder.setMessage("Are you sure you want to delete ALL team profiles within this event? Cannot be undone!");

                    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new Loader(getActivity()).deleteAllTeams(event.getID());
                            preference.setSummary("Delete 0 teams");
                            Text.showSnackbar(layout, getActivity(), "Teams successfully deleted", false, rui.getPrimaryColor());
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    if (dialog.getWindow() != null)
                        dialog.getWindow().getAttributes().windowAnimations = rui.getAnimation();
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
                    dialog.show();
                    dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(rui.getAccent());
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());
                } else if (preference.getKey().equals("delete_images")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    builder.setTitle("Delete all images?");
                    builder.setMessage("Are you sure you want to delete ALL image within this event? Cannot be undone!");

                    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            preference.setSummary("Delete 0 images");
                            new Loader(getActivity()).deleteImages(event.getID());
                            Text.showSnackbar(layout, getActivity(), "Images successfully deleted", false, rui.getPrimaryColor());
                            new DeleteImageIDs(event.getID()).execute();
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    if (dialog.getWindow() != null)
                        dialog.getWindow().getAttributes().windowAnimations = rui.getAnimation();
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
                    dialog.show();
                    dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(rui.getAccent());
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());
                } else if (preference.getKey().equals("delete_event")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    builder.setTitle("Warning! Delete event?");
                    builder.setMessage("Are you sure you want to delete the event? This CANNOT be undone!!");

                    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new Loader(getActivity()).deleteEvent(event.getID());
                            startActivity(new Intent(getActivity(), TeamsView.class));
                            Toast.makeText(getActivity(), "Event successfully deleted", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    if (dialog.getWindow() != null)
                        dialog.getWindow().getAttributes().windowAnimations = rui.getAnimation();
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
                    dialog.show();
                    dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(rui.getAccent());
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());
                } else if (preference.getKey().equals("export_csv")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    builder.setTitle("Generate verbose data?");
                    builder.setMessage("Would you like Roblu to generate verbose data? Verbose data contains all data, even unmodified data, and is useful if you plan on writing formulas to analyze data in Excel.");

                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new ExportCSV(layout, event, getActivity(), true).execute();
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new ExportCSV(layout, event, getActivity(), false).execute();
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    if (dialog.getWindow() != null)
                        dialog.getWindow().getAttributes().windowAnimations = rui.getAnimation();
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
                    dialog.show();
                    dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(rui.getAccent());
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());


                }
            }
            return false;
        }



        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            if(preference.getKey().equals("sync")) {
                if(!new Loader(getActivity()).loadSettings().isSignedIn()) {
                    Text.showSnackbar(layout, getActivity(), "You must sign in to Roblu Cloud in the settings before syncing an event", true, 0);
                    return false;
                }

                event.setCloudEnabled((boolean)o);
                new Loader(getActivity()).saveEvent(event);
                if((boolean)o) Text.showSnackbar(layout, getActivity(), "Cloud sync enabled for "+event.getName(), false, rui.getPrimaryColor());
                else Text.showSnackbar(layout, getActivity(), "Cloud sync disabled for "+event.getName(), false, rui.getPrimaryColor());
                return true;
            }
            return false;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if(requestCode == Constants.FILE_CHOOSER) {
                try {
                    OutputStream os = getActivity().getContentResolver().openOutputStream(data.getData());
                    if(os != null) {
                        Files.copy(backupFile, os);
                        os.flush();
                        os.close();
                        Text.showSnackbar(layout, getActivity(), "Successfully created backup file", false, rui.getPrimaryColor());
                        new Loader(getActivity()).cleanBackupDirs();
                    } else Text.showSnackbar(layout, getActivity(), "Error occured while creating backup", true, 0);
                } catch(Exception e) {
                    Text.showSnackbar(layout, getActivity(), "Error occured while creating backup", true, 0);
                }
            }
            if(resultCode == Constants.EVENT_INFO_EDITED) {
                event.setName(data.getStringExtra("name"));
                event.setStartTime(data.getLongExtra("start", System.currentTimeMillis()));
                event.setEndTime(data.getLongExtra("end", System.currentTimeMillis()));
                event.setKey(data.getStringExtra("key"));
                new Loader(getActivity()).saveEvent(event);
                actionbar.setTitle(event.getName());
            }
            if(resultCode == Constants.FORM_CONFIMRED) {
                Bundle bundle = data.getExtras();
                ArrayList<Element> tempPit = (ArrayList<Element>) bundle.getSerializable("tempPit");
                ArrayList<Element> tempMatch = (ArrayList<Element>) bundle.getSerializable("tempMatch");
                RForm form = new RForm(tempPit, tempMatch);
                Loader loader = new Loader(getActivity());
                loader.saveForm(form, event.getID());
            }
        }

        public class DeleteImageIDs extends AsyncTask<Void, Void, Void> {
            private final long eventID;

            public DeleteImageIDs(long eventID) {
                this.eventID = eventID;
            }

            protected Void doInBackground(Void... params) {
                Loader l = new Loader(getActivity());
                RTeam[] teams = l.getTeams(eventID);
                RForm form = l.loadForm(eventID);
                if(teams == null || teams.length == 0) return null;

                for(RTeam team : teams) {
                    team.verify(form);
                    for(int j = 0; j < team.getTabs().size(); j++) {
                        for(int k = 0; k < team.getTabs().get(j).getElements().size(); k++) {
                            if(team.getTabs().get(j).getElements().get(k) instanceof EGallery) {
                                EGallery gallery = (EGallery) team.getTabs().get(j).getElements().get(k);
                                gallery.removeAllIds();
                                team.getTabs().get(j).getElements().set(k, gallery);
                            }
                        }
                    }
                    l.saveTeam(team, eventID);
                }
                return null;
            }

        }

        public class BackupEvent extends AsyncTask<Void, Void, RBackup> {
            protected RBackup doInBackground(Void... params) {
                RTeam[] teams = new Loader(getActivity()).getTeams(event.getID());
                RForm form = new Loader(getActivity()).loadForm(event.getID());
                RBackup backup = new RBackup(event, teams, form);
                backup.setImages(new Loader(getActivity()).getImages(event.getID()));


                return backup;
            }

            protected void onPostExecute(RBackup result) {
                backupFile = new Loader(getActivity()).saveBackup(result);

                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.setType("application/*");
                        intent.putExtra(Intent.EXTRA_TITLE, backupFile.getName());
                        startActivityForResult(intent, Constants.FILE_CHOOSER);
                    } else Text.showSnackbar(layout, getActivity(), "Your phone doesn't support backup exporting", true, 0);
                } catch(Exception e) {
                    Text.showSnackbar(layout, getActivity(), "No file manager found", true, 0);
                }
            }
        }
    }
}
