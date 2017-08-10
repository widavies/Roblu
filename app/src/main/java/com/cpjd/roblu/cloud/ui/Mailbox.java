package com.cpjd.roblu.cloud.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.cpjd.roblu.R;
import com.cpjd.roblu.cloud.ui.fragments.MailAdapter;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;

/**
 * Displays incoming and outgoing assignments
 */
public class Mailbox extends AppCompatActivity {

    private MailAdapter adapter;
    private IntentFilter serviceFilter;
    public static boolean addedConflict;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_viewer);

        addedConflict = false;

        RUI rui = new Loader(getApplicationContext()).loadSettings().getRui();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        adapter = new MailAdapter(getSupportFragmentManager(), getIntent().getLongExtra("eventID", 0));
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setCurrentItem(0);

        tabLayout.setupWithViewPager(pager);
        tabLayout.setBackgroundColor(rui.getPrimaryColor());
        tabLayout.setSelectedTabIndicatorColor(rui.getAccent());
        tabLayout.setTabTextColors(rui.darker(rui.getText(), 0.95f), rui.getText());

        // locate the background service
        serviceFilter = new IntentFilter();
        serviceFilter.addAction("com.cpjd.roblu.broadcast");

        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        new UIHandler(this, toolbar).update();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(serviceReceiver, serviceFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(serviceReceiver);
    }

    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int mode = intent.getIntExtra("mode", 0);
            if(mode == 0) adapter.forceUpdate(true);
            else if(mode == 1) adapter.forceUpdate(false);
            else {
                adapter.forceUpdate(true);
                adapter.forceUpdate(false);
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if(addedConflict) setResult(Constants.MAILBOX_EXITED);
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if(addedConflict) setResult(Constants.MAILBOX_EXITED);
        finish();
    }

}
