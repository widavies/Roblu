package com.cpjd.roblu.teams;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.forms.SaveThread;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.teams.fragments.TeamTabAdapter;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Text;

import static com.cpjd.roblu.R.id.won;

/*******************************************************
 * Copyright (C) 2016 Will Davies wdavies973@gmail.com
 *
 * This file is part of Roblu
 *
 * Roblu cannot be distributed for a price or to people outside of your local robotics team.
 *******************************************************/

public class TeamViewer extends AppCompatActivity implements ViewPager.OnPageChangeListener{

    private RUI rui;
    private REvent event;
    public static RTeam team;
    private boolean readOnly;

    // ViewPager
    private ViewPager pager;
    private TeamTabAdapter tabAdapter;
    private TabLayout tabLayout;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_viewer);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        event = (REvent) getIntent().getSerializableExtra("event");
        RCheckout tempCheckout = null;
        if(getIntent().getBooleanExtra("isConflict", false)) {
            tempCheckout = new Loader(getApplicationContext()).loadCheckoutConflict(getIntent().getLongExtra("checkout", 0));
            team = tempCheckout.getTeam();
        }
        else team = new Loader(getApplicationContext()).loadTeam(event.getID(), getIntent().getLongExtra("team", 0));
        readOnly = getIntent().getBooleanExtra("readOnly", false);

        if(readOnly) Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Read only mode is enabled when resolving conflicts", false, new Loader(getApplicationContext()).loadSettings().getRui().getPrimaryColor());

        RForm form = new Loader(getApplicationContext()).loadForm(event.getID());

        rui = new Loader(getApplicationContext()).loadSettings().getRui();

        // Verify the team
        if(!readOnly) {
            team.verify(form);
            new Loader(getApplicationContext()).saveTeam(team, event.getID());
        }

        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        getSupportActionBar().setTitle(team.getName());
        getSupportActionBar().setSubtitle("#"+String.valueOf(team.getNumber()));

        if(getIntent().getBooleanExtra("isConflict", false)) tabAdapter = new TeamTabAdapter(getSupportFragmentManager(), event, tempCheckout, form, getApplicationContext());
        else tabAdapter = new TeamTabAdapter(getSupportFragmentManager(), event, form, getApplicationContext(), readOnly);
        pager = (ViewPager) findViewById(R.id.pager);

        pager.addOnPageChangeListener(this);

        pager.setAdapter(tabAdapter);
        pager.setCurrentItem(team.getPage());

        tabLayout.setupWithViewPager(pager);
        tabLayout.setBackgroundColor(rui.getPrimaryColor());
        tabLayout.setSelectedTabIndicatorColor(rui.getAccent());
        tabLayout.setTabTextColors(rui.darker(rui.getText(), 0.95f), rui.getText());

        new UIHandler(this, toolbar).update();
        if(team.getPage() > 2) onPageSelected(team.getPage());
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent result = new Intent();
            // reload team
            result.putExtra("team", team.getID());
            setResult(Constants.TEAM_EDITED, result);
            finish();
            return true;
        }
        if(item.getItemId() == R.id.add_match) {
            openMatchCreater();
            return true;
        }
        if(item.getItemId() == R.id.match_settings) {
            showPopup();
            return true;
        }
        return false;
    }

    private void showPopup(){
        View menuItemView = findViewById(R.id.match_settings);
        final PopupMenu popup = new PopupMenu(TeamViewer.this, menuItemView);
        MenuInflater inflate = popup.getMenuInflater();
        inflate.inflate(R.menu.match_options, popup.getMenu());

        final PopupMenu.OnMenuItemClickListener popupListener = new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.match_page) {
                    if(event.getKey() == null || event.getKey().equals("")) {
                        Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "No event key found. Set it in event settings.", true, 0);
                        return false;
                    }

                    if(pager.getCurrentItem() == 0) Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Overiew can't be opened on TBA.", true, 0);
                    else if(pager.getCurrentItem() == 1) Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "PIT can't be opened on TBA.", true, 0);
                    else if(pager.getCurrentItem() == 2) Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Predictions can't be opened on TBA.", true, 0);
                    else {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse("https://www.thebluealliance.com/match/"+event.getKey()+"_"+Text.guessMatchKey(team.getTabs().get(pager.getCurrentItem() - 1).getTitle())));
                        startActivity(i);
                        popup.dismiss();
                    }
                    return true;
                }
                if(item.getItemId() == R.id.delete_match) {
                    if(pager.getCurrentItem() == 0) Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Overiew can't be deleted", true, 0);
                    else if(pager.getCurrentItem() == 1) Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "PIT can't be deleted", true, 0);
                    else if(pager.getCurrentItem() == 2) Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Predictions can't be deleted", true, 0);
                    else {
                        int pos = pager.getCurrentItem() - 1;
                        String title = team.getTabs().get(pos).getTitle();
                        team = tabAdapter.deleteTab(pos);
                        pager.setCurrentItem(pos);
                        popup.dismiss();
                        Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), title+" was successfully deleted.", false, rui.getPrimaryColor());
                        team.updateEdit();
                        new SaveThread(getApplicationContext(), event.getID(), team);
                    }
                    return true;
                }
                if(item.getItemId() == won) {
                    if(pager.getCurrentItem() == 0) Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Overiew can't be marked as won", true, 0);
                    else if(pager.getCurrentItem() == 1) Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "PIT can't be marked as won", true, 0);
                    else if(pager.getCurrentItem() == 2) Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Predictions can't be marked as won", true, 0);
                    else {
                        String title = team.getTabs().get(pager.getCurrentItem() - 1).getTitle();
                        boolean won = tabAdapter.markWon(pager.getCurrentItem() - 1);
                        if(won) Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), title+" marked as won.", false, rui.getPrimaryColor());
                        else Text.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), title+" marked as lost.", false, rui.getPrimaryColor());
                        popup.dismiss();
                        team.updateEdit();
                        new SaveThread(getApplicationContext(), event.getID(), team);
                    }
                }
                return true;
            }
        };
        popup.setOnMenuItemClickListener(popupListener);
        if(pager.getCurrentItem() > 2) {
            boolean won;
            if(readOnly) won = team.getTabs().get(pager.getCurrentItem()).isWon();
            else won = team.getTabs().get(pager.getCurrentItem() - 1).isWon();
            if(won && pager.getCurrentItem() > 2) popup.getMenu().getItem(0).setTitle("Mark as lost");
        }
        popup.show();

    }

    @Override
    public void onStop() {
        super.onStop();
        if(!getIntent().getBooleanExtra("isConflict", false) && !getIntent().getBooleanExtra("isSpecialConflict", false)) {
            // Check for modified tabs that need to be packaged
            for(int i = 0; i < TeamViewer.team.getTabs().size(); i++) {
                if(TeamViewer.team.getTabs().get(i).isModified()) {
                    TeamViewer.team.getTabs().get(i).setModified(false);
                    new Loader(getApplicationContext()).saveTeam(TeamViewer.team, event.getID());
                    RTeam temp = team.duplicate();
                    if(i == 0) temp.removeAllTabsButPIT();
                    else temp.removeAllTabsBut(i);

                    RCheckout checkout = new RCheckout(temp);
                    checkout.setHistoryID(new Loader(getApplicationContext()).getNewCheckoutID());
                    checkout.setSyncRequired(true);
                    checkout.setStatus("local-edit");
                    checkout.setConflictType("local-edit");
                    new Loader(getApplicationContext()).saveCheckout(checkout);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        result.putExtra("team", team.getID());
        setResult(Constants.TEAM_EDITED, result);
        finish();
    }
    @Override
    public void onPageSelected(int page) {
        if(!getIntent().getBooleanExtra("isConflict", false) && !getIntent().getBooleanExtra("isSpecialConflict", false)) {
            TeamViewer.team.setPage(page);
            new SaveThread(getApplicationContext(), event.getID(), TeamViewer.team);
        }

        if(page < 3) setColorScheme(rui.getPrimaryColor(), rui.darker(rui.getPrimaryColor(), 0.85f));
        else {
            if(tabAdapter.isPageRed(page)) setColorScheme(ContextCompat.getColor(getApplicationContext(), R.color.red), ContextCompat.getColor(getApplicationContext(), R.color.darkRed));
            else setColorScheme(ContextCompat.getColor(getApplicationContext(), R.color.primary), ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
        }
    }

    private void setColorScheme(int color, int darkColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(darkColor);
        }
        toolbar.setBackgroundColor(color);
        tabLayout.setBackgroundColor(color);
    }

    private void openMatchCreater() {
        final Dialog d = new Dialog(this);
        d.setTitle("Create match");
        d.setContentView(R.layout.match_create_dialog);
        final AppCompatEditText number = (AppCompatEditText) d.findViewById(R.id.editText);
        Text.setInputTextLayoutColor(rui.getAccent(), rui.getText(), null, number);
        TextView spinnerTip = (TextView) d.findViewById(R.id.spinner_tip);
        spinnerTip.setTextColor(rui.getText());
        TextView numberTip = (TextView) d.findViewById(R.id.number_tip);
        numberTip.setTextColor(rui.getText());
        TextView colorTip = (TextView) d.findViewById(R.id.color_tip);
        colorTip.setTextColor(rui.getText());
        Button button = (Button) d.findViewById(R.id.button7);
        button.setTextColor(rui.getText());
        button.setBackgroundColor(rui.getBackground());

        String[] values = {"Quals", "Quarters 1", "Quarters 2", "Quarters 3", "Quarters 4", "Semis 1", "Semis 2", "Finals"};
        final Spinner spinner = (Spinner) d.findViewById(R.id.type);
        spinner.getBackground().setColorFilter(rui.getText(), PorterDuff.Mode.SRC_ATOP);
        ArrayAdapter<String> adp = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,values);
        adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adp);

        final Spinner spinner2 = (Spinner) d.findViewById(R.id.spinner2);
        spinner2.getBackground().setColorFilter(rui.getText(), PorterDuff.Mode.SRC_ATOP);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check for duplicates!!
                final String processedName = processName(spinner, number);
                if(doesExist(processedName)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(TeamViewer.this);

                    builder.setTitle("Match already exists");
                    builder.setMessage("Would you like to go to its tab?");

                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            d.dismiss();
                            dialog.dismiss();
                            pager.setCurrentItem(getPosition(processedName));
                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    if(dialog.getWindow() != null) dialog.getWindow().getAttributes().windowAnimations = rui.getAnimation();
                    dialog.show();
                    dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(rui.getAccent());
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());
                } else {
                    boolean isRed = spinner2.getSelectedItemPosition() == 0;
                    pager.setCurrentItem(tabAdapter.createMatch(processedName, isRed));
                    d.dismiss();
                }
            }
        });

        if(d.getWindow() != null) {
            d.getWindow().getAttributes().windowAnimations = rui.getDialogDirection();
            d.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
        }
        d.getWindow().getAttributes().windowAnimations = rui.getDialogDirection();
        d.show();
    }

    private boolean doesExist(String name) {
        for(int i = 0 ; i < team.getTabs().size(); i++) if(team.getTabs().get(i).getTitle().equalsIgnoreCase(name)) return true;
        return false;
    }

    private int getPosition(String name) {
        for(int i = 0; i < team.getTabs().size(); i++) {
            if(name.equalsIgnoreCase(team.getTabs().get(i).getTitle())) return i + 1;
        }
        return 0;
    }

    private String processName(Spinner spinner, EditText text) {
        String value;

        switch(spinner.getSelectedItem().toString()) {
            case "Quals":
                value = "Quals ";
                break;
            case "Finals":
                value = "Finals ";
                break;
            default:
                value = spinner.getSelectedItem().toString() + " Match ";
                break;
        }

        if(text.getText().toString().equalsIgnoreCase("")) value += "1";
        else value += text.getText().toString();
        return value;
    }


    @Override
    public void onPageScrollStateChanged(int arg0) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Constants.GALLERY_EXIT) {
            team = new Loader(getApplicationContext()).loadTeam(event.getID(), data.getLongExtra("team", 0));
            tabAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
        hideKeyboard(this);
    }
    public void setActionBarTitle(String title) {
        if(getSupportActionBar() != null) getSupportActionBar().setTitle(title);
    }

    public void setActionBarSubtitle(String title) {
        if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.team_viewer_actionbar, menu);
        new UIHandler(this, menu).updateMenu();
        if(getIntent().getBooleanExtra("readOnly", false)) {
            menu.findItem(R.id.match_settings).setVisible(false);
            menu.findItem(R.id.add_match).setVisible(false);
        }
        return true;
    }

    private void hideKeyboard(Activity activity) {
        if (activity == null || activity.getCurrentFocus() == null || activity.getCurrentFocus().getWindowToken() == null) return;

        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
}
