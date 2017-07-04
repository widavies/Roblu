package com.cpjd.roblu.tba;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.cpjd.main.Settings;
import com.cpjd.main.TBA;
import com.cpjd.models.Event;
import com.cpjd.roblu.R;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Text;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APIEventSelect extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {

    private MaterialSearchView searchView;

    private ListView sharingView;
    private Event[] events;
    private ArrayList<Event> active;
    private int selectedYear;
    private RelativeLayout layout;
    private RUI rui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apievent_select);
        layout = (RelativeLayout) findViewById(R.id.activity_apievent_select);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rui = new Loader(getApplicationContext()).loadSettings().getRui();

        setTitle("Select an event");

        searchView = (MaterialSearchView) findViewById(R.id.search_view);
        searchView.setHintTextColor(Color.BLACK);
        searchView.setHint("Search events");

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.getBackground().setColorFilter(rui.getText(), PorterDuff.Mode.SRC_ATOP);
        //ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.years_array, android.R.layout.simple_spinner_item);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.years_array));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        selectedYear = 2017;

        sharingView = (ListView) findViewById(R.id.listView1);
        sharingView.setOnItemClickListener(this);
        new FetchEvents().execute(selectedYear);
        new SearchEvents("").execute();

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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            new ImportEvent().execute(active.get(position).key);
        } catch(NullPointerException e) {
            System.out.println("Failed to import event");
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long l) {
        selectedYear = Integer.parseInt(parent.getItemAtPosition(position).toString());
        new FetchEvents().execute(selectedYear);
        new SearchEvents("").execute();
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
        if(dialog.getWindow() != null) dialog.getWindow().getAttributes().windowAnimations = rui.getDialogDirection();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
        dialog.show();
        dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(rui.getAccent());
        dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());
    }

    private class SearchEvents extends AsyncTask<Void, Void, ArrayList<Event>> {

        private final String query;

        public SearchEvents(String query) {
            this.query = query.toLowerCase();
        }

        @Override
        protected ArrayList<Event> doInBackground(Void... params) {
            if(events == null || events.length == 0) return null;

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
                active.addAll(Arrays.asList(events));
            }

            Collections.sort(Arrays.asList(events));
            return new ArrayList<>(Arrays.asList(events));
        }

        @Override
        public void onPostExecute(ArrayList<Event> events) {
            String[] items = new String[events.size()];
            String[] sub_items = new String[events.size()];

            for(int i = 0; i < events.size(); i++) {
                items[i] = events.get(i).name;
                sub_items[i] = events.get(i).start_date+", "+events.get(i).location;
            }

            final List<Map<String, String>> data = new ArrayList<>();
            for (int i = 0; i < items.length; i++) {
                Map<String, String> datum = new HashMap<>(2);
                datum.put("item", items[i]);
                datum.put("description", sub_items[i]);
                data.add(datum);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), data, android.R.layout.simple_list_item_2, new String[] { "item", "description" },
                            new int[] { android.R.id.text1, android.R.id.text2 }) {
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                            text1.setTextColor(rui.getText());
                            text1 = (TextView) view.findViewById(android.R.id.text2);
                            text1.setTextColor(rui.getText());
                            return view;
                        }
                    };
                    sharingView.setAdapter(adapter);

                }
            });
        }
    }

    private class FetchEvents extends AsyncTask<Integer, Void, Event[]> {
        @Override
        protected Event[] doInBackground(Integer... year) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build(); StrictMode.setThreadPolicy(policy);

            Settings.disableAll();
            Event[] events = new TBA().getEvents(2017, false);
            return events;
        }

        @Override
        protected void onPostExecute(Event[] event) {
            events = event;

            if(events == null) {
                new FetchEvents().execute(selectedYear);
                return;
            }
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
                        Text.showSnackbar(layout, getApplicationContext(), "No event found with key: "+key+".", true, 0);
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

