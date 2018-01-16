package com.cpjd.roblu.ui.forms;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RTextfield;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.ui.dialogs.FastDialogBuilder;
import com.cpjd.roblu.ui.events.EventDrawerManager;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.BottomBarTab;
import com.roughike.bottombar.OnTabSelectListener;

import java.util.ArrayList;

/**
 * Form viewer is the UI representation of the RForm model, it will display all the RMetrics contained in the form.
 *
 * Parameters:
 * -"form" - pass a form if an edit is requested
 * -"master" - signifies that the passed in form is also a master form
 * -"ignoreDiscard" - discard dialog won't be displayed (default: false)
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class FormViewer extends AppCompatActivity implements View.OnClickListener, OnTabSelectListener, FormRecyclerAdapter.MetricSelectedListener, EventDrawerManager.EventSelectListener {

    /**
     * The form that's being edited
     */
    private RForm form;
    /**
     * If true, FormViewer should display an "Are you sure?" dialog before exiting the view
     */
    private boolean changesMade;
    /**
     * The back-end to the metrics recycler view
     */
    private FormRecyclerAdapter metricsAdapter;
    /**
     * 0 if we're editing the PIT form, 1 if we're editing the MATCH form
     */
    private int currentTab;

    private RecyclerView rv;
    /**
     * Stores the user's UI preferences
     */
    private RUI rui;

    /**
     * If the user requests a metric edit and decides to discard it,
     * the old metric needs to be added without any modified attributes,
     * so keep an extra reference to it here.
     */
    private RMetric metricEditHolder;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_form);

		/*
		 * Load dependencies
		 */
        rui = new IO(getApplicationContext()).loadSettings().getRui();

		/*
		 * Setup UI
		 */
		// Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Form editor");
            if(getIntent().getBooleanExtra("master", false)) getSupportActionBar().setSubtitle("Master form");
        }

        // Bottom bar - selector that lets the user switch between PIT and MATCH forms
        BottomBar bBar = findViewById(R.id.bottomBar);
        bBar.setOnTabSelectListener(this);
        BottomBarTab tab = bBar.getTabAtPosition(0);
        BottomBarTab tab2 = bBar.getTabAtPosition(1);
        tab.setBarColorWhenSelected(rui.getPrimaryColor());
        tab2.setBarColorWhenSelected(rui.getPrimaryColor());
        bBar.selectTabAtPosition(0);

        // Add the "New metric" button
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);

        // Recycler view setup
        rv = findViewById(R.id.movie_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);

        metricsAdapter = new FormRecyclerAdapter(this, this);
        rv.setAdapter(metricsAdapter);

        // Gesture helper
        ItemTouchHelper.Callback callback = new FormRecyclerTouchHelper(metricsAdapter);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        /*
         * Check to see if we received a form from a different class or
         * if we need to create a new one
         */
        if(getIntent().getSerializableExtra("form") != null) {
            form = (RForm) getIntent().getSerializableExtra("form");
        } else {
            RTextfield name = new RTextfield(0, "Team name", false, true, "");
            RTextfield number = new RTextfield(1, "Team number", true, true, "");
            ArrayList<RMetric> pit = new ArrayList<>();
            pit.add(name);
            pit.add(number);
            form = new RForm(pit, new ArrayList<RMetric>());
        }

        loadViews(true, 0);

        new UIHandler(this, toolbar, fab).update();
	}

    /**
     * This method does several things:
     *
     * 1) First, retrieve elements from adapter and store them in a temporary array (either pit or match)
     * 2) Tell the adapter to remove all views
     * 3) Re-load the appropriate views into the adapter
     *
     * We have to handle these events differently based on the selected tab (and whether we're editing or creating form):
     * 1) Element edited
     * 2) Element created
     * 3) Element deleted
     *
     * We have to handle these events globally:
     * 1) Back button pressed (put everything into a RForm and return it as a result)
     * 2) Saved (put everything into a RForm and return)
     *
     * Tab 0 - pit
     * Tab 1 - match
     *
     * We're assuming that we're loading coming from the other tab
     *
     */
	private void loadViews(boolean init, int tab) {
        currentTab = tab;

        if(tab == 0) {
            if(!init) form.setMatch(metricsAdapter.getMetrics());
            metricsAdapter.setMetrics(form.getPit());
        } else {
            form.setPit(metricsAdapter.getMetrics());
            metricsAdapter.setMetrics(form.getMatch());
        }
    }

    /**
     * Called when the user selects either PIT or MATCH tab
     * @param tabId the tab ID
     */
    @Override
    public void onTabSelected(@IdRes int tabId) {
        if(metricsAdapter == null) return;
        if(tabId == R.id.tab_pit) {
            loadViews(false, 0);
        } else {
            loadViews(false, 1);
        }
    }

    /**
     * Inflates the menu, if master form, add the "import from event" menu button
     * @param menu the menu to inflate
     * @return true if the menu is created
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if(getIntent().getBooleanExtra("master", false)) inflater.inflate(R.menu.master_edit_form, menu);
        else inflater.inflate(R.menu.add_element, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }

    /**
     * Called when the user selects a menu option
     * @param item the menu item that was selected
     * @return true if the event is consumed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
         * User wants to exit the form editor
         */
        if(item.getItemId() == android.R.id.home) {
            launchParent();
            return true;
        }
        /*
         * User wants to save changes to the form
         */
        else if(item.getItemId() == R.id.add_element) {
            /*
             * Make sure to retrieve all metrics
             */
            if(currentTab == 0) form.setPit(metricsAdapter.getMetrics());
            else if(currentTab == 1) form.setMatch(metricsAdapter.getMetrics());
            Intent result = new Intent();
            result.putExtra("form", form);
            setResult(Constants.FORM_CONFIRMED, result);
            finish();
            return true;
        }
        if(item.getItemId() == R.id.import_from_event) {
            if(!Utils.launchEventPicker(this, this)) {
                Utils.showSnackbar(findViewById(R.id.edit_form_layout), getApplicationContext(), "No events found.", true, 0);
            }
            return true;
        }
        return false;
    }

    private void launchParent() {
        if(changesMade && !getIntent().getBooleanExtra("ignoreDiscard", false)) {
            new FastDialogBuilder()
                    .setTitle("Discard changes?")
                    .setMessage("Really discard changes you've made to this form?")
                    .setPositiveButtonText("Discard")
                    .setNegativeButtonText("Cancel")
                    .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                        @Override
                        public void accepted() {
                            setResult(Constants.CANCELLED);
                            finish();
                        }

                        @Override
                        public void denied() {}
                        @Override
                        public void neutral() {}
                    }).build(FormViewer.this);
        } else {
            // Make sure to include the form in the return transfer for EventEditor primarily, even on a form discard
            if(getIntent().getBooleanExtra("ignoreDiscard", false)) {
                /*
                 * Make sure to retrieve all metrics
                 */
                if(currentTab == 0) form.setPit(metricsAdapter.getMetrics());
                else if(currentTab == 1) form.setMatch(metricsAdapter.getMetrics());
                Intent intent = new Intent();
                intent.putExtra("form", form);
                setResult(Constants.CANCELLED, intent);
            } else {
                setResult(Constants.CANCELLED);
            }
            finish();
        }
    }

    /**
     * Receive information from a child activity
     * @param requestCode the request code the child activity was launched with
     * @param resultCode the result code of the child activity
     * @param data any data returned with the child activity
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*
         * User created a new metric
         */
        if(requestCode == Constants.NEW_METRIC_REQUEST && resultCode == Constants.METRIC_CONFIRMED) {
            changesMade = true;
            Bundle b = data.getExtras();
            RMetric metric = (RMetric) b.getSerializable("metric");
            metricsAdapter.addMetric(metric);
        }
        /*
         * User edited a metric
         */
        else if(requestCode == Constants.EDIT_METRIC_REQUEST && resultCode == Constants.METRIC_CONFIRMED) {
            changesMade = true;
            Bundle b = data.getExtras();
            RMetric metric = (RMetric) b.getSerializable("metric");
            metricsAdapter.reAdd(metric);
        }
        /*
         * User discarded a metric edit
         */
        else if(requestCode == Constants.EDIT_METRIC_REQUEST && resultCode == Constants.CANCELLED) {
            metricsAdapter.reAdd(metricEditHolder);
        }
    }

    /**
     * Also launch the metric editor if the user taps on the metric (not swiping)
     * @param v the view that was tapped
     */
    @Override
    public void metricSelected(View v) {
        // do nothing, turns out this was a pain in the butt
    }

    /**
     * User wants to edit a metric
     * @param metric the metric to edit
     */
    @Override
    public void metricEditRequested(RMetric metric) {
        metricEditHolder = metric;
        Intent intent = new Intent(this, MetricEditor.class);
        intent.putExtra("metric", metric);
        startActivityForResult(intent, Constants.EDIT_METRIC_REQUEST);
    }

    /**
     * User clicked the "add metric" button
     * @param view the add button
     */
    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, MetricEditor.class);
        startActivityForResult(intent, Constants.NEW_METRIC_REQUEST);
    }

    /**
     * User selected "import from Event", so merge the forms
     * @param event the event to import the form from
     */
    @Override
    public void eventSelected(REvent event) {
        form = new IO(getApplicationContext()).loadForm(event.getID());
        metricsAdapter.setMetrics(form.getMatch());
        onTabSelected(R.id.tab_pit);
        //bBar.getChildAt(0).setSelected(true);
    }

    @Override
    public void onBackPressed() {
        launchParent();
    }

}
