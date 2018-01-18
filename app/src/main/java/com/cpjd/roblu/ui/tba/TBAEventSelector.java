package com.cpjd.roblu.ui.tba;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.cpjd.main.Settings;
import com.cpjd.main.TBA;
import com.cpjd.models.Event;
import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.ui.events.EventEditor;
import com.cpjd.roblu.ui.tutorials.TutorialsRecyclerTouchHelper;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * TBAEventSelector will fetch a list of events from TheBlueAlliance.com, the events are displayed in a
 * searchable, filterable list. When an event is selected, download the teams and matches within that event and
 * start up EventEditor so the user can setup a form they want.
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class TBAEventSelector extends AppCompatActivity implements TBAEventAdapter.TBAEventSelectListener, TBALoadEventsTask.LoadTBAEventsListener {
    /**
     * Stores a list of events so that they don't have to be re-downloaded when the user wants to search them, or a similar action
     */
    private ArrayList<Event> events;
    /**
     * The UI controller for the events list
     */
    private RecyclerView rv;
    /**
     * The tbaEventAdapter that manages the actively displaying events.
     * (Backend to the RecyclerView)
     */
    private TBAEventAdapter tbaEventAdapter;
    /**
     * A search view that makes events searchable
     */
    private MaterialSearchView searchView;
    /**
     * The selected year that the user has set
     */
    private int selectedYear;
    /**
     * A progress bar to display as events are being loaded
     */
    private ProgressBar bar;
    /**
     * true to only displays events containing the team number specified in RSettings, false to display all events
     * downloaded from the server
     */
    private boolean onlyShowMyEvents;

    private int teamNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apievent_select);

        teamNumber = new IO(getApplicationContext()).loadSettings().getTeamNumber();

        /*
         * Setup UI
         */
        // Setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Select an event");
        }

        // Progress bar
        bar = findViewById(R.id.progress_bar);
        bar.setVisibility(View.GONE);

        // Recycler view
        rv = findViewById(R.id.events_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);

        // Setup the tbaEventAdapter
        tbaEventAdapter = new TBAEventAdapter(this, this);
        rv.setAdapter(tbaEventAdapter);

        // Setup the gesture listener
        ItemTouchHelper.Callback callback = new TutorialsRecyclerTouchHelper();
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);
        /*
         * Create the "My Events / All Events" chooser
         */
        Spinner showTeam = findViewById(R.id.show_team);
        ArrayAdapter<String> sAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"My events", "All events"});
        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        showTeam.setAdapter(sAdapter);
        showTeam.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long l) {
                onlyShowMyEvents = parent.getItemAtPosition(position).toString().equals("All events");
                rv.setVisibility(View.INVISIBLE);
                bar.setVisibility(View.VISIBLE);
                TBALoadEventsTask task = new TBALoadEventsTask(bar, rv, tbaEventAdapter, TBAEventSelector.this, teamNumber, selectedYear, onlyShowMyEvents);
                task.execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        // If no team number is found, default to "All events"
        if(new IO(getApplicationContext()).loadSettings().getTeamNumber() == 0) showTeam.setSelection(1);

        /*
         * Setup the years spinner
         */
        Spinner yearsSpinner = findViewById(R.id.spinner);
        selectedYear = Calendar.getInstance().get(Calendar.YEAR);
        String[] years = new String[selectedYear - 1991];
        for(int i = years.length - 1; i >= 0; i--) years[Math.abs(i - years.length + 1)] = String.valueOf(1992 + i);
        final ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearsSpinner.setAdapter(adapter);
        yearsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedYear = Integer.parseInt(adapterView.getItemAtPosition(i).toString());
                rv.setVisibility(View.INVISIBLE);
                bar.setVisibility(View.VISIBLE);
                TBALoadEventsTask task = new TBALoadEventsTask(bar, rv, tbaEventAdapter, TBAEventSelector.this, teamNumber, selectedYear, onlyShowMyEvents);
                task.execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        /*
         * Setup the search view
         */
        searchView = findViewById(R.id.search_view);
        searchView.setHintTextColor(Color.BLACK);
        searchView.setHint("Search events");

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {}

            @Override
            public void onSearchViewClosed() {
                TBALoadEventsTask task = new TBALoadEventsTask(bar, rv, tbaEventAdapter, TBAEventSelector.this, teamNumber, selectedYear, onlyShowMyEvents);
                task.setEvents(events);
                task.execute();
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
                TBALoadEventsTask task = new TBALoadEventsTask(bar, rv, tbaEventAdapter, TBAEventSelector.this, teamNumber, selectedYear, onlyShowMyEvents);
                task.setEvents(events);
                task.setQuery(newText);
                task.execute();
                return true;
            }
        });

        // Sync UI with user preferences
        new UIHandler(this, toolbar).update();
    }

    /**
     * The user selected an event from the downloaded events, what happens next is we need to create a sort of
     * "partial REvent" and pass it to the EventEditor class so the user can decide which form they want, etc.
     * @param v the view that was tapped
     */
    @Override
    public void tbaEventSelected(View v) {
        /*
         * Force close the keyboard, Android doesn't always automatically do this
         */
        View view = this.getCurrentFocus();
        if(view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        // Get the event reference from the UI
        Event event = tbaEventAdapter.getEvents().get(rv.getChildLayoutPosition(v));

        Utils.showSnackbar(findViewById(R.id.activity_apievent_select), getApplicationContext(), "Downloading event...", false, new IO(getApplicationContext()).loadSettings().getRui().getPrimaryColor());

        /*
         * Import the event specifically, eventDownloaded(Event event) will receive the result of this
         * task execution
         */
        new ImportEvent(this).execute(event.key);
    }

    /**
     * Called when a error happens related to the API for TheBlueAlliance.com
     * @param errMsg the error message describing what went wrong
     */
    @Override
    public void errorOccurred(String errMsg) {
        Utils.showSnackbar(findViewById(R.id.activity_apievent_select), getApplicationContext(), errMsg, true, 0);
    }

    /**
     * Called when a specific event is downloaded, it should be passed off to EventEditor for
     * form creation
     * @param event the downloaded event
     */
    @Override
    public void eventDownloaded(Event event) {
        Log.d("RBS", "Event: "+event.name+" was downloaded.");
        Intent intent = new Intent(this, EventEditor.class);
        intent.putExtra("editing", false);
        intent.putExtra("key", event.key);
        intent.putExtra("name", event.name);
        intent.putExtra("tbaEvent", event);
        startActivityForResult(intent, Constants.GENERAL);
    }
    /**
     * Called when a new events list is successfully downloaded,
     * these events should immediately stored
     */
    @Override
    public void eventListDownloaded(ArrayList<Event> events) {
        this.events = events;
    }

    /**
     * Called when a child activity returns some data
     * @param requestCode the code the child activity was started with
     * @param resultCode the code the child activity returned
     * @param data any data included with the return
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*
         * An event was successfully created by EventEditor,
         * just close the current activity
         */
        if(resultCode == Constants.NEW_EVENT_CREATED) {
            Bundle b = data.getExtras();
            Intent intent = new Intent();
            intent.putExtras(b);
            setResult(Constants.NEW_EVENT_CREATED, intent);
            finish();
        }
    }

    /**
     * User decided they don't want to import an event, so just cancel
     */
    @Override
    public void onBackPressed() {
        setResult(Constants.CANCELLED);
        finish();
    }

    /**
     * Syncs the menu items with the UI
     * @param menu the menu to sync
     * @return true if the menu was created successfully here
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.api_eventpicker, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }

    /**
     * Called when one of the extra toolbar buttons is pressed
     * @param item the item that was pressed
     * @return true if the event is consumed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
         * User decided to not import an event
         */
        if(item.getItemId() == android.R.id.home) {
            setResult(Constants.CANCELLED);
            finish();
            return true;
        }
        /*
         * User wants to search events
         */
        else if(item.getItemId() == R.id.search) {
            searchView.showSearch(true);
            return true;
        }
        /*
         * User wants to manually import an event
         */
        else if(item.getItemId() == R.id.api_manual) {
            manualAdd();
            return false;
        }
        return false;
    }

    /**
     * ImportEvent is used when the user has tapped a specific Event and more specific info
     * needs to be downloaded for that event (teams, matches, etc.), so let's do that!
     */
    private static class ImportEvent extends AsyncTask<String, Void, Event> {
        /**
         * The TBA key of the event to download specific data for
         */
        private String key;
        /**
         * The listener that will be notified when the event is successfully imported
         */
        private TBALoadEventsTask.LoadTBAEventsListener listener;

        ImportEvent(TBALoadEventsTask.LoadTBAEventsListener listener) {
            this.listener = listener;
        }

        protected Event doInBackground(String... key) {
            this.key = key[0];

            /*
             * Make sure this thread has network permissions
             */
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
            StrictMode.setThreadPolicy(policy);

            // set what should be included in the event download
            Settings.defaults();

            // notify the listener of the downloaded event
            Event e = new TBA().getEvent(this.key);
            if(e != null) listener.eventDownloaded(new TBA().getEvent(this.key));
            else listener.errorOccurred("No event found with key: "+this.key+".");
            return null;
        }
    }

    /**
     * This is a dark ages method, it's for users who want to type in the event code manually
     */
    private void manualAdd() {
        AlertDialog.Builder builder = new AlertDialog.Builder(TBAEventSelector.this);
        builder.setTitle("Add event manually");

        LinearLayout layout = new LinearLayout(TBAEventSelector.this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText input = new EditText(TBAEventSelector.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("year,event code");
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Utils.showSnackbar(findViewById(R.id.activity_apievent_select), getApplicationContext(), "Downloading event...", false, new IO(getApplicationContext()).loadSettings().getRui().getPrimaryColor());

                    new ImportEvent(TBAEventSelector.this).execute(input.getText().toString().replaceFirst(",", ""));
                } catch(Exception e) {
                    e.printStackTrace();
                    Utils.showSnackbar(findViewById(R.id.activity_apievent_select), getApplicationContext(), "Invalid key: "+input.getText().toString()+".", true, 0);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}

