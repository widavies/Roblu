package com.cpjd.roblu.teams.customsort;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.cpjd.roblu.R;
import com.cpjd.roblu.forms.elements.ESTextfield;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.ui.UIHandler;

public class CustomSort extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_customsort);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Custom sort");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        long eventID = getIntent().getLongExtra("eventID", 0);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setBackgroundColor(new Loader(getApplicationContext()).loadSettings().getRui().getPrimaryColor());
        RForm form = new Loader(getApplicationContext()).loadForm(getIntent().getLongExtra("eventID", 0));

        for(int i = 0; i < form.getPit().size(); i++) {
            if(form.getPit().get(i) instanceof ESTextfield) {
                form.getPit().remove(form.getPit().get(i));
                i = 0;
            }
        }

        SortTabAdapter adapter = new SortTabAdapter(getSupportFragmentManager(), form, eventID);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.addOnPageChangeListener(this);

        pager.setAdapter(adapter);

        tabLayout.setupWithViewPager(pager);

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

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

}
