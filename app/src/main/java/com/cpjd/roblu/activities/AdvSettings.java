/*
 * ******************************************************
 *  * Copyright (C) 2016 Will Davies wdavies973@gmail.com
 *  *
 *  * This file is part of Roblu
 *  *
 *  * Roblu cannot be distributed for a price or to people outside of your local robotics team.
 *  ******************************************************
 */

package com.cpjd.roblu.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.cloud.api.CloudRequest;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.ui.UICustomizer;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.RegenTokenListener;
import com.cpjd.roblu.utils.Text;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import org.json.simple.JSONObject;

/**
 *
 * AdvSettings is short for "Advanced Settings", because the last version of settings was absolute garbage.
 * This one is a bit better. This manages application level settings, not specific to any elements.
 *
 * @since 2.0.0
 * @author Will Davies
 */
public class AdvSettings extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    /*
     * This is a reference to settings object, it's used for loading and updating settings
     */
    private static RSettings settings;
    /*
     * The GoogleApiClient manages Google Sign-In, Google sign-in works by pulling a list of Google accounts locally
     * on the phone and displaying a chooser dialog, once the user selects an account, we gain access to their email
     * and display name, among other things, but no password.
     *
     */
    private static GoogleApiClient apiClient;
    /*
     * This is a reference to the UI object, it manages all the fancy UI colors
     */
    private RUI rui;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set layout, ui attributes
        setContentView(R.layout.activity_settings);
        setTitle("Settings");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Load settings
        settings = new Loader(getApplicationContext()).loadSettings();
        rui = new Loader(getApplicationContext()).loadSettings().getRui();

        // Replace the view with the preference fragment, the preference fragment manages all the setting changes and whatnot
        getFragmentManager().beginTransaction()
                .replace(R.id.blankFragment, new SettingsFragment())
                .commit();


        // UIHandler updates our activity to match what's set in RUI
        new UIHandler(this, (Toolbar)findViewById(R.id.toolbar)).update();

        // Here we set up the apiClient, we are just requesting sign in access, we don't need any other API access, we'll also request the email
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        apiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, this).addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();
    }

    // Callback occurs when Google services can't be connected to, usually the account is stored locally and the chance of this method getting called is relatively low
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Text.showSnackbar(findViewById(R.id.advsettings), getApplicationContext(), "Failed to connect to Google services", true, 0);
    }

    // This is called when the user exits the UI customizer, we basically just want to reload the UI to match any changes the user may have made to the UI
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        new UIHandler(this, (Toolbar)findViewById(R.id.toolbar)).update();
    }

    // The settings fragment manages the loading & updating of settings
    @SuppressWarnings("WeakerAccess")
    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener, RegenTokenListener {
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

        private RUI rui;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Make sure that this fragment is being loaded in the context of AdvSettings
            if (!(getActivity() instanceof AdvSettings)) {
                getActivity().finish();
                return;
            }

            rui = new Loader(getActivity()).loadSettings().getRui();

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
            findPreference("sync_service").setOnPreferenceClickListener(this);
            findPreference("display_code").setOnPreferenceClickListener(this);
            findPreference("cloud_support").setOnPreferenceClickListener(this);

            toggleJoinTeam(!(settings.getTeamCode() != null && !settings.getTeamCode().equals("")));

            // We want to update the UI to match if we're signed in or not (eg "sign-in" or "sign-out")
            toggleCloudControls(settings.isSignedIn());
            updateUI(settings.isSignedIn());
        }

        // Toggles cloud controls, we don't want to let the user access cloud controls without being signed in
        private void toggleCloudControls(boolean b) {
            findPreference("display_code").setEnabled(b);
        }

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

        private void handleSignOut() {
            Auth.GoogleSignInApi.signOut(apiClient).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    toggleCloudControls(!status.isSuccess());
                    if (status.isSuccess()) {
                        settings.setAuth("");
                        settings.setTeamCode("");
                        settings.setClearActiveRequested(true);
                        Loader l = new Loader(getActivity());
                        REvent[] events = l.getEvents();
                        for(int i = 0; events != null && i < events.length; i++) {
                            events[i].setCloudEnabled(false);
                            l.saveEvent(events[i]);
                        }
                        l.saveSettings(settings);
                        l.clearCheckouts();
                        Text.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "Signed out successfully", false, new Loader(getActivity()).loadSettings().getRui().getPrimaryColor());
                    } else
                        Text.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "Sign out failed", true, 0);
                }
            });
            updateUI(false);
        }

        // Handles preference clicks
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if(preference.getKey().equals("display_code")) { // user tapped display team code
                if(settings.getTeamCode() != null && !settings.getTeamCode().equals("")) Text.showTeamCode(getActivity(), settings.getTeamCode(), this);
                else joinTeam();
                return true;
            }
            else if(preference.getKey().equals("cloud_support")) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://roblu.weebly.com/support.html"));
                startActivity(browserIntent);
                return true;
            }
            else if(preference.getKey().equals("sync_service")) { // user tapped sign-in button, we have to decide whether to request sign-in, or sign out of Google
                if(new Loader(getActivity()).loadSettings().isSignedIn()) { // If we're already signed in, let's make a request to sign out
                    // attempt to leave team on the server
                    try {
                        if(settings.getTeamCode() == null || settings.getTeamCode().equalsIgnoreCase("")) {
                            handleSignOut();
                        } else {
                            if(!Text.hasInternetConnection(getActivity())) {
                                Text.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "You are not connected to the internet.", true, 0);
                                return false;
                            }
                            handleSignOut();
                        }
                    } catch(Exception e) {
                        Text.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "Error occurred while contacting Roblu Server. Please try again later.", true, 0);
                    }
                } else { // we're signing into Google, let's display the account picker dialog
                    Intent signIn = Auth.GoogleSignInApi.getSignInIntent(apiClient);
                    startActivityForResult(signIn, Constants.CLOUD_SIGN_IN);
                }
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
            else if(preference.getKey().equals("customizer")) { // launch the UI customizer, make sure to listen for any changes the user makes
                getActivity().startActivityForResult(new Intent(getActivity(), UICustomizer.class), Constants.GENERAL);
                return true;
            }
            return false;
        }

        // This is called when the user taps an account on the account picker dialog, we are managing a sign-in event here
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == Constants.CLOUD_SIGN_IN) {
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(result);
            }
        }

        // Manages the result of a user tapping an account to sign in
        private void handleSignInResult(GoogleSignInResult result) {
            if (result.isSuccess()) { // 99% of the time it will be a success
                toggleCloudControls(true);
                // Signed in successfully, show authenticated UI.
                GoogleSignInAccount acct = result.getSignInAccount();
                if(acct != null) { // update our local settings with the email and display name, don't touch anything else! we don't want to store unnecessary personal information
                    // check for an internet connection
                    if(!Text.hasInternetConnection(getActivity())) {
                        Text.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "You are not connected to the internet.", true, 0);
                        return;
                    }
                    // Attempt to contact the Roblu Server
                    try {
                        JSONObject response = (JSONObject) new CloudRequest(Text.getDeviceID(getActivity())).signIn(acct.getDisplayName(), acct.getEmail());
                        JSONObject response2 = (JSONObject) response.get("data");
                        settings.setAuth(response2.get("auth").toString());
                        new Loader(getActivity()).saveSettings(settings);
                        Text.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "Successfully signed in to Roblu Cloud", false, rui.getPrimaryColor());
                        updateUI(true);
                    } catch(Exception e) {
                        System.out.println(e.getMessage());
                        Text.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "Error occurred while contacting Roblu Server. Please try again later.", true, 0);
                    }

                } else return;
            } else {
                // Signed out, show unauthenticated UI.
                updateUI(false);
            }
        }

        // Updates the ui to match the sign in state (true or false)
        private void updateUI(boolean b) {
            if(b) {
                findPreference("sync_service").setTitle("Sign-out of Roblu Cloud");
                findPreference("sync_service").setSummary("Signing out of Roblu Cloud will disable cloud functionality");
            } else {
                findPreference("sync_service").setTitle("Sign-in to Roblu Cloud");
                findPreference("sync_service").setSummary("Sign-in to Roblu Cloud using your Google account.");
            }
            settings.setSignedIn(b);
            new Loader(getActivity()).saveSettings(settings);
        }

        // Manages preferences that have a value and can be changed
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            if(preference.getKey().equals("team_number")) { // user changed their team number
                // We have to verify that the number they typed in is not above the Integer.MAX_VALUE or we'll get an error
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
            new Loader(getActivity()).saveSettings(settings); // save the settings to the file system
            return true;
        }
        private void joinTeam() {
            RUI rui = settings.getRui();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LinearLayout layout = new LinearLayout(getActivity());
            layout.setOrientation(LinearLayout.VERTICAL);

            final AppCompatEditText input = new AppCompatEditText(getActivity());
            Text.setInputTextLayoutColor(rui.getAccent(), rui.getText(), null, input);
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
                    // attempt to sign in with the provided team code
                    try {
                        JSONObject response = (JSONObject) new CloudRequest(settings.getAuth(), input.getText().toString(), Text.getDeviceID(getActivity())).joinTeam();
                        System.out.println(response);
                        if(response.get("status").toString().equalsIgnoreCase("success")) {
                            // it works
                            settings.setTeamCode(input.getText().toString());
                            new Loader(getActivity()).saveSettings(settings);
                            toggleJoinTeam(false);
                        } else { // didn't exist or already signed in
                            Text.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "Team doesn't exist.", true, 0);
                        }
                    } catch(Exception e) {
                        Text.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "Error occurred while contacting Roblu Server. Please try again later.", true, 0);
                    } finally {
                        dialog.dismiss();
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { dialog.cancel(); }
            });
            TextView view = new TextView(getActivity());
            view.setTextSize(Text.DPToPX(getActivity(), 5));
            view.setPadding(Text.DPToPX(getActivity(), 18), Text.DPToPX(getActivity(), 18), Text.DPToPX(getActivity(), 18), Text.DPToPX(getActivity(), 18));
            view.setText("Team code:\n(provided with Roblu Cloud purchase) ");
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

        @Override
        public void tokenRegenerated(String token) {
            settings.setTeamCode(token);
            new Loader(getActivity()).saveSettings(settings);
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
                Text.showSnackbar(findViewById(R.id.advsettings), getApplicationContext(), "No email clients installed.", true, 0);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}