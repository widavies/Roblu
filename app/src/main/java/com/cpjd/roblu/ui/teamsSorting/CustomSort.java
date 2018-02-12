package com.cpjd.roblu.ui.teamsSorting;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.metrics.RDivider;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;

/**
 * CustomSort manages advanced sorting methods that can't be achieved with regular sorting.
 * CustomSort is the UI selector that allows a user to select the custom sorting method they want to
 * use. CustomSort needs to output IDs that can be processed by
 * @see TeamMetricProcessor
 *
 * This physical class doesn't actually do much, since custom sort methods are contained under tabs.
 *
 * Interacting with this class:
 * -This class must receive an Intent extra "eventID" (used for loading the Form and list of matches in the "IN_MATCHES" sort method)
 * -This class will return int extra "method" (as defined by TeamMetricProcess.PROCESS_METHOD) and int extra "ID" (RMetric ID, if applicable) in the return Intent
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class CustomSort extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_customsort);

        /*
         * Load the form and remove the team NAME and NUMBER metrics (since the user can sort by
         * this already without using CustomSort). Good news is that we know the form will ALWAYS
         * keep the team NAME and NUMBER as ID 0 and 1 respectively.
         */
        RForm form = new IO(getApplicationContext()).loadForm(getIntent().getIntExtra("eventID", 0));

        for(int i = 0; i < form.getPit().size(); i++) {
            if(form.getPit().get(i).getID() == 0 || form.getPit().get(i).getID() == 1 || form.getPit().get(i) instanceof RDivider) {
                form.getPit().remove(i);
                i--;
            }
        }

        // Remove dividers - they are useless for sorting
        for(int i = 0; i < form.getMatch().size(); i++) {
            if(form.getMatch().get(i) instanceof RDivider) {
                form.getMatch().remove(i);
                i--;
            }
        }

        /*
         * Setup UI
         */
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Custom sort");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Setup tabs
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setBackgroundColor(new IO(getApplicationContext()).loadSettings().getRui().getPrimaryColor());

        // Setup the adapter - the back-end to the UI (manages all the MetricFragments)
        MetricSortAdapter adapter = new MetricSortAdapter(getSupportFragmentManager(), form, getIntent().getIntExtra("eventID", 0));
        ViewPager pager = findViewById(R.id.pager);
        pager.addOnPageChangeListener(this);
        pager.setAdapter(adapter);
        tabLayout.setupWithViewPager(pager);

        // Sync UI with user preferences
        new UIHandler(this, toolbar).update();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(Constants.CUSTOM_SORT_CANCELLED);
            finish();
            return true;
        }
        return false;
    }
    @Override
    public void onBackPressed() {
        setResult(Constants.CUSTOM_SORT_CANCELLED);
        finish();
    }
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
    @Override
    public void onPageSelected(int position) {}
    @Override
    public void onPageScrollStateChanged(int state) {}
}
