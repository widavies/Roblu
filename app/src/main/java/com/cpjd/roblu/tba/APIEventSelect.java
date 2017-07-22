package com.cpjd.roblu.tba;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.teams.customsort.SelectListener;
import com.cpjd.roblu.tutorials.TutorialTouchHelper;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Text;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

/**
 * To add a new year, go into res/values/strings.xml and add it to the years array there.
 *
 * Then, change the selectedYear variable in onCreate() to the year
 *
 * You might want to automate this in the future
 *
 * @since 3.0.0
 * @author Will Davies
 */
public class APIEventSelect extends AppCompatActivity implements AdapterView.OnItemSelectedListener, SelectListener {

    private EventAdapter adapter;
    private ArrayList<Event> events;
    private ArrayList<Event> active;

    private RecyclerView rv;

    private MaterialSearchView searchView;

    private int selectedYear;
    //private RUI rui;

    private ProgressBar bar;

    private boolean showAllEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apievent_select);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle("Select an event");

        bar = (ProgressBar) findViewById(R.id.progress_bar);
        bar.setVisibility(View.GONE);

        rv = (RecyclerView) findViewById(R.id.events_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        adapter = new EventAdapter(this, this);
        rv.setAdapter(adapter);
        ItemTouchHelper.Callback callback = new TutorialTouchHelper();
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        searchView = (MaterialSearchView) findViewById(R.id.search_view);
        searchView.setHintTextColor(Color.BLACK);
        searchView.setHint("Search events");

        Spinner showTeam = (Spinner) findViewById(R.id.show_team);
        ArrayAdapter<String> sAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"My events", "All events"});
        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        showTeam.setAdapter(sAdapter);
        showTeam.setOnItemSelectedListener(this);
        if(new Loader(getApplicationContext()).loadSettings().getTeamNumber() == 0) showTeam.setSelection(1);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        //spinner.getBackground().setColorFilter(rui.getText(), PorterDuff.Mode.SRC_ATOP);
        //ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.years_array, android.R.layout.simple_spinner_item);

        selectedYear = Calendar.getInstance().get(Calendar.YEAR);
        String[] years = new String[selectedYear - 1991];
        for(int i = years.length - 1; i >= 0; i--) years[Math.abs(i - years.length + 1)] = String.valueOf(1992 + i);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        new UIHandler(this, toolbar).update();

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                //Do some magic
            }

            @Override
            public void onSearchViewClosed() {
                new SearchEvents("").execute();
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
                new SearchEvents(newText).execute();
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.api_eventpicker, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(Constants.EVENT_IMPORT_CANCELLED);
            finish();
            return true;
        }
        if(item.getItemId() == R.id.search) {
            searchView.showSearch(true);
            return true;
        }
        if(item.getItemId() == R.id.api_manual) {
            manualAdd();
            return false;
        }
        return false;
    }
    @Override
    public void onBackPressed() {
        setResult(Constants.EVENT_IMPORT_CANCELLED);
        finish();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long l) {
        if(parent.getId() == R.id.spinner) selectedYear = Integer.parseInt(parent.getItemAtPosition(position).toString());
        else showAllEvents = parent.getItemAtPosition(position).toString() == "All events";

        new FetchEvents(showAllEvents).execute(selectedYear);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void manualAdd() {
        AlertDialog.Builder builder = new AlertDialog.Builder(APIEventSelect.this);
        builder.setTitle("Add event manually");

        LinearLayout layout = new LinearLayout(APIEventSelect.this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText input = new EditText(APIEventSelect.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("year,event code");
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    new ImportEvent().execute(input.getText().toString().replaceFirst(",", ""));
                } catch(Exception e) {
                    e.printStackTrace();
                    Text.showSnackbar(findViewById(R.id.activity_apievent_select), getApplicationContext(), "Invalid key: "+input.getText().toString()+".", true, 0);
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

    @Override
    public void onItemClick(View v) {
        // Close the keyboard right away
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        Event event = adapter.getEvent(rv.getChildLayoutPosition(v));
        try {
            new ImportEvent().execute(event.key);
        } catch(NullPointerException e) {
            System.out.println("Failed to import event");
        }
    }

    private class SearchEvents extends AsyncTask<Void, Void, ArrayList<Event>> {

        private final String query;

        public SearchEvents(String query) {
            this.query = query.toLowerCase();
        }

        @Override
        protected ArrayList<Event> doInBackground(Void... params) {
            if(events == null || events.size() == 0) return null;
            if(active == null) active = new ArrayList<>();

            if(!query.equals("")) {
                active.clear();
                for(Event e : events) {
                    e.relevance = 0;
                    if(e.name.toLowerCase().equals(query)) e.relevance += 500;
                    if(e.name.toLowerCase().contains(query)) e.relevance += 200;
                    if(Text.contains(e.name.toLowerCase(), query)) e.relevance += 400;
                    if(e.start_date.toLowerCase().contains(query)) e.relevance += 200;
                    if(e.start_date.toLowerCase().equals(query)) e.relevance += 500;
                    if(Text.contains(e.start_date.toLowerCase(), query)) e.relevance += 400;
                    if(e.location.toLowerCase().equals(query)) e.relevance += 500;
                    if(e.location.toLowerCase().contains(query)) e.relevance += 200;
                    if(Text.contains(e.location.toLowerCase(), query)) e.relevance += 400;

                    if(e.relevance != 0) active.add(e);
                }
                Collections.sort(active);
                Collections.reverse(active);
                return active;
            } else {
                active.clear();
                for(Event e : events) e.relevance = 0;
                active.addAll(events);
            }

            if(active != null) Collections.sort(active);

            return active;
        }

        @Override
        public void onPostExecute(ArrayList<Event> result) {
            if(adapter != null) {
                adapter.removeAll();
                adapter.setEvents(result);
            }
        }
    }

    /**
     * Fetch events will load a list of events from TBA.com
     */
    private class FetchEvents extends AsyncTask<Integer, Void, Event[]> {

        private final boolean showEvents;

        public FetchEvents(boolean showEvents) {
            this.showEvents = showEvents;
            if(events == null) events = new ArrayList<>();

            rv.setVisibility(View.GONE);
            bar.setVisibility(View.VISIBLE);
            bar.getIndeterminateDrawable().setColorFilter(new Loader(getApplicationContext()).loadSettings().getRui().getAccent(), PorterDuff.Mode.MULTIPLY);
        }

        @Override
        protected Event[] doInBackground(Integer... year) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build(); StrictMode.setThreadPolicy(policy);

            Settings.disableAll();
            Event[] events = null;
            if(showEvents) events = new TBA().getEvents(year[0], false);
            else {
                try {
                    events = new TBA().getTeamEvents(new Loader(getApplicationContext()).loadSettings().getTeamNumber(), year[0], false);
                } catch (Exception e) {}
            }

            // Clean names and dates
            try {
                for(Event e : events) {
                    while(e.name.startsWith(" ")) e.name = e.name.substring(1);
                    e.start_date = Integer.parseInt(e.start_date.split("-")[1])+"/"+Integer.parseInt(e.start_date.split("-")[2])+"/"+e.start_date.split("-")[0];
                }
            } catch(Exception e) {
                System.err.println("An error occurred while trying to clean event names.");
            }


            return events;
        }

        @Override
        protected void onPostExecute(Event[] event) {
            events.clear();
            if(event != null) events.addAll(Arrays.asList(event));

            rv.setVisibility(View.VISIBLE);
            bar.setVisibility(View.GONE);
            new SearchEvents("").execute();
        }
    }

    private class ImportEvent extends AsyncTask<String, Void, Event> {
        private String key;

        protected Event doInBackground(String... key) {
            this.key = key[0];

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build(); StrictMode.setThreadPolicy(policy);

            Settings.defaults();
            return new TBA().getEvent(this.key);
        }

        @Override
        protected void onPostExecute(Event event) {
            if(event == null || event.name == null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Text.showSnackbar(findViewById(R.id.activity_apievent_select), getApplicationContext(), "No event found with key: "+key+".", true, 0);
                    }
                });
                return;
            }
            Intent result = new Intent();
            result.putExtra("event", event);
            setResult(Constants.EVENT_IMPORTED, result);
            finish();
        }
    }
}

