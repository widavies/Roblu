package com.cpjd.roblu.cloud.ui;

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

/**
 * Displays incoming and outgoing assignments
 */
public class Mailbox extends AppCompatActivity {

    private ViewPager pager;
    private MailAdapter adapter;
    private TabLayout tabLayout;
    private RUI rui;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_viewer);

        rui = new Loader(getApplicationContext()).loadSettings().getRui();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        adapter = new MailAdapter(getSupportFragmentManager(), getIntent().getLongExtra("eventID", 0));
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setCurrentItem(0);

        tabLayout.setupWithViewPager(pager);
        tabLayout.setBackgroundColor(rui.getPrimaryColor());
        tabLayout.setSelectedTabIndicatorColor(rui.getAccent());
        tabLayout.setTabTextColors(RUI.darker(rui.getText(), 0.95f), rui.getText());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        new UIHandler(this, toolbar).update();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
