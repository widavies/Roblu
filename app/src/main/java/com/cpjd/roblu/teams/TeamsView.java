package com.cpjd.roblu.teams;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cpjd.main.TBA;
import com.cpjd.roblu.R;
import com.cpjd.roblu.activities.AdvSettings;
import com.cpjd.roblu.activities.MyMatches;
import com.cpjd.roblu.activities.SetupActivity;
import com.cpjd.roblu.cloud.sync.Service;
import com.cpjd.roblu.cloud.ui.Mailbox;
import com.cpjd.roblu.events.CreateEventPicker;
import com.cpjd.roblu.events.EventSettings;
import com.cpjd.roblu.forms.EditForm;
import com.cpjd.roblu.forms.ElementsProcessor;
import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.teams.customsort.CustomSort;
import com.cpjd.roblu.tutorials.Tutorial;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Text;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

/*******************************************************
 * Copyright (C) 2016 Will Davies wdavies973@gmail.com
 *
 * This file is part of Roblu
 *
 * Roblu cannot be distributed for a price or to people outside of your local robotics team.
 *******************************************************/
public class TeamsView extends AppCompatActivity implements View.OnClickListener, TeamsItemClickListener, View.OnLongClickListener {

    // UI
    private Drawer drawer;
    private MaterialSearchView searchView;
    private RSettings settings;
    private RUI rui;
    private FloatingActionButton fab;
    private Toolbar toolbar;
    // Teams & Adapters
    private LinkedList<RTeam> teams; // should be an up to date copy of all teams
    private LinkedList<RTeam> activeTeams; // can be manipulated, adjusted, etc. this is the one that is displayed

    private RecyclerView rv;
    private TeamsAdapter adapter;
    private IntentFilter serviceFilter;

    // Filter & searching
    private int lastFilter;
	public static final int NUMERICAL = 0;
	public static final int ALPHABETICAL = 1;
	public static final int LAST_EDIT = 2;
    public static final int SORT = 3;
    public static final int SEARCH = 4; // when searching, teams are sorting be relevance
    private String lastQuery;
    private String lastSortToken;

    // Events
    private ArrayList<REvent> events;
    private REvent event; // active event

    // For snackbar
    private RelativeLayout layout;

    private ProgressBar bar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (new Loader(getApplicationContext()).checkSettings()) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }
        settings = new Loader(getApplicationContext()).loadSettings();
        rui = settings.getRui();
        if(rui == null) {
            rui = new RUI();
            settings.setRui(rui);
            new Loader(getApplicationContext()).saveSettings(settings);
        }
        setContentView(R.layout.activity_teams_view);
        layout = (RelativeLayout) findViewById(R.id.main_layout);
        bar = (ProgressBar) findViewById(R.id.progress_bar);
        bar.setVisibility(View.GONE);

        new Loader(getApplicationContext()).verifyFileCompatibility();

        activeTeams = new LinkedList<>();

        // Initialize UI elements
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        searchView = (MaterialSearchView) findViewById(R.id.search_view);
        searchView.setHintTextColor(Color.BLACK);
        searchView.setHint("Name, number, or match");
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);
        fab.setOnLongClickListener(this);
        rv = (RecyclerView) findViewById(R.id.teams_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        adapter = new TeamsAdapter(this, this);
        rv.setAdapter(adapter);

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && fab.isShown()) fab.hide();
                if (dy < 0 && !fab.isShown()) fab.show();

            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) fab.show();
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        ItemTouchHelper.Callback callback = new TeamTouchHelper(adapter);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        getSupportActionBar().setTitle("Roblu");
        initDrawer();

        // General initialization stuff
        TBA.setID("Roblu", "Scouting-App", "v3");
        Text.initWidth(this);

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                //Do some magic
            }

            @Override
            public void onSearchViewClosed() {
                new LoadTeams(false, "", lastSortToken, lastFilter).execute();
                fab.setVisibility(FloatingActionButton.VISIBLE);
            }
        });
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.closeSearch();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                new LoadTeams(false, newText, lastSortToken, lastFilter).execute();
                return true;
            }
        });

        // Display update messages
        if (settings.getUpdateLevel() != Constants.VERSION) {
            settings.setUpdateLevel(Constants.VERSION);

            AlertDialog.Builder builder = new AlertDialog.Builder(TeamsView.this)
                    .setTitle("Changelist for Version 3.5.9 - 3.6.0")
                    .setMessage("-Added my matches (long press on search button)\n-Improvements to searching and filtering\n-Ads removed, UI customizer available for everyone\n-Reworked cloud controls\n-Event import now searchable\n-Bug fixes\n\n" +
                            "Cloud support is coming in 3.6.1")
                    .setPositiveButton("Rock on", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            AlertDialog dialog = builder.create();
            if(dialog.getWindow() != null) {
                dialog.getWindow().getAttributes().windowAnimations = rui.getDialogDirection();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
            }
            dialog.show();
            dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());

            new Loader(getApplicationContext()).saveSettings(settings);
        }

        // locate the background service
        serviceFilter = new IntentFilter();
        serviceFilter.addAction("com.cpjd.roblu.broadcast.main");

        loadEvents();
        selectEvent(settings.getLastEventID());

        new UIHandler(this, toolbar, fab, true).update();

        // Start the service if it isn't running already
        if(!Text.isMyServiceRunning(getApplicationContext())) {
            Intent serviceIntent = new Intent(this, Service.class);
            startService(serviceIntent);
        }
    }

    // Init the material design drawer
    private void initDrawer() {
        // Drawables
        Drawable create = ContextCompat.getDrawable(getApplicationContext(), R.drawable.create);
        create.mutate(); create.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        Drawable master = ContextCompat.getDrawable(getApplicationContext(), R.drawable.master);
        master.mutate(); master.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        Drawable tutorials = ContextCompat.getDrawable(getApplicationContext(), R.drawable.school);
        tutorials.mutate(); tutorials.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        Drawable options = ContextCompat.getDrawable(getApplicationContext(), R.drawable.settings_circle);
        options.mutate(); options.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        Drawable cloud = ContextCompat.getDrawable(getApplicationContext(), R.drawable.cloud);
        cloud.mutate(); cloud.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);

        ArrayList<IDrawerItem> items = new ArrayList<>();
        items.add(new PrimaryDrawerItem().withIdentifier(Constants.CREATE_EVENT).withName("Create event").withIcon(create).withTextColor(rui.getText()));
        items.add(new com.cpjd.roblu.utils.DividerDrawerItem(rui.getText()));
        items.add(new SectionDrawerItem().withName("Events").withTextColor(rui.getText()).withDivider(false));
        items.add(new com.cpjd.roblu.utils.DividerDrawerItem(rui.getText()));
        items.add(new SecondaryDrawerItem().withTextColor(rui.getText()).withIdentifier(Constants.EDIT_MASTER_FORM).withName("Edit master form").withIcon(master));
        items.add(new SecondaryDrawerItem().withTextColor(rui.getText()).withIdentifier(Constants.TUTORIALS).withName("Tutorials").withIcon(tutorials));
        items.add(new SecondaryDrawerItem().withTextColor(rui.getText()).withIdentifier(Constants.SETTINGS).withName("Settings").withIcon(options));

        drawer = new DrawerBuilder().withActivity(this).withToolbar(toolbar).withDrawerItems(items).withSelectedItem(-1)
                .withActionBarDrawerToggleAnimated(true).withTranslucentStatusBar(false).withSliderBackgroundColor(rui.getBackground()).
                        withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                            @Override
                            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                                if(drawerItem.getIdentifier() == Constants.CREATE_EVENT) {
                                    startActivityForResult(new Intent(TeamsView.this, CreateEventPicker.class), Constants.CREATE_EVENT_PICKER);
                                    drawer.closeDrawer();
                                    drawer.setSelectionAtPosition(-1);
                                }
                                else if(drawerItem.getIdentifier() == Constants.SETTINGS) {
                                    startActivityForResult(new Intent(TeamsView.this, AdvSettings.class), Constants.GENERAL);
                                    drawer.closeDrawer();
                                    drawer.setSelectionAtPosition(-1);
                                }
                                else if(drawerItem.getIdentifier() == Constants.SCOUT) {
                                    drawer.closeDrawer();
                                    selectEvent((Long)drawerItem.getTag());
                                }
                                else if(drawerItem.getIdentifier() == Constants.TUTORIALS) {
                                    startActivity(new Intent(TeamsView.this, Tutorial.class));
                                    drawer.closeDrawer();
                                    drawer.setSelectionAtPosition(-1);
                                }
                                else if(drawerItem.getIdentifier() == Constants.EVENT_SETTINGS) {
                                    for(int i = 0; i < events.size(); i++) {
                                        if(events.get(i).getID() == (Long)drawerItem.getTag()) {
                                            Intent intent = new Intent(TeamsView.this, EventSettings.class);
                                            intent.putExtra("event", events.get(i));
                                            startActivityForResult(intent, Constants.GENERAL);
                                            drawer.setSelectionAtPosition(-1);
                                            drawer.closeDrawer();
                                            break;
                                        }
                                    }
                                }
                                else if(drawerItem.getIdentifier() == Constants.EDIT_MASTER_FORM) {
                                    Intent start = new Intent(TeamsView.this, EditForm.class);
                                    start.putExtra("master", true);
                                    startActivityForResult(start, Constants.MASTER_FORM);
                                    drawer.setSelectionAtPosition(-1);
                                    drawer.closeDrawer();
                                }
                                else if(drawerItem.getIdentifier() == Constants.MAILBOX) {
                                    for(int i = 0; i < events.size(); i++) {
                                        if(events.get(i).getID() == (Long)drawerItem.getTag()) {
                                            Intent intent = new Intent(TeamsView.this, Mailbox.class);
                                            intent.putExtra("eventID", events.get(i).getID());
                                            startActivityForResult(intent, Constants.GENERAL);
                                            drawer.setSelectionAtPosition(-1);
                                            drawer.closeDrawer();
                                            break;
                                        }
                                    }
                                }
                                return true;
                            }
                        }).build();
        if(drawer.getActionBarDrawerToggle() != null) {
            drawer.getActionBarDrawerToggle().getDrawerArrowDrawable().setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_ATOP);
            drawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
        }
    }


    // Set the event to display, return true if the event is found and succesfully selected
    private void selectEvent(long ID) {
        if(events == null || events.size() == 0) return;

        for(REvent e : events) {
            if(e.getID() == ID) {
                event = e;
                if(getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(event.getName());
                    getSupportActionBar().setSubtitle("Teams");
                }
                settings.setLastEventID(ID);
                new Loader(getApplicationContext()).saveSettings(settings);
                new LoadTeams(true, "", "", event.getLastFilter()).execute();
                return;
            }
        }

        settings.setLastEventID(ID);
        new Loader(getApplicationContext()).saveSettings(settings);
    }

    /*
     * Loads events into the drawer
     */
    private void loadEvents() {
        // Load events
        REvent[] local = new Loader(getApplicationContext()).getEvents();
        if(local == null) {
            if(getSupportActionBar() != null) getSupportActionBar().setSubtitle("No events :(");
            return;
        }

        Drawable folder = ContextCompat.getDrawable(getApplicationContext(), R.drawable.event);
        folder.mutate(); folder.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        Drawable scout = ContextCompat.getDrawable(getApplicationContext(), R.drawable.match);
        scout.mutate(); scout.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        Drawable options = ContextCompat.getDrawable(getApplicationContext(), R.drawable.settings_circle);
        options.mutate(); options.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        Drawable mail = ContextCompat.getDrawable(getApplicationContext(), R.drawable.mail);
        mail.mutate(); mail.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);

        events = new ArrayList<>(Arrays.asList(local));

        Collections.sort(events);
        Collections.reverse(events);

        ArrayList<IDrawerItem> items = new ArrayList<>();
        if(events != null) for(REvent e : events) {
            if(e.isCloudEnabled()) {
                items.add(new ExpandableDrawerItem().withTextColor(rui.getText()).withName(e.getName()).withTag(e.getID()).withArrowColor(rui.getText()).withIcon(folder).withIdentifier(Constants.HEADER).withSelectable(false).withSubItems(
                        new SecondaryDrawerItem().withTextColor(rui.getText()).withName("Scout").withLevel(2).withIcon(scout).withIdentifier(Constants.SCOUT).withTag(e.getID()),
                        new SecondaryDrawerItem().withTextColor(rui.getText()).withName("Mailbox").withLevel(2).withIcon(mail).withIdentifier(Constants.MAILBOX).withTag(e.getID()),
                        new SecondaryDrawerItem().withTextColor(rui.getText()).withName("Settings").withLevel(2).withIcon(options).withIdentifier(Constants.EVENT_SETTINGS).withTag(e.getID()))
                );
            } else {
                items.add(new ExpandableDrawerItem().withTextColor(rui.getText()).withName(e.getName()).withTag(e.getID()).withArrowColor(rui.getText()).withIcon(folder).withIdentifier(Constants.HEADER).withSelectable(false).withSubItems(
                        new SecondaryDrawerItem().withTextColor(rui.getText()).withName("Scout").withLevel(2).withIcon(scout).withIdentifier(Constants.SCOUT).withTag(e.getID()),
                        new SecondaryDrawerItem().withTextColor(rui.getText()).withName("Settings").withLevel(2).withIcon(options).withIdentifier(Constants.EVENT_SETTINGS).withTag(e.getID()))
                );
            }
        }

        // Clear old events
        for(int i = 0; i < drawer.getDrawerItems().size(); i++) {
            long identifier = drawer.getDrawerItems().get(i).getIdentifier();
            if(identifier == Constants.HEADER || identifier == Constants.SCOUT || identifier == Constants.EVENT_SETTINGS || identifier == Constants.MAILBOX) {
                drawer.removeItemByPosition(i);
                i = 0;
            }
        }

        // Add new events
        for(int i = 0; i < items.size(); i++) drawer.addItemAtPosition(items.get(i), i + 3);

    }

	private void addFilter() {
        if(event == null) return;

        final Dialog d = new Dialog(this);
        d.setContentView(R.layout.dialog_sort);
        TextView view = (TextView) d.findViewById(R.id.sort_title);
        view.setTextColor(rui.getText());
        RadioGroup group  = (RadioGroup) d.findViewById(R.id.filter_group);
        for(int i = 0; i < group.getChildCount(); i++) {
            AppCompatRadioButton rb = (AppCompatRadioButton) group.getChildAt(i);
            if(i == lastFilter) rb.setChecked(true);
            rb.setTextColor(rui.getText());
            ColorStateList colorStateList = new ColorStateList (
                    new int[][]{
                            new int[]{-android.R.attr.state_checked},
                            new int[]{android.R.attr.state_checked}
                    },
                    new int[] { rui.getText(),rui.getAccent(), }
            );
            rb.setSupportButtonTintList(colorStateList);
            final int i2 = i;
            rb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(i2 == 3) {
                        Intent intent = new Intent(TeamsView.this, CustomSort.class);
                        intent.putExtra("eventID", event.getID());
                        startActivityForResult(intent, Constants.GENERAL);
                        d.dismiss();
                        return;
                    }
                    event.setLastFilter(i2);
                    new Loader(getApplicationContext()).saveEvent(event);
                    new LoadTeams(false, "", "", i2).execute();
                    d.dismiss();
                }
            });
        }

        if(d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
            d.getWindow().getAttributes().windowAnimations = rui.getAnimation();
        }
        d.show();
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
            new LoadTeams(true, lastQuery, lastSortToken, lastFilter).execute();
        }
    };

    private void createTeam() {
        if(event == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final AppCompatEditText input = new AppCompatEditText(this);
        Text.setInputTextLayoutColor(rui.getAccent(), rui.getText(), null, input);
        input.setHighlightColor(rui.getAccent());
        input.setHintTextColor(rui.getText());
        input.setTextColor(rui.getText());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Team name");
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(30);
        input.setFilters(FilterArray);
        layout.addView(input);

        final AppCompatEditText input2 = new AppCompatEditText(this);
        Text.setInputTextLayoutColor(rui.getAccent(), rui.getText(), null, input2);
        input2.setHighlightColor(rui.getAccent());
        input2.setHintTextColor(rui.getText());
        input2.setTextColor(rui.getText());
        input2.setInputType(InputType.TYPE_CLASS_NUMBER);
        input2.setHint("Team number");
        InputFilter[] FilterArray2 = new InputFilter[1];
        FilterArray2[0] = new InputFilter.LengthFilter(6);
        input2.setFilters(FilterArray2);
        layout.addView(input2);

        builder.setView(layout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(input2.getText().toString().equals("")) input2.setText("0");
                RTeam team = new RTeam(input.getText().toString(), Integer.parseInt(input2.getText().toString()), new Loader(getApplicationContext()).getNewTeamID(event.getID()));
                new Loader(getApplicationContext()).saveTeam(team, event.getID());
                if(teams == null) teams = new LinkedList<>();
                teams.add(team);
                new LoadTeams(false, lastQuery, lastSortToken, lastFilter).execute();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { dialog.cancel(); }
        });
        TextView view = new TextView(this);
        view.setTextSize(Text.DPToPX(getApplicationContext(), 5));
        view.setPadding(Text.DPToPX(this, 18), Text.DPToPX(this, 18), Text.DPToPX(this, 18), Text.DPToPX(this, 18));
        view.setText(R.string.create_team);
        view.setTextColor(rui.getText());
        AlertDialog dialog = builder.create();
        dialog.setCustomTitle(view);
       if(dialog.getWindow() != null) {
           dialog.getWindow().getAttributes().windowAnimations = rui.getAnimation();
           dialog.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
       }
        dialog.show();
        dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(rui.getAccent());
        dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        loadEvents();
        if(resultCode == Constants.CUSTOM_SORT_CONFIRMED) {
            String sortToken = data.getStringExtra("sortToken");
            new LoadTeams(false, "", sortToken, SORT).execute();
        }
        if(Constants.MASTER_FORM == requestCode && resultCode == Constants.FORM_CONFIMRED) {
            Bundle b = data.getExtras();
            settings.setMaster(new RForm((ArrayList<Element>) b.getSerializable("tempPit"), ((ArrayList<Element>) b.getSerializable("tempMatch"))));
            new Loader(getApplicationContext()).saveSettings(settings);
            return;
        }
        if(resultCode == Constants.PICKER_EVENT_CREATED){
            Bundle d = data.getExtras();
            selectEvent(d.getLong("eventID"));
        }
        if(resultCode == Constants.TEAM_EDITED) {
            if(teams == null || teams.size() == 0 || event == null) return;
            RTeam temp = new Loader(getApplicationContext()).loadTeam(event.getID(), data.getLongExtra("team", 0));
            for(int i = 0; i < teams.size(); i++) {
                if(teams.get(i).getID() == temp.getID()) {
                    teams.set(i, temp);
                    break;
                }
            }
            LoadTeams lt = new LoadTeams(false, lastQuery, lastSortToken, lastFilter);
            lt.enableForceReload();
            lt.execute();
        }
        if(resultCode == Constants.DATA_SETTINGS_CHANGED) {
            REvent temp = (REvent) data.getSerializableExtra("event");
            if(event != null && temp.getID() == event.getID()) {
                if(getSupportActionBar() != null) getSupportActionBar().setTitle(temp.getName());
                event = temp;
            }
            new LoadTeams(true, lastQuery, lastSortToken, lastFilter).execute();
        }
        if(resultCode == Constants.SETTINGS_CHANGED) {
            settings = new Loader(getApplicationContext()).loadSettings();
            initDrawer();
            loadEvents();
            new UIHandler(this, toolbar, fab, true).update();
            adapter.notifyDataSetChanged();
        }
        if(resultCode == Constants.PREMIUM_PURCHASED) {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }

    }

    @Override
    public boolean onLongClick(View v) {
        if(v.getId() == R.id.fab) {
            if(event == null) return false;
            Intent intent = new Intent(this, MyMatches.class);
            intent.putExtra("eventID", event.getID());
            startActivity(intent);
            return true;
        }
        return false;
    }

    /*
     * Asynchronous method to load teams from the file system.
     * Manages the following:
     *
     * -Loading teams
     * -Sorting teams
     * -Setting teams list to the UI
     *
     * loadFromDisk parameter: whether a reload of the system is required
     */
    private class LoadTeams extends AsyncTask<Void, Void, LinkedList<RTeam>> {
        private final boolean loadFromDisk;
        private String query;
        private String sortToken;
        private final int filter;
        private boolean forceSortReload; // when a team is edited

        public LoadTeams(boolean loadFromDisk, String query, String sortToken, int filter) {
            this.loadFromDisk = loadFromDisk;
            this.query = query;
            this.sortToken = sortToken;
            this.filter = filter;
            lastFilter = filter;
            lastSortToken = sortToken;
            lastQuery = query;
            if(filter != SEARCH && filter != SORT) lastSortToken = "";
            if(this.query == null) this.query = "";
            if(this.sortToken == null) this.sortToken = "";

            if((loadFromDisk || (sortToken != null && !sortToken.equals("")))  && event != null) {
                rv.setVisibility(View.GONE);
                bar.setVisibility(View.VISIBLE);
                bar.getIndeterminateDrawable().setColorFilter(rui.getAccent(), PorterDuff.Mode.MULTIPLY);
            }
        }

        public void enableForceReload() {
            this.forceSortReload = true;
        }

        protected LinkedList<RTeam> doInBackground(Void... params) {
            // Before we do anything, we must verify that an event exists, if not, nothing needs to be done
            if(event == null || !new Loader(getApplicationContext()).doesEventExist(event.getID())) return null;

            // Alright, let's take care of loadFromDisk. If it's true, we gotta load all teams into the global teams array from the file system
            if(loadFromDisk) {
                RTeam[] local = new Loader(getApplicationContext()).getTeams(event.getID());
                if(local == null || local.length == 0)  {
                    teams = null;
                    return null; // There weren't any teams, so the discussion ends here
                }
                teams = new LinkedList<>(Arrays.asList(local));
            }

            // Okay, let's make sure that teams actually contains stuff, in case the array wasn't reloaded
            if(teams == null || teams.size() == 0) return null;

            for(RTeam team : teams) {
                if(team != null) team.setFilter(filter); // set the desired filter, this might get overrided though
                if(team != null && (filter == NUMERICAL || filter == ALPHABETICAL || filter == LAST_EDIT))team.resetSortRelevance();
            }

            /**
             * The sorting method will simply assign a sortTip and a relevance to each team
             *
             * Use CUSTOM filter to sort by these items, but if the user is searching these items, then SEARCH filter is more appropriate
             */
            if(!sortToken.equals("") && (query.equals("") || forceSortReload)) {
                lastFilter = SORT;
                Loader l = new Loader(getApplicationContext());
                RForm form = l.loadForm(event.getID());

                // first, let's verify the elements
                for(int i = 0; i < teams.size(); i++) {
                    teams.get(i).verify(form);
                    l.saveTeam(teams.get(i), event.getID());
                }

                int tab = Integer.parseInt(sortToken.split(":")[0]);
                int ID = Integer.parseInt(sortToken.split(":")[1]);

                // It's easier to process sort by specific match
                if(tab == ElementsProcessor.OTHER && ID == -1) {
                    activeTeams.clear();
                    for(RTeam tempTeam : teams) {
                        tempTeam.resetSortRelevance();
                        for(RTab temp : tempTeam.getTabs()) {
                            if(temp.getTitle().equalsIgnoreCase(sortToken.split(":")[2])) {
                                tempTeam.setSearchTip("In "+temp.getTitle());
                                tempTeam.setFilter(SORT);
                                activeTeams.add(tempTeam);
                            }
                        }
                    }
                    return activeTeams;
                } else {
                    ElementsProcessor ep = new ElementsProcessor();
                    for(int i = 0; i < teams.size(); i++) {
                        teams.set(i, ep.process(teams.get(i), tab, ID));
                    }
                }
            }

            // Okay, let's manage searching next
            if(!query.equals("")) {
                activeTeams.clear();
                int relevance;
                for(int i = 0; i < teams.size(); i++) {
                    relevance = 0;
                    teams.get(i).resetSearchRelevance();
                    String name = teams.get(i).getName().toLowerCase();
                    int number = teams.get(i).getNumber();

                    // Search criteria
                    if(name.equals(query)) relevance += 500;
                    if(String.valueOf(number).equals(query)) relevance += 500;
                    if(Text.contains(name, query)) relevance += 400;
                    if(name.contains(query)) relevance += 200;
                    if(String.valueOf(number).contains(query)) relevance += 200;
                    relevance += teams.get(i).searchMatches(query);

                    if(relevance > 0) {
                        teams.get(i).setSearchRelevance(teams.get(i).getSearchRelevance() + relevance);
                        teams.get(i).setFilter(SEARCH);
                        activeTeams.add(teams.get(i));
                    }
                }
                Collections.sort(activeTeams);
                Collections.reverse(activeTeams);
                return activeTeams;
            } else {
                if(teams != null && teams.size() > 0) for(int i = 0; i < teams.size(); i++) if(teams.get(i) != null) teams.get(i).resetSearchRelevance();
                if(activeTeams != null) activeTeams.clear();
            }

            // Okay, let's manage sorting / filter, for this, we want to display all teams, so display the teams array
            try {
                Collections.sort(teams);
                if(filter == SORT || filter == SEARCH || filter == LAST_EDIT) Collections.reverse(teams);
            } catch(final Exception e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Text.showSnackbar(layout, getApplicationContext(), "Team files are corrupt.", true, rui.getPrimaryColor());
                    }
                });
                return null;
            }
            return teams;
        }

        protected void onPostExecute(LinkedList<RTeam> result) {
            // Clear the teams list if applicable
            if(adapter != null) {
                adapter.removeAll();
                adapter.setElements(result);
            }
            updateActionBar(result);
            rv.setVisibility(View.VISIBLE);
            bar.setVisibility(View.GONE);

        }
    }

    private void updateActionBar(LinkedList<RTeam> result) {
        if(event != null) {
            String subtitle;
            if(result != null) {
                if(result.size() == 1) subtitle = "1 Team";
                else subtitle = result.size()+" Teams";
            }
            else subtitle = "0 Teams";
            if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(subtitle);
        }
    }

    /*
     * Irrelevant methods
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.teams_view_actionbar, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_team_create) {
            createTeam();
            return true;
        }
        else if(item.getItemId() == R.id.action_teams_filter) {
            addFilter();
            return true;
        }
        return false;
    }
    @Override
    public void onClick(View v) {
        if(!searchView.isSearchOpen() && event != null) {
            searchView.showSearch(true);
            fab.setVisibility(FloatingActionButton.INVISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if(searchView.isSearchOpen()) {
            searchView.closeSearch();
            new LoadTeams(false, "", lastSortToken, lastFilter).execute();
        }
    }

    @Override
    public void onItemClick(View v) {
        int itemPosition = rv.getChildLayoutPosition(v);
        RTeam team = adapter.getTeam(itemPosition);
        Intent startView = new Intent(this, TeamViewer.class);
        startView.putExtra("team", team.getID());
        startView.putExtra("event", event);
        startView.putExtra("readOnly", false);
        startActivityForResult(startView, Constants.GENERAL);
    }

    @Override
    public void deleteTeam(RTeam team) {
        for(int i= 0; i < teams.size(); i++) {
            if(team.getID() == teams.get(i).getID()) {
                teams.remove(i);
                new Loader(getApplicationContext()).deleteTeam(team, event.getID());
                break;
            }
        }
        Text.showSnackbar(layout, getApplicationContext(), team.getName()+" was successfully deleted", false, rui.getPrimaryColor());
        new LoadTeams(false, lastQuery, lastSortToken, lastFilter).execute();
    }
}