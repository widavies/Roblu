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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.cpjd.roblu.R;
import com.cpjd.roblu.cloud.ui.CreateCloudTeam;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.ui.UICustomizer;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Text;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class AdvSettings extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static RSettings settings;
    private RUI rui;

    private static GoogleApiClient apiClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("Settings");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        settings = new Loader(getApplicationContext()).loadSettings();

        getFragmentManager().beginTransaction()
                .replace(R.id.blankFragment, new SettingsFragment())
                .commit();


        rui = new Loader(getApplicationContext()).loadSettings().getRui();

        new UIHandler(this, (Toolbar)findViewById(R.id.toolbar)).update();

        // setup Google signin
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        apiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, this).addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Text.showSnackbar(findViewById(R.id.advsettings), getApplicationContext(), "Failed to connect to Google services", true, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        new UIHandler(this, (Toolbar)findViewById(R.id.toolbar)).update();
    }

    @SuppressWarnings("WeakerAccess")
    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (!(getActivity() instanceof AdvSettings)) {
                getActivity().finish();
                return;
            }

            addPreferencesFromResource(R.xml.preferences);

            EditTextPreference teamNumber = (EditTextPreference) findPreference("team_number");
            teamNumber.setDefaultValue(settings.getTeamNumber());
            teamNumber.setOnPreferenceChangeListener(this);

            findPreference("about").setOnPreferenceClickListener(this);
            findPreference("privacy").setOnPreferenceClickListener(this);
            findPreference("git").setOnPreferenceClickListener(this);
            findPreference("customizer").setOnPreferenceClickListener(this);
            findPreference("sync_service").setOnPreferenceClickListener(this);
            findPreference("display_code").setOnPreferenceClickListener(this);

            toggleCloudControls(settings.isSignedIn());

        }

        private void toggleCloudControls(boolean b) {
            findPreference("create_cloud_team").setEnabled(b);
            findPreference("display_code").setEnabled(b);
            findPreference("delete_team").setEnabled(b);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if(preference.getKey().equals("display_code")) {
                Text.showTeamCode(getActivity(), "234x99d");
                return true;
            }
            else if(preference.getKey().equals("create_cloud_team")) {
                startActivity(new Intent(getActivity(), CreateCloudTeam.class));
                return true;
            }
            else if(preference.getKey().equals("sync_service")) {
                if(new Loader(getActivity()).loadSettings().isSignedIn()) { // it will be the opposite because we are in the process of switching
                    Auth.GoogleSignInApi.signOut(apiClient).setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            toggleCloudControls(!status.isSuccess());
                            if(status.isSuccess()) Text.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "Signed out successfully", true, 0);
                            else Text.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "Sign out failed", true, 0);
                        }
                    });
                    updateUI(false);
                } else {
                    Intent signIn = Auth.GoogleSignInApi.getSignInIntent(apiClient);
                    startActivityForResult(signIn, Constants.CLOUD_SIGN_IN);
                }
                return true;
            }
            else if(preference.getKey().equals("about")) {
                Intent start = new Intent(getActivity(), About.class);
                startActivity(start);
                return true;
            }
            else if(preference.getKey().equals("tut")) {
                String url = "https://docs.google.com/document/d/1fp3aq5ta4SpJE6AOiHujj8NVpqYdq6gSVoLAl3wQ7f8/edit?usp=sharing";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
            }
            else if(preference.getKey().equals("privacy")) {
                String url = "https://www.cpjd.weebly.com/privacy";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
            }
            else if(preference.getKey().equals("git")) {
                String url = "https://www.github.com/wdavies973/Roblu";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
            }
            else if(preference.getKey().equals("customizer")) {
                if(!settings.isPremium()) {
                    Text.showSnackbar(getActivity().findViewById(R.id.advsettings), getActivity(), "You don't have premium", true, 0);
                    return false;
                }
                getActivity().startActivityForResult(new Intent(getActivity(), UICustomizer.class), Constants.GENERAL);
                return true;
            }
            return false;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == Constants.CLOUD_SIGN_IN) {
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(result);
            }
        }
        private void handleSignInResult(GoogleSignInResult result) {
            if (result.isSuccess()) {
                toggleCloudControls(true);
                // Signed in successfully, show authenticated UI.
                GoogleSignInAccount acct = result.getSignInAccount();
                updateUI(true);
            } else {
                // Signed out, show unauthenticated UI.
                updateUI(false);
            }
        }
        private void updateUI(boolean b) {
            if(b) {
                findPreference("sync_service").setTitle("Sign-out of Roblu Cloud");
                findPreference("sync_service").setSummary("Signing out of Roblu Cloud will disable cloud functionality");
            } else {
                findPreference("sync_service").setTitle("Sign-in to Roblu Cloud");
                findPreference("sync_service").setSummary("Sign-in to Roblu Cloud using your Google Account.");
            }
            settings.setSignedIn(b);
            new Loader(getActivity()).saveSettings(settings);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            if(preference.getKey().equals("team_number")) {
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
            if(preference.getKey().equals("quality")) {
                int num;
                try {
                    num = Integer.parseInt(o.toString());
                } catch(Exception e) {
                    num = 100;
                }
                if(num > 100) {
                    num = 100;
                    preference.setDefaultValue(num);
                }
            }
            new Loader(getActivity()).saveSettings(settings);
            return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_actionbar, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }

    @Override
    public void onBackPressed() {
        setResult(Constants.SETTINGS_CHANGED);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            setResult(Constants.SETTINGS_CHANGED);
            finish();
            return true;
        }
        if (id == R.id.settings_bug) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"wdavies973@gmail.com"});
            i.putExtra(Intent.EXTRA_SUBJECT, "Roblu Bug Report");
            i.putExtra(Intent.EXTRA_TEXT   , "The information below includes how to reproduce the bug and a thorough description of what went wrong. (Thanks - WD)");
            try {
                startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (ActivityNotFoundException ex) {
                Text.showSnackbar(findViewById(R.id.advsettings), getApplicationContext(), "No email clients installed.", true, rui.getPrimaryColor());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}