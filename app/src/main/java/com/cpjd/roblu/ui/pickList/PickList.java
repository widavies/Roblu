package com.cpjd.roblu.ui.pickList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
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
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RPickLists;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;

/**
 * PickList manages different pick list available. Here the user can create lists,
 * populate them with teams, and re-order teams.
 *
 * @since 4.5.7
 * @version 1
 * @author Will Davies
 */
public class PickList extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    private ViewPager pager;
    private TabLayout tabs;
    private PickListAdapter listAdapter;
    private Toolbar toolbar;
    private REvent event;

    public static RPickLists pickLists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_viewer);

        event = new IO(getApplicationContext()).loadEvent(getIntent().getIntExtra("eventID", 0));

        pickLists = new IO(getApplicationContext()).loadPickLists(event.getID());

        if(pickLists == null) pickLists = new RPickLists(null);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tabs = findViewById(R.id.tab_layout);
        tabs.setTabGravity(TabLayout.GRAVITY_FILL);


        getSupportActionBar().setTitle("Pick lists");
        getSupportActionBar().setSubtitle("Hold and drag to re-arrange");
        listAdapter = new PickListAdapter(getSupportFragmentManager(), getApplicationContext(), event);
        pager = findViewById(R.id.pager);
        pager.addOnPageChangeListener(this);
        pager.setAdapter(listAdapter);
        tabs.setupWithViewPager(pager);

        RUI rui = new IO(getApplicationContext()).loadSettings().getRui();

        tabs.setBackgroundColor(rui.getPrimaryColor());
        tabs.setSelectedTabIndicatorColor(rui.getAccent());
        tabs.setTabTextColors(RUI.darker(rui.getText(), 0.95f), rui.getText());
        new UIHandler(this, toolbar).update();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else if(item.getItemId() == R.id.list_delete) {
            listAdapter.deleteList(pager.getCurrentItem());
            return true;
        }
        else if(item.getItemId() == R.id.add_list) {
            showListCreateDialog();
            return true;
        }

        return false;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pick_list_actionbar, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Constants.TEAM_SEARCHED) {
            PickList.pickLists.getPickLists().get(data.getIntExtra("position", 0)).getTeamIDs().add(data.getIntExtra("teamID", 0));
            new IO(getApplicationContext()).savePickLists(event.getID(), PickList.pickLists);
        }
        listAdapter.reload();
    }

    private void showListCreateDialog() {
        RSettings settings = new IO(getApplicationContext()).loadSettings();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final AppCompatEditText input = new AppCompatEditText(this);
        Utils.setInputTextLayoutColor(settings.getRui().getAccent(), null, input);
        input.setHighlightColor(settings.getRui().getAccent());
        input.setHintTextColor(settings.getRui().getText());
        input.setTextColor(settings.getRui().getText());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("List name");
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(30);
        input.setFilters(FilterArray);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listAdapter.createList(input.getText().toString());
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { dialog.cancel(); }
        });
        TextView view = new TextView(this);
        view.setTextSize(Utils.DPToPX(getApplicationContext(), 5));
        view.setPadding(Utils.DPToPX(this, 18), Utils.DPToPX(this, 18), Utils.DPToPX(this, 18), Utils.DPToPX(this, 18));
        view.setText("Create list");
        view.setTextColor(settings.getRui().getText());
        AlertDialog dialog = builder.create();
        dialog.setCustomTitle(view);
        if(dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = settings.getRui().getAnimation();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(settings.getRui().getBackground()));
        }
        dialog.show();
        dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(settings.getRui().getAccent());
        dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(settings.getRui().getAccent());
    }
}
