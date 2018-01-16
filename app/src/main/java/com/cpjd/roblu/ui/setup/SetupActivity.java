package com.cpjd.roblu.ui.setup;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.ui.teams.TeamsView;
import com.cpjd.roblu.ui.tutorials.Tutorial;

/**
 * Manages the initial setup of the application
 *
 * Steps:
 * -Welcome the user
 * -Agree to terms & privacy (explains what we store about the user)
 * -Permissions (in newer versions of Android, some permissions must be explicity requested with a dialog, in this case, Roblu only needs the camera (for the gallery element), and local
 * device storage (for the gallery element save to device feature)
 * -team number - user can set their team number at the get go if they want
 * -tutorials & final welcome - welcome the user again and give them a quick link to tutorials
 *
 * @since 3.0.0
 * @author Will Davies
 */
public class SetupActivity extends Activity implements View.OnClickListener {
    // manages our horizontal swipe view thing, but we don't let the user swipe, they must press next buttons
    private DisableSwipeViewPager pager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        if(getActionBar() != null) getActionBar().hide();

        pager = findViewById(R.id.view_pager);
        View welcomeNextButton = findViewById(R.id.welcome_next_page);
        View bluetoothNextButton = findViewById(R.id.bluetooth_next_page);
        View permsNext = findViewById(R.id.permissions_next_page);
        View finished = findViewById(R.id.finished_next);
        pager.setOffscreenPageLimit(4);
        pager.setAdapter(new SetupFragmentAdapter(this));
        findViewById(R.id.ytube).setOnClickListener(this);
        findViewById(R.id.number_next).setOnClickListener(this );

        welcomeNextButton.setOnClickListener(this);
        bluetoothNextButton.setOnClickListener(this);
        permsNext.setOnClickListener(this);
        finished.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        pager.goToNextPage();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ytube:
                startActivity(new Intent(this, Tutorial.class));
                break;
            case R.id.welcome_next_page:
                pager.goToNextPage();
                break;
            case R.id.bluetooth_next_page:
                pager.goToNextPage();
                break;
            case R.id.permissions_next_page:
                if(android.os.Build.VERSION.SDK_INT < 23) { // Below API Level 23 doesn't require asking for permissions
                    pager.goToNextPage();
                    break;
                }
                String[] perms = {
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                };
                ActivityCompat.requestPermissions(this, perms, 0);
                break;
            case R.id.number_next:
                EditText et = findViewById(R.id.number_input);
                EditText et2 = findViewById(R.id.username);
                try {
                    RSettings settings = new IO(getApplicationContext()).loadSettings();
                    settings.setTeamNumber(Integer.parseInt(et.getText().toString()));
                    settings.setUsername(et2.getText().toString());
                    new IO(getApplicationContext()).saveSettings(settings);
                } catch(Exception e) {
                    try {
                        RSettings settings = new IO(getApplicationContext()).loadSettings();
                        settings.setTeamNumber(0);
                        settings.setUsername("");
                        new IO(getApplicationContext()).saveSettings(settings);
                    } catch(Exception e2) {
                        Log.d("RBS", "Failed to save team number.");
                    }
                    Log.d("RBS", "Failed to save team number.");
                }
                pager.goToNextPage();
                break;
            case R.id.finished_next:
                setupFinished();
                break;
        }
    }

    private void setupFinished() {
        startActivity(new Intent(this, TeamsView.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        pager.goToPreviousPage();
    }
}

