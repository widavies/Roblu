package com.cpjd.roblu.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.EditText;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.teams.TeamsView;
import com.cpjd.roblu.tutorials.Tutorial;
import com.cpjd.roblu.utils.DisableSwipeViewPager;
import com.cpjd.roblu.utils.SetupFragmentAdapter;

public class SetupActivity extends Activity implements View.OnClickListener {
    private DisableSwipeViewPager pager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);


        if(getActionBar() != null) getActionBar().hide();

        pager = (DisableSwipeViewPager) findViewById(R.id.view_pager);
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
                EditText et = (EditText) findViewById(R.id.number_input);
                try {
                    RSettings settings = new Loader(getApplicationContext()).loadSettings();
                    settings.setTeamNumber(Integer.parseInt(et.getText().toString()));
                    new Loader(getApplicationContext()).saveSettings(settings);
                } catch(Exception e) {
                    try {
                        RSettings settings = new Loader(getApplicationContext()).loadSettings();
                        settings.setTeamNumber(0);
                        new Loader(getApplicationContext()).saveSettings(settings);
                    } catch(Exception e2) {}
                    System.out.println("failed to save team number "+e.getMessage());
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

