package com.cpjd.roblu.ui.teams;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.cpjd.main.TBA;
import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.sync.cloud.Service;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.ui.events.EventDrawerManager;
import com.cpjd.roblu.ui.mymatches.MyMatches;
import com.cpjd.roblu.ui.setup.SetupActivity;
import com.cpjd.roblu.ui.team.TeamViewer;
import com.cpjd.roblu.ui.teamsSorting.CustomSort;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;

/**
 * TeamsView is the launcher activity.
 *
 * It manages a list of RTeam models and hosts various other general functions
 * @see RTeam
 *
 * @version 3
 * @since 3.0.0
 * @author Will Davies
 */
public class TeamsView extends AppCompatActivity implements View.OnClickListener, TeamsRecyclerAdapter.TeamSelectedListener, View.OnLongClickListener, EventDrawerManager.EventSelectListener, LoadTeamsTask.LoadTeamsTaskListener {
    /**
     * IO handles access between the file system and the models
     */
    private IO io;

    /**
     * Everything that has to do with events will be managed by the EventDrawerManager, which also takes care of the UI drawer
     * @see EventDrawerManager
     */
    private EventDrawerManager eventDrawerManager;

    /**
     * Provides a link to the background service for starting it or receiving information
     */
    private IntentFilter serviceFilter;

    /*
     * Teams
     */
    /**
     * A list of ALL teams within a certain even, this array can be sorted, re-arranged as desired, but
     * items should NOT be removed from it. Searches should occur by clearing the adapter and placing
     * teams in individually
     */
    private ArrayList<RTeam> teams;
    /**
     * The adapter is essentially the backend to the teams array, it helps manage UI binding, actions,
     * and more
     */
    private TeamsRecyclerAdapter adapter;
    /**
     * The UI front-end for the teams array, the backend of the recyclerView is the TeamsRecyclerAdapter
     */
    private RecyclerView rv;
    /**
     * A UI text field that collects a search query to sort teams by
     */
    private MaterialSearchView searchView;
    // End teams

    /*
     * Filters and searching
     */
    /**
     * The filter to use next time the team list is sorted, must be one of SORT_TYPE
     */
    private int lastFilter;
    /**
     * The query to use next time the team list is sorted, must be one of SORT_TYPE
     */
    private String lastQuery;
    /**
     * The custom sort token to use next time the team list is sorted, must be one of SORT_TYPE.
     * In the format of [TeamMetricProcessor.PROCESS_METHOD:metricID], example: 2:2
     */
    private String lastCustomSortToken;

    /**
     * Stores all the possible ways to sort the teams list.
     * {@link RTeam#compareTo(RTeam team)}
     */
    public static class SORT_TYPE {
        /**
         * Sort the teams by number, with the smallest number appearing at the top
         */
        public static final int NUMERICAL = 0;
        /**
         * Sort teams alphabetically, with 'A' starting at the top
         */
        public static final int ALPHABETICAL = 1;
        /**
         * Sorts teams by the time they were edited, with most recently edited at the top
         */
        public static final int LAST_EDIT = 2;
        /**
         * Sorts teams by search relevance, each team has a 'customRelevance' int, this is what is being sorted
         */
        public static final int SEARCH = 4;
        /**
         * Sorts teams by a sort token defined in CustomSort
         * @see CustomSort
         */
        public static final int CUSTOM_SORT = 3;

    }
    // End filters and searching

    /*
     * UI and other
     */
    /**
     * Reference to the application settings object
     */
    private RSettings settings;
    /**
     * When this button is clicked, the search view should appear
     */
    private FloatingActionButton searchButton;
    /**
     * The ProgressBar is displayed when a asynchronous change is being made to the
     * teams list
     */
    private ProgressBar bar;
    // End UI and other

    private LoadTeamsTask loadTeamsTask;

    private boolean ignoreOnResumeOnce;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teams_view);

        io = new IO(getApplicationContext());

        // Initialize startup requirements
        Utils.initWidth(this); // sets width for UI, needed for RMetricToUI
        if(IO.init(getApplicationContext())) { // checks if we need to show startup dialog
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }
        TBA.setID("Roblu", "Scouting-App", "v3"); //setup TBA api vars
        settings = io.loadSettings();

        /*
         * Setup UI
         */
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Roblu");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        // Progress bar, also make sure to hide it
        bar = findViewById(R.id.progress_bar);
        bar.setVisibility(View.GONE);

        // Recycler View, UI front-end to teams array
        rv = findViewById(R.id.teams_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);

        // Setup the team adapter
        adapter = new TeamsRecyclerAdapter(this, this);
        rv.setAdapter(adapter);

        // Setup the UI gestures manager and link to recycler view
        ItemTouchHelper.Callback callback = new TeamsRecyclerTouchHelper(adapter);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        // Search view
        searchView = findViewById(R.id.search_view);
        searchView.setHintTextColor(Color.BLACK);
        searchView.setHint("Name, number, or match");

        // Search button
        searchButton = findViewById(R.id.fab);
        searchButton.setOnClickListener(this);
        searchButton.setOnLongClickListener(this);

        // Link the search button appearance to the scrolling behavior of the recycler view
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if(dy > 0 && searchButton.isShown()) searchButton.hide();
                if(dy < 0 && !searchButton.isShown()) searchButton.show();
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) searchButton.show();
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        // If the user closes the search bar, refresh the teams view with all the original items
        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {}
            @Override
            public void onSearchViewClosed() {
                executeLoadTeamsTask(lastFilter, false);
                searchButton.setVisibility(FloatingActionButton.VISIBLE);
            }
        });

        // Listen for text in the search view, if text is found, complete teh search
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.closeSearch();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                lastQuery = newText;
                if(lastFilter == SORT_TYPE.CUSTOM_SORT) executeLoadTeamsTask(SORT_TYPE.CUSTOM_SORT, false);
                else executeLoadTeamsTask(SORT_TYPE.SEARCH, false);
                return true;
            }
        });

        // Make general changes to UI, keep it synced with RUI
        new UIHandler(this, toolbar, searchButton, true).update();

        // Reload last filter, custom sort, and query
        lastFilter = settings.getLastFilter();
        if(lastFilter == SORT_TYPE.CUSTOM_SORT || lastFilter == SORT_TYPE.SEARCH) lastFilter = SORT_TYPE.NUMERICAL; // custom sort or search can't be loaded at startup because lastQuery and lastCustomSortFilter aren't saved

        /*
         * Setup events drawer and load events to it
         */
        eventDrawerManager = new EventDrawerManager(this, toolbar, this);
        eventDrawerManager.selectEvent(settings.getLastEventID());

        // Check to see if the background service is running, if it isn't, start it
        serviceFilter = new IntentFilter();
        serviceFilter.addAction(Constants.SERVICE_ID);
        Log.d("RBS", "Is service running: "+Utils.isMyServiceRunning(getApplicationContext()));

        if(!Utils.isMyServiceRunning(getApplicationContext())) {
            Intent serviceIntent = new Intent(this, Service.class);
            startService(serviceIntent);
        }

        /*
         * Display update messages
         *
         */
        if (settings.getUpdateLevel() != Constants.VERSION) {
            settings.setUpdateLevel(Constants.VERSION);

            AlertDialog.Builder builder = new AlertDialog.Builder(TeamsView.this)
                    .setTitle("Changelist for Version 4.0.0")
                    .setMessage(Constants.UPDATE_MESSAGE)
                    .setPositiveButton("Rock on", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            AlertDialog dialog = builder.create();
            if(dialog.getWindow() != null) {
                dialog.getWindow().getAttributes().windowAnimations = settings.getRui().getDialogDirection();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(settings.getRui().getBackground()));
            }
            dialog.show();
            dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(settings.getRui().getAccent());

            io.saveSettings(settings);
        }
    }

    /**
     * This method can receive global UI refresh requests from other activities or from the background service.
     */
    private BroadcastReceiver uiRefreshRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("serverHealth") != null) {
                eventDrawerManager.setServerHealthString(intent.getStringExtra("serverHealth"));
                return;
            }

            // Return if the update was only for the team viewer
            if(intent.getBooleanExtra("teamViewerOnly", false)) {
                return;
            }

            executeLoadTeamsTask(lastFilter, true);
            // Make sure data is persistent
            settings = new IO(getApplicationContext()).loadSettings();
        }
    };

    /**
     * Displays the filter dialog and refreshes the team's list when the user selects the
     * desired filter method
     */
	private void showFilterDialog() {
        if(eventDrawerManager.getEvent() == null) return;

        final Dialog d = new Dialog(this);
        d.setContentView(R.layout.dialog_sort);
        TextView view = d.findViewById(R.id.sort_title);
        view.setTextColor(settings.getRui().getText());
        RadioGroup group  = d.findViewById(R.id.filter_group);
        for(int i = 0; i < group.getChildCount(); i++) {
            AppCompatRadioButton rb = (AppCompatRadioButton) group.getChildAt(i);
            if(i == lastFilter) rb.setChecked(true);
            rb.setTextColor(settings.getRui().getText());
            final int i2 = i;
            rb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(i2 == 3) {
                        Intent intent = new Intent(TeamsView.this, CustomSort.class);
                        intent.putExtra("eventID", eventDrawerManager.getEvent().getID());
                        startActivityForResult(intent, Constants.GENERAL);
                        d.dismiss();
                        return;
                    }
                    eventDrawerManager.getEvent().setLastFilter(i2);
                    io.saveEvent(eventDrawerManager.getEvent());
                    lastFilter = i2;
                    executeLoadTeamsTask(lastFilter, false);
                    d.dismiss();
                    settings.setLastFilter(lastFilter);
                    new IO(getApplicationContext()).saveSettings(settings);
                }
            });
        }
        if(d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(settings.getRui().getBackground()));
            d.getWindow().getAttributes().windowAnimations = settings.getRui().getAnimation();
        }
        d.show();
    }

    /**
     * Shows the team create dialog where the user can manually create a team
     */
    private void showTeamCreateDialog() {
        if(eventDrawerManager.getEvent() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final AppCompatEditText input = new AppCompatEditText(this);
        Utils.setInputTextLayoutColor(settings.getRui().getAccent(), null, input);
        input.setHighlightColor(settings.getRui().getAccent());
        input.setHintTextColor(settings.getRui().getText());
        input.setTextColor(settings.getRui().getText());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Team name");
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(30);
        input.setFilters(FilterArray);
        layout.addView(input);

        final AppCompatEditText input2 = new AppCompatEditText(this);
        Utils.setInputTextLayoutColor(settings.getRui().getAccent(), null, input2);
        input2.setHighlightColor(settings.getRui().getAccent());
        input2.setHintTextColor(settings.getRui().getText());
        input2.setTextColor(settings.getRui().getText());
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
                RTeam team = new RTeam(input.getText().toString(), Integer.parseInt(input2.getText().toString()), io.getNewTeamID(eventDrawerManager.getEvent().getID()));
                io.saveTeam(eventDrawerManager.getEvent().getID(), team);
                executeLoadTeamsTask(lastFilter, true);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { dialog.cancel(); }
        });
        TextView view = new TextView(this);
        view.setTextSize(Utils.DPToPX(getApplicationContext(), 5));
        view.setPadding(Utils.DPToPX(this, 18), Utils.DPToPX(this, 18), Utils.DPToPX(this, 18), Utils.DPToPX(this, 18));
        view.setText(R.string.create_team);
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

    /**
     * This method starts a LoadTeamsTasks to search, filter, or load teams from an event.
     * teamstaskComplete() will be called when LoadTeamsTasks finishes its job
     */
    private void executeLoadTeamsTask(int filter, boolean reload) {
        if(eventDrawerManager.getEvent() == null) return;

        if(loadTeamsTask != null) {
            loadTeamsTask.quit();
        }

        loadTeamsTask = new LoadTeamsTask(new IO(getApplicationContext()), this);
        loadTeamsTask.setTaskParameters(eventDrawerManager.getEvent().getID(), filter, lastQuery, lastCustomSortToken);
        if(!reload) loadTeamsTask.setTeams(teams);
        // Flag UI to look like it's loading something
        rv.setVisibility(View.GONE);
        bar.setVisibility(View.VISIBLE);
        bar.getIndeterminateDrawable().setColorFilter(settings.getRui().getAccent(), PorterDuff.Mode.MULTIPLY);
        // Start the actual loading
        loadTeamsTask.start();
    }

    /**
     * Receives data from activities originally launched from this class
     * @param requestCode the request code the original activity was launched with
     * @param resultCode the result code return from the launched activity
     * @param data any payload data received from the launched activity
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Constants.CUSTOM_SORT_CONFIRMED) { // the user selected a custom sort token, retrieve it and sort
            lastCustomSortToken = data.getStringExtra("sortToken");
            lastFilter = SORT_TYPE.CUSTOM_SORT;
            executeLoadTeamsTask(lastFilter, false);
            ignoreOnResumeOnce = true;
        }
        else if(Constants.MASTER_FORM == requestCode && resultCode == Constants.FORM_CONFIRMED) { // the user edited the master form, retrieve it and save it
            Bundle b = data.getExtras();
            if(b != null) {
                settings.setMaster((RForm) b.getSerializable("form"));
            }
            io.saveSettings(settings);
        }
        /*
         * This must occur BEFORE the straight up NEW_EVENT_CREATED return.
         * This will be called after the user does a TBA sync
         */
        else if(requestCode == Constants.EVENT_SETTINGS_REQUEST && resultCode == Constants.NEW_EVENT_CREATED) {
            executeLoadTeamsTask(lastFilter, true);
        }
        else if(resultCode == Constants.NEW_EVENT_CREATED) { // The user created an event, let's get the ID and select it
            Bundle d = data.getExtras();
            eventDrawerManager.loadEventsToDrawer();
            eventDrawerManager.selectEvent(d != null ? d.getInt("eventID") : 0);
        }
        else if(resultCode == Constants.MY_MATCHES_EXITED) {
            executeLoadTeamsTask(lastFilter, true);
        }
        else if(resultCode == Constants.CUSTOM_SORT_CANCELLED) { // user exited custom sort, don't make the button in the filter dialog still display custom sort
            lastFilter = settings.getLastFilter();
        }
        else if(resultCode == Constants.TEAM_EDITED) { // the user edited a team, let's toss it in the teams array and reload it also
            if(eventDrawerManager.getEvent() == null) return;

            RTeam temp = io.loadTeam(eventDrawerManager.getEvent().getID(), data.getIntExtra("teamID", 0));
            // Reload the teams array
            for(int i = 0; i < teams.size(); i++) {
                if(teams.get(i).getID() == temp.getID()) {
                    teams.set(i, temp);
                    break;
                }
            }
            // Reload the edited team into the adapter
            adapter.reAdd(temp);
            if(lastQuery != null && !lastQuery.equals("")) executeLoadTeamsTask(SORT_TYPE.SEARCH, false);
            else executeLoadTeamsTask(lastFilter, false);

            ignoreOnResumeOnce = true;
        }
        else if(resultCode == Constants.EVENT_SETTINGS_CHANGED) { // user edited the event
            int eventID = data.getIntExtra("eventID", 0);
            eventDrawerManager.loadEventsToDrawer();
            eventDrawerManager.selectEvent(eventID);
            if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(eventDrawerManager.getEvent().getName());
            executeLoadTeamsTask(lastFilter,true);
        }
        else if(resultCode == Constants.SETTINGS_CHANGED) { // user changed application settings (refresh UI to make sure it matches a possible RUI change)
            // reload settings
            settings = new IO(getApplicationContext()).loadSettings();

            eventDrawerManager.loadEventsToDrawer();
            if(eventDrawerManager.getEvent() != null) eventDrawerManager.selectEvent(eventDrawerManager.getEvent().getID());

           // new UIHandler(this, toolbar).update();
            //eventDrawerManager = new EventDrawerManager(this, toolbar, this);
        }
    }
    /**
     * Called when a new teams list is successfully loaded,
     * these teams should immediately stored
     */
    @Override
    public void teamsListLoaded(final ArrayList<RTeam> teams, final boolean hideZeroRelevanceTeams) {
        this.teams = teams;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bar.setVisibility(View.INVISIBLE);
                rv.setVisibility(View.VISIBLE);
                adapter.setTeams(teams, hideZeroRelevanceTeams);

                int numTeams = 0;
                if(teams != null) numTeams = teams.size();

                StringBuilder subtitle = new StringBuilder(String.valueOf(numTeams));
                subtitle.append(" Team");
                if(numTeams != 1) subtitle.append("s");
                if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(subtitle.toString());
            }
        });

    }

    @Override
    public boolean onLongClick(View v) {
        if(v.getId() == R.id.fab) {
            if(eventDrawerManager.getEvent() == null) return false;
            Intent intent = new Intent(this, MyMatches.class);
            intent.putExtra("eventID", eventDrawerManager.getEvent().getID());
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.teams_view_actionbar, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_team_create) {
            showTeamCreateDialog();
            return true;
        }
        else if(item.getItemId() == R.id.action_teams_filter) {
            showFilterDialog();
            return true;
        }
        return false;
    }
    @Override
    public void onClick(View v) {
        if(!searchView.isSearchOpen() && eventDrawerManager.getEvent() != null) {
            searchView.showSearch(true);
            searchButton.setVisibility(FloatingActionButton.INVISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if(searchView.isSearchOpen()) {
            searchView.closeSearch();
            lastFilter = SORT_TYPE.NUMERICAL;
            executeLoadTeamsTask(lastFilter, false);
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(eventDrawerManager.getBluetooth() != null) {
            eventDrawerManager.getBluetooth().onDestroy();
        }

    }
    @Override
    public void teamSelected(View v) {
        RTeam team = adapter.getTeams().get(rv.getChildLayoutPosition(v));
        Intent startView = new Intent(this, TeamViewer.class);
        startView.putExtra("teamID", team.getID());
        startView.putExtra("eventID", eventDrawerManager.getEvent().getID());
        startView.putExtra("editable", true);
        startActivityForResult(startView, Constants.GENERAL);
    }

    @Override
    public void teamDeleted(RTeam team) {
        io.deleteTeam(eventDrawerManager.getEvent().getID(), team.getID());
        executeLoadTeamsTask(lastFilter, true);
        Utils.showSnackbar(findViewById(R.id.main_layout), getApplicationContext(), team.getName()+" was successfully deleted", false, settings.getRui().getPrimaryColor());
    }

    @Override
    public void eventSelected(REvent event) {
        settings = new IO(getApplicationContext()).loadSettings(); // make sure to reload settings, since the event drawer manager will be modifying them
        executeLoadTeamsTask(lastFilter, true);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(uiRefreshRequestReceiver, serviceFilter);

        if(ignoreOnResumeOnce) {
            ignoreOnResumeOnce = false;
            return;
        }
        lastFilter = settings.getLastFilter();
        if(lastFilter == SORT_TYPE.CUSTOM_SORT || lastFilter == SORT_TYPE.SEARCH) lastFilter = SORT_TYPE.NUMERICAL; // custom sort or search can't be loaded at startup because lastQuery and lastCustomSortFilter aren't saved
        executeLoadTeamsTask(lastFilter, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(uiRefreshRequestReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
    }
}