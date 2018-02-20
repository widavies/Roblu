package com.cpjd.roblu.ui.team;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.util.Log;
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
import android.widget.Toast;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.models.metrics.RFieldDiagram;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.ui.dialogs.FastDialogBuilder;
import com.cpjd.roblu.ui.images.Drawing;
import com.cpjd.roblu.ui.team.fragments.TeamTabAdapter;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;


/**
 * TeamViewer is used for displaying information about a team, including
 * all the RTabs it includes.
 *
 * Bundle parameters for specifying TeamViewer behavior.
 * -"teamID" - the ID of the team to edit
 * -"eventID" - the event that contains the above team
 * -"editable" - whether scouting data should be editable, default: false
 *
 * -"checkout" - int extra containing ID of checkout to load
 * -"editable" - whether the team scouting data should be editable
 *
 * Bundle return
 * -"checkout" - int ID of the checkout that was modified
 *
 * @version 2
 * @since 1.0.0
 * @author Will Davies
 */
public class TeamViewer extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    private REvent event;

    /**
     * RCheckout contains the team, plus some generic meta-data pulled from the server.
     * It's kept static so that sub-activities like image loading don't have to re-load the
     * checkout. It's not practical to transfer RCheckout between activities because if it contains only
     * a couple pictures, it will exceed an Intent's payload size.
     */
    public static RTeam team;

    private TeamTabAdapter tabAdapter;
    private TabLayout tabLayout;
    private RUI rui;
    private Toolbar toolbar;

    private ViewPager pager;

    private boolean editable;

    /**
     * The team viewer needs to listen to the background service, if new data is inbound, we have to force close the viewer so the inbound scouting data
     * doesn't get overwritten
     */
    private IntentFilter serviceFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_viewer);

        // load the checkout that the user requested
        int teamID = getIntent().getIntExtra("teamID", 0);
        event = new IO(getApplicationContext()).loadEvent(getIntent().getIntExtra("eventID", 0));
        team = new IO(getApplicationContext()).loadTeam(event.getID(), teamID);

        /*
         Flag that determines if any of this team information should be editable. Team information
         should be read only if it's loaded from the "checkouts" list
         */
        editable = getIntent().getBooleanExtra("editable", true);

        /*
         * Optional parameter for choosing a page to go to
         */
        String requestedMatch = getIntent().getStringExtra("match");
        if(requestedMatch != null) {
            for(int i = 0; i < team.getTabs().size(); i++) {
                if(team.getTabs().get(i).getTitle().equalsIgnoreCase(requestedMatch)) {
                    team.setPage(i + 1);
                    break;
                }
            }
        }

        /*
         * What's the RForm reference for? It's used for verifying that a local checkout's form is matched with the Roblu Master form.
         * However, with update 4.0.0, we're not actually going to force a sync on the client if a form isn't available. Instead, all
         * incoming Checkouts will be re-verified by Roblu Master, so if the form can't be loaded here, no biggy.
         */
        RForm form = new IO(getApplicationContext()).loadForm(event.getID());
        if(form == null) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), this, "Form could not by synced with server. Local form may contain discrepancies.", true, 0);
        else { // verify the form
            team.verify(form);
            if(editable) new IO(this).saveTeam(event.getID(), team);
            //else Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Read only mode is enabled", false, new IO(getApplicationContext()).loadSettings().getRui().getPrimaryColor());
        }

        /*
         * Setup UI
         */
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rui = new IO(getApplicationContext()).loadSettings().getRui();
        tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        getSupportActionBar().setTitle(team.getName());
        getSupportActionBar().setSubtitle("#"+String.valueOf(team.getNumber()));
        tabAdapter = new TeamTabAdapter(getSupportFragmentManager(), event, form, getApplicationContext(), editable);
        pager = findViewById(R.id.pager);
        pager.addOnPageChangeListener(this);
        pager.setAdapter(tabAdapter);
        pager.setCurrentItem(team.getPage());
        onPageSelected(team.getPage());
        tabLayout.setupWithViewPager(pager);
        tabLayout.setBackgroundColor(rui.getPrimaryColor());
        tabLayout.setSelectedTabIndicatorColor(rui.getAccent());
        tabLayout.setTabTextColors(RUI.darker(rui.getText(), 0.95f), rui.getText());
        new UIHandler(this, toolbar).update();
        if(team.getPage() > 1) onPageSelected(team.getPage());

        /*
         * Attach to background service
         */
        serviceFilter = new IntentFilter();
        serviceFilter.addAction(Constants.SERVICE_ID);


    }

    /**
     * When the user returns home, return the ID of the checkout that was finished being edited, because it will
     * need to be loaded by the CheckoutView UI
     * @param item UI menu element pressed
     * @return true if a menu element selection was processed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent result = new Intent();
            result.putExtra("teamID", TeamViewer.team.getID());
            setResult(Constants.TEAM_EDITED, result);
            finish();
            return true;
        }
        else if(item.getItemId() == R.id.add_match) {
            showMatchCreator();
            return true;
        }
        else if(item.getItemId() == R.id.match_settings) {
            showPopup();
            return true;
        }
        return false;
    }

    /**
     * Same story as onOptionsItemSelected(), check the method above.
     */
    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        result.putExtra("teamID", team.getID());
        setResult(Constants.TEAM_EDITED, result);
        finish();
    }

    /**
     * Called when a new page is selected, some UI changes need to be made
     * (such as toolbar color change)
     * @param page the page index to switch to
     */
    @Override
    public void onPageSelected(int page) {
        if(page < 2) setColorScheme(rui.getPrimaryColor(), RUI.darker(rui.getPrimaryColor(), 0.85f));
        else {
            if(tabAdapter.isPageRed(page)) setColorScheme(ContextCompat.getColor(getApplicationContext(), R.color.red), ContextCompat.getColor(getApplicationContext(), R.color.darkRed));
            else setColorScheme(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary), ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
        }
        TeamViewer.team.setPage(page);
        new IO(getApplicationContext()).saveTeam(event.getID(), TeamViewer.team);
    }

    /**
     * Toggles the color of the toolbar
     * @param color the color to switch to
     * @param darkColor a dark complement color to color
     */
    private void setColorScheme(int color, int darkColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(darkColor);
        }
        toolbar.setBackgroundColor(color);
        tabLayout.setBackgroundColor(color);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Constants.GALLERY_EXIT) {
            team = new IO(getApplicationContext()).loadTeam(event.getID(), team.getID());
            tabAdapter.notifyDataSetChanged();
        }
        else if(resultCode == Constants.FIELD_DIAGRAM_EDITED) {
            int position = data.getIntExtra("position", 0);
            int ID = data.getIntExtra("ID", 0);
            byte[] drawings = Drawing.DRAWINGS;
            Drawing.DRAWINGS = null;

            for(RMetric metric : TeamViewer.team.getTabs().get(position).getMetrics()) {
                if(metric.getID() == ID) {
                    Log.d("RBS", "Updating metric!");
                    metric.setModified(true);
                    ((RFieldDiagram)metric).setDrawings(drawings);
                    break;
                }
            }
            TeamViewer.team.setLastEdit(System.currentTimeMillis());
            new IO(getApplicationContext()).saveTeam(event.getID(), TeamViewer.team);
            tabAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Hides the keyboard, sometimes the keyboard doesn't hide itself when we want it to
     * @param activity the activity reference
     */
    private void hideKeyboard(Activity activity) {
        if (activity == null || activity.getCurrentFocus() == null || activity.getCurrentFocus().getWindowToken() == null) return;

        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if(inputManager != null) inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
        hideKeyboard(this);
    }
    @Override
    public void onPageScrollStateChanged(int arg0) {}

    /**
     * Returns true if the match exists
     * @param name the match title
     * @return true if it exists
     */
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

    public void setActionBarTitle(String title) {
        if(getSupportActionBar() != null) getSupportActionBar().setTitle(title);
    }

    public void setActionBarSubtitle(String title) {
        if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(title);
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

    /**
     * Displays a match popup menu with various actions.
     *
     * "Mark as won"
     * "Open on TBA"
     * "Delete match"
     */
    private void showPopup() {
        View menuItemView = findViewById(R.id.match_settings);
        final PopupMenu popup = new PopupMenu(TeamViewer.this, menuItemView);
        MenuInflater inflate = popup.getMenuInflater();
        inflate.inflate(R.menu.match_options, popup.getMenu());

        final PopupMenu.OnMenuItemClickListener popupListener = new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.match_page) {
                    if(event.getKey() == null || event.getKey().equals("")) {
                        Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "No event key found. Set it in event settings.", true, 0);
                        return false;
                    }

                    if(pager.getCurrentItem() == 0) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Overview can't be opened on TBA.", true, 0);
                    else if(pager.getCurrentItem() == 1) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "PIT can't be opened on TBA.", true, 0);
                    else if(pager.getCurrentItem() == 2) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Predictions can't be opened on TBA.", true, 0);
                    else {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse("https://www.thebluealliance.com/match/"+event.getKey()+"_"+ Utils.guessMatchKey(team.getTabs().get(pager.getCurrentItem() - 1).getTitle())));
                        startActivity(i);
                        popup.dismiss();
                    }
                    return true;
                }
                if(item.getItemId() == R.id.delete_match) {
                    if(!editable) return true;

                    if(pager.getCurrentItem() == 0) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Overview can't be deleted", true, 0);
                    else if(pager.getCurrentItem() == 1) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "PIT can't be deleted", true, 0);
                    else if(pager.getCurrentItem() == 2) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Predictions can't be deleted", true, 0);
                    else {
                        int pos = pager.getCurrentItem() - 1;
                        String title = team.getTabs().get(pos).getTitle();
                        tabAdapter.deleteTab(pos);
                        pager.setCurrentItem(pos);
                        popup.dismiss();
                        Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), title+" was successfully deleted.", false, rui.getPrimaryColor());
                        new IO(getApplicationContext()).saveTeam(event.getID(), team);
                    }
                    return true;
                }
                if(item.getItemId() == R.id.won) {
                    if(!editable) return true;

                    if(pager.getCurrentItem() == 0) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Overview can't be marked as won", true, 0);
                    else if(pager.getCurrentItem() == 1) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "PIT can't be marked as won", true, 0);
                    else if(pager.getCurrentItem() == 2) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), "Predictions can't be marked as won", true, 0);
                    else {
                        String title = team.getTabs().get(pager.getCurrentItem() - 1).getTitle();
                        boolean won = tabAdapter.markWon(pager.getCurrentItem() - 1);
                        if(won) Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), title+" marked as won.", false, rui.getPrimaryColor());
                        else Utils.showSnackbar(findViewById(R.id.teams_viewer_layout), getApplicationContext(), title+" marked as lost.", false, rui.getPrimaryColor());
                        popup.dismiss();
                        new IO(getApplicationContext()).saveTeam(event.getID(), team);
                    }
                }
                return true;
            }
        };
        popup.setOnMenuItemClickListener(popupListener);
        if(pager.getCurrentItem() > 2) {
            boolean won;
            if(!editable) won = team.getTabs().get(pager.getCurrentItem()).isWon();
            else won = team.getTabs().get(pager.getCurrentItem() - 1).isWon();
            if(won && pager.getCurrentItem() > 2) popup.getMenu().getItem(0).setTitle("Mark as lost");
        }
        popup.show();

    }

    /**
     * Opens the manual match creator
     */
    private void showMatchCreator() {
        if(!editable) {
            Toast.makeText(getApplicationContext(), "Can't create match in read only mode.", Toast.LENGTH_LONG).show();
            return;
        }

        final Dialog d = new Dialog(this);
        d.setTitle("Create match");
        d.setContentView(R.layout.match_create_dialog);
        final AppCompatEditText number = d.findViewById(R.id.editText);
        Utils.setInputTextLayoutColor(rui.getAccent(), null, number);
        TextView spinnerTip = d.findViewById(R.id.spinner_tip);
        spinnerTip.setTextColor(rui.getText());
        TextView numberTip = d.findViewById(R.id.number_tip);
        numberTip.setTextColor(rui.getText());
        TextView colorTip = d.findViewById(R.id.color_tip);
        colorTip.setTextColor(rui.getText());
        Button button = d.findViewById(R.id.button7);
        button.setTextColor(rui.getText());
        button.setBackgroundColor(rui.getBackground());

        String[] values = {"Quals", "Quarters 1", "Quarters 2", "Quarters 3", "Quarters 4", "Semis 1", "Semis 2", "Finals"};
        final Spinner spinner = d.findViewById(R.id.type);
        spinner.getBackground().setColorFilter(rui.getText(), PorterDuff.Mode.SRC_ATOP);
        ArrayAdapter<String> adp = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,values);
        adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adp);

        final Spinner spinner2 = d.findViewById(R.id.spinner2);
        spinner2.getBackground().setColorFilter(rui.getText(), PorterDuff.Mode.SRC_ATOP);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check for duplicates!!
                final String processedName = processName(spinner, number);
                if(doesExist(processedName)) {
                    new FastDialogBuilder()
                            .setTitle("Match already exists")
                            .setMessage("Would you like to go to its tab?")
                            .setPositiveButtonText("Yes")
                            .setNegativeButtonText("No")
                            .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                                @Override
                                public void accepted() {
                                    d.dismiss();
                                    pager.setCurrentItem(getPosition(processedName));
                                }

                                @Override
                                public void denied() {}

                                @Override
                                public void neutral() {}
                            }).build(TeamViewer.this);
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

    /**
     * This method can receive global UI refresh requests from other activities or from the background service.
     */
    private BroadcastReceiver uiRefreshRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!intent.getBooleanExtra("teamViewerOnly", false)) {
                return;
            }
            // Force close UI if the team we are viewing was just updated.
            if(intent.getStringExtra("teamName") != null && intent.getStringExtra("teamName").equalsIgnoreCase(TeamViewer.team.getName())) launchParent();
        }
    };

    // Force closes the TeamViewer
    private void launchParent() {
        Toast.makeText(getApplicationContext(), "TeamViewer has been force closed because new data has been received.", Toast.LENGTH_LONG).show();
        setResult(Constants.CANCELLED);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.team_viewer_actionbar, menu);
        new UIHandler(this, menu).updateMenu();
        if(getIntent().getBooleanExtra("readOnly", false)) {
            menu.findItem(R.id.match_settings).setVisible(false);
            menu.findItem(R.id.add_match).setVisible(false);
            menu.findItem(R.id.add_match).setEnabled(false);
            menu.findItem(R.id.match_settings).setEnabled(false);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(uiRefreshRequestReceiver, serviceFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(uiRefreshRequestReceiver);
    }

}