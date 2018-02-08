/*
 * ******************************************************
 *  * Copyright (C) 2016 Will Davies wdavies973@gmail.com
 *  *
 *  * This file is part of Roblu
 *  *
 *  * Roblu cannot be distributed for a price or to people outside of your local robotics team.
 *  ******************************************************
 */

package com.cpjd.roblu.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cpjd.http.Request;
import com.cpjd.models.CloudTeam;
import com.cpjd.requests.CloudTeamRequest;
import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RCloudSettings;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.sync.bluetooth.BTDeviceSetup;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.ui.dialogs.FastDialogBuilder;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import me.aflak.bluetooth.Bluetooth;

/**
 *
 * AdvSettings is short for "Advanced Settings", because the last version of settings was absolute garbage.
 * This one is a bit better. This manages application level settings, not specific to any events
 *
 * @version 2
 * @since 2.0.0
 * @author Will Davies
 */
public class AdvSettings extends AppCompatActivity {
    /**
     * This is a reference to settings object, it's used for loading and updating settings
     */
    private static RSettings settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set layout, ui attributes
        setContentView(R.layout.activity_settings);
        setTitle("Settings");
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Load settings
        settings = new IO(getApplicationContext()).loadSettings();

        // Replace the view with the preference fragment, the preference fragment manages all the setting changes and whatnot
        getFragmentManager().beginTransaction()
                .replace(R.id.blankFragment, new SettingsFragment())
                .commit();


        // UIHandler updates our activity to match what's set in RUI
        new UIHandler(this, (Toolbar)findViewById(R.id.toolbar)).update();
    }

    /**
     * This is called when the user exits the UI customizer, we basically just want to reload the UI to match any changes the user may have made to the UI
     * @param requestCode the request code of the child activity
     * @param resultCode the result code of the child activity
     * @param data any data returned by the child activity
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        new UIHandler(this, (Toolbar)findViewById(R.id.toolbar)).update();
    }

    /**
     *  The settings fragment manages the loading & updating of settings
     */
    @SuppressWarnings("WeakerAccess")
    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

        /**
         * The Bluetooth connect object, must be managed by an activity. This will allow us to listen for
         * incoming Bluetooth connections
         */
        private Bluetooth bluetooth;

        // text constants that will be accessible in the about libraries view
        private final String PRIVACY = "Roblu Privacy & Terms of Use\n" +
                "\nData that Roblu stores and transfers:\n-Google email\n-Google display name\n-FRC Name and Number\n-Any and all form data, including scouters' data, local data, and more." +
                "\n\nRoblu does NOT manage your Google password, payments, or any other data on your device that is not created by Roblu. Data is transferred over" +
                " an internet connection if syncing is enabled, and all connections are encrypted and secured using your team code. Scouting data is not inherently extremely sensitive information, so appropriate " +
                "cautions have been made to the level of security required. At any time, Roblu many crash or malfunction and all your data could be deleted. By using Roblu, you agree to all responsibility if your " +
                "data is lost, or data is stolen. If you do not agree, do not use Roblu.";

        private final String CONTRIBUTIONS = "Roblu Master Android App - Will Davies\n\nRoblu Scouter Android App - Will Davies\n\nRoblu Scouter IOS App - Alex Harker\n\nRoblu Cloud Backend - Andy Pethan & Isaac Faulkner\n\nRoblu Cloud API - Will Davies";

        private final String CHANGELOG = "3.5.9\n-Added my matches\n-Improvements to searching and filtering\n-Ads removed, UI customizer available for everyone\n-Reworked cloud controls\n-Event import now searchable\n-Bug fixes" +
                "\n\n3.5.8\n-Bug fixes\n\n3.5.5 - 3.5.7\n-Changed app name to Roblu Master\n-Bug fixes\n\n3.5.4\n-Added custom sorting\n-Mark matches as won, delete, open on TBA\n-Bug fixes\n\n3.5.3\n-Bug fixes\n\n3.5.2\n-Added gallery elements\n-Bug fixes" +
                "\n\n3.5.0 - 3.5.1\n-Bug fixes\n\n3.0.0 - 3.4.9\n-Completed redesigned system\n-Redesigned file system\n-New form editor\n-New form elements\n-TBA-API improvements\n-Less restrictions on naming, editing, etc\n-New interface\n\n" +
                "2.0.0-2.9.9\nRoblu Version 2, we don't talk about that anymore\n\n1.0.0-1.9.9\nRoblu Version 1 is where humans go to die";


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Make sure that this fragment is being loaded in the context of AdvSettings
            if(!(getActivity() instanceof AdvSettings)) {
                getActivity().finish();
                return;
            }

            // setup Bluetooth
            bluetooth = new Bluetooth(getActivity());

            // Load the preferences specified in xml into the system, we only have to modify a couple of things manually
            addPreferencesFromResource(R.xml.preferences);

            // We'll have to set the EditText's value to the team number we have stored locally
            EditTextPreference teamNumber = (EditTextPreference) findPreference("team_number");
            teamNumber.setDefaultValue(settings.getTeamNumber());
            teamNumber.setText(String.valueOf(settings.getTeamNumber()));
            teamNumber.setOnPreferenceChangeListener(this);

            // We have to set all the preference click listener so we can process a preference click event and update its value
            findPreference("about").setOnPreferenceClickListener(this);
            findPreference("customizer").setOnPreferenceClickListener(this);
            findPreference("server_ip").setOnPreferenceChangeListener(this);
            findPreference("display_code").setOnPreferenceClickListener(this);
            findPreference("cloud_support").setOnPreferenceClickListener(this);
            findPreference("reddit").setOnPreferenceClickListener(this);
            findPreference("purge").setOnPreferenceClickListener(this);
            findPreference("bt_devices").setOnPreferenceClickListener(this);

            toggleJoinTeam(!(settings.getCode() != null && !settings.getCode().equals("")));

        }

        // This updates the UI depending on whether the user has entered a team code or not
        private void toggleJoinTeam(boolean b) {
            Preference joinTeam = findPreference("display_code");
            if(!b) {
                joinTeam.setTitle("Display team code");
                joinTeam.setSummary("Show the team code so teammates can join the team");
            } else {
                joinTeam.setTitle("Join team");
                joinTeam.setSummary("Enter your team code to join a team");
            }
        }

        // Called when the user taps a preference
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if(preference.getKey().equals("reddit")) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/Roblu"));
                startActivity(browserIntent);
                return true;
            }
            else if(preference.getKey().equals("bt_devices")) { // user tapped on Bluetooth device setup button

                final ProgressDialog dialog = ProgressDialog.show(getActivity(), "Listening for devices", "Your device is visible to nearby devices. Roblu is listening for Bluetooth devices...", false);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                new BTDeviceSetup(bluetooth, new BTDeviceSetup.BTDeviceSetupListener() {
                    @Override
                    public void success() {
                        dialog.dismiss();
                    }

                    @Override
                    public void error(String message) {
                        Utils.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), message, true, 0);
                        dialog.dismiss();
                    }
                });
                return true;
            }
            else if(preference.getKey().equals("display_code")) { // user tapped display team code
                // if already signed in, display the team code
                if(settings.getCode() != null && !settings.getCode().equals("")) showTeamCode();
                else joinTeam(); // if not, prompt the user for a team code
                return true;
            }
            else if(preference.getKey().equals("purge")) { // user tapped "purge" event
                new FastDialogBuilder()
                        .setTitle("Are you sure?!?")
                        .setMessage("This will delete ALL scouting data on ALL devices (except locally). You cannot undo this.")
                        .setPositiveButtonText("Purge")
                        .setNegativeButtonText("Cancel")
                        .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                            @Override
                            public void accepted() {
                                // Remove all the other synced events
                                IO io = new IO(getActivity());
                                REvent[] events = io.loadEvents();
                                for(int i = 0; events != null && i < events.length; i++) {
                                    events[i].setCloudEnabled(false);
                                    io.saveEvent(events[i]);
                                }

                                // Flag purge!
                                RCloudSettings cloudSettings = io.loadCloudSettings();
                                cloudSettings.setPurgeRequested(true);
                                io.saveCloudSettings(cloudSettings);
                            }

                            @Override
                            public void denied() {

                            }

                            @Override
                            public void neutral() {

                            }
                        }).build(getActivity());
            }
            else if(preference.getKey().equals("cloud_support")) { // open roblu.net in a browser on the user's device
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://roblu.net"));
                startActivity(browserIntent);
                return true;
            }
            else if(preference.getKey().equals("about")) { // launch the about libraries view, this is managed via an external library
                new LibsBuilder().withFields(R.string.class.getFields()).withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR).withAboutIconShown(true).withAboutVersionShown(true).withAboutDescription("Copyright 2017. A scouting app for robotics competitions focused on customization, simplicity, and functionality. Roblu is a" +
                        " project designed to streamline your scouting experience. Thank you to Andy Pethan and Isaac Faulkner for all the help. App written by Will Davies.")
                        .withActivityTitle("About Roblu").withLicenseShown(true).withAboutSpecial1("Privacy").withAboutSpecial1Description(PRIVACY).withAboutSpecial2("Contributors").withAboutSpecial2Description(CONTRIBUTIONS).withAboutSpecial3("Changelog")
                        .withAboutSpecial3Description(CHANGELOG).
                        start(getActivity());
                return true;
            }
            else if(preference.getKey().equals("customizer")) { // launch the UI customizer, make sure to listen for any changes the user makes since this activity isn't re-created on back button pressed
                getActivity().startActivityForResult(new Intent(getActivity(), UICustomizer.class), Constants.GENERAL);
                return true;
            }
            return false;
        }

        // Manages preferences that have a value and can be changed
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            if(preference.getKey().equals("team_number")) { // user changed their team number
                // We have to verify that the number they typed in is not above the Integer.MAX_VALUE or we'll get an error, for our purposes, we'll just cap it at a million possible numbers
                int num;
                try {
                    num = Integer.parseInt(o.toString());
                } catch(Exception e) {
                    num = 999999;
                }
                if(num > 999999) {
                    num = 999999;
                    preference.setDefaultValue(num);
                }
                settings.setTeamNumber(num);
            }
            /*
             * User tapped "server IP"
             */
            else if(preference.getKey().equalsIgnoreCase("server_ip")) {
                if(o.toString() == null || o.toString().equals("") || o.toString().replaceAll(" ", "").equals("")) {
                    settings.setServerIPToDefault();
                } else settings.setServerIP(o.toString());
            }
            new IO(getActivity()).saveSettings(settings); // save the settings to the file system
            return true;
        }

        /*
         * If this is the user's first time entering their code, open a dialog with a text field
         */
        private void joinTeam() {
            // check for an internet connection
            if(!Utils.hasInternetConnection(getActivity())) {
                Utils.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "You are not connected to the internet", true, 0);
                return;
            }
            /*
             * We need to make sure that this thread has access to the internet
             */
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
            StrictMode.setThreadPolicy(policy);

            RUI rui = settings.getRui();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LinearLayout layout = new LinearLayout(getActivity());
            layout.setOrientation(LinearLayout.VERTICAL);

            // this is the team code input edit text
            final AppCompatEditText input = new AppCompatEditText(getActivity());
            Utils.setInputTextLayoutColor(rui.getAccent(),null, input);
            input.setHighlightColor(rui.getAccent());
            input.setHintTextColor(rui.getText());
            input.setTextColor(rui.getText());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            InputFilter[] FilterArray = new InputFilter[1];
            FilterArray[0] = new InputFilter.LengthFilter(30);
            input.setFilters(FilterArray);
            layout.addView(input);

            builder.setView(layout);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Request r = new Request(settings.getServerIP());
                    CloudTeamRequest tr = new CloudTeamRequest(r, input.getText().toString());

                    if(!r.ping()) {
                        Utils.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "It looks like the server is down, try again later.", true, 0);
                        return;
                    }

                    if(tr.getTeam(-1) != null) {
                        settings.setCode(input.getText().toString());
                        new IO(getActivity()).saveSettings(settings);
                        toggleJoinTeam(false);
                        Utils.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "Successfully joined team.", false, new IO(getActivity()).loadSettings().getRui().getPrimaryColor());
                    } else {
                        Snackbar s = Snackbar.make(getActivity().findViewById(R.id.advsettings), "Team code is invalid.", Snackbar.LENGTH_LONG);
                        s.getView().setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
                        s.setAction("Purchase Roblu Cloud", new PurchaseListener());
                        s.show();
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { dialog.cancel(); }
            });
            TextView view = new TextView(getActivity());
            view.setTextSize(Utils.DPToPX(getActivity(), 5));
            view.setPadding(Utils.DPToPX(getActivity(), 18), Utils.DPToPX(getActivity(), 18), Utils.DPToPX(getActivity(), 18), Utils.DPToPX(getActivity(), 18));
            view.setText("Team code: ");
            view.setTextColor(rui.getText());
            AlertDialog dialog = builder.create();
            dialog.setCustomTitle(view);
            if(dialog.getWindow() != null) {
                dialog.getWindow().getAttributes().windowAnimations = rui.getAnimation();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
            }
            dialog.show();
            dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(rui.getAccent());
            dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());
        }

        private void showTeamCode() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle("Your team code is: ");
            builder.setMessage(settings.getCode());

            builder.setPositiveButton("COPY", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Roblu team code", settings.getCode());
                    clipboard.setPrimaryClip(clip);
                    dialog.dismiss();
                    Toast.makeText(getActivity(), "Team code copied to clipboard", Toast.LENGTH_LONG).show();
                }
            });
            builder.setNegativeButton("Regenerate", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    new FastDialogBuilder()
                            .setNeutralButtonText("Cancel")
                            .setPositiveButtonText("Regenerate")
                            .setTitle("Warning")
                            .setMessage("If you regenerate your team code, all of your team members must rejoin this team with the new code.")
                            .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                                @Override
                                public void accepted() {
                                    Request r = new Request(settings.getServerIP());

                                    if(!r.ping()) {
                                        Toast.makeText(getActivity(), "Looks like the server is down. Please try again later.", Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    CloudTeamRequest tr = new CloudTeamRequest(r, settings.getCode());
                                    CloudTeam ct = tr.regenerateCode();
                                    if(ct != null) {
                                        settings.setCode(ct.getCode());
                                        new IO(getActivity()).saveSettings(settings);
                                        showTeamCode();
                                    }
                                }

                                @Override
                                public void denied() {

                                }

                                @Override
                                public void neutral() {

                                }
                            }).build(getActivity());
                }
            });
            builder.setNeutralButton("Leave", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    settings.setCode("");
                    new IO(getActivity()).saveSettings(settings);
                    toggleJoinTeam(true);
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            if(dialog.getWindow() != null) dialog.getWindow().getAttributes().windowAnimations = settings.getRui().getAnimation();
            dialog.show();
        }

        @Override
        public void onStart() {
            super.onStart();
            bluetooth.onStart();
        }

        @Override
        public void onStop() {
            super.onStop();
            bluetooth.onStop();
        }

        // Listens to the custom snack bar, displays a link to Roblu Cloud purchase
        public class PurchaseListener implements View.OnClickListener{

            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://roblu.net/purchase"));
                startActivity(browserIntent);
            }
        }
    }

    // load in the bug report button, and make it match the ui settings
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_actionbar, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }

    // user tapped the android back button on their phone, not the in-app one, we return Constants.SETTINGS_CHANGED because teamsview will need to reload settings
    @Override
    public void onBackPressed() {
        setResult(Constants.SETTINGS_CHANGED);
        finish();
    }

    // manages what to do if the user tapped bug report, or the back button on the toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) { // stop the activity, tell teamsview to reload settings
            setResult(Constants.SETTINGS_CHANGED);
            finish();
            return true;
        }
        if (id == R.id.settings_bug) { // launch an email intent to the developer, easy way to do a bug report
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"wdavies973@gmail.com"});
            i.putExtra(Intent.EXTRA_SUBJECT, "Roblu Bug Report");
            i.putExtra(Intent.EXTRA_TEXT   , "The information below includes how to reproduce the bug and a thorough description of what went wrong. (Thanks - WD)");
            try {
                startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (ActivityNotFoundException ex) {
                // to avoid a crash, make sure that the user actually has an email client installed (most will)
                Utils.showSnackbar(findViewById(R.id.advsettings), getApplicationContext(), "No email clients installed.", true, 0);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}