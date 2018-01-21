package com.cpjd.roblu.ui.forms;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.models.metrics.RBoolean;
import com.cpjd.roblu.models.metrics.RCheckbox;
import com.cpjd.roblu.models.metrics.RChooser;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RSlider;
import com.cpjd.roblu.models.metrics.RStopwatch;
import com.cpjd.roblu.models.metrics.RTextfield;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;

import java.util.LinkedHashMap;

/**
 * MetricEditor is a neat utility that manages the creation and editing of metrics.
 * It will assist in showing a metric preview and adjusting metric settings.
 *
 * Two specific things happen depending on the metric:
 * -The metric is loaded into the Toolbar
 * -The metric settings are loaded below the toolbar
 *
 * There are 5 possible text configs: name, min, max, increment, comma-list
 *
 * Parameters:
 * -"metric" - a metric instance that should be edited, this will be returned in the Intent return upon confirmed editing!
 *
 */
public class MetricEditor extends AppCompatActivity implements AdapterView.OnItemSelectedListener, RMetricToUI.MetricListener {
    /**
     * This is the metric that the user is currently working on, if we're editing the metric,
     * then this will receive some initial data
     */
    private RMetric metric;
    /**
     * Maps an RMetric instance to a UI card
     */
    private RMetricToUI rMetricToUI;
    /**
     * The layout below the toolbar that metric settings UI cards should be added to
     */
    private LinearLayout layout;
    /**
     * All the different metric types that the user can select from
     */
    private final static String[] METRIC_TYPES = {"Boolean", "Counter", "Slider", "Chooser", "Checkbox", "Stopwatch", "Textfield", "Gallery"};
    /**
     * The user's color preferences, so the metrics can be synced with the user's preferences
     */
    private RUI rui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_element);

        /*
         * Load dependencies
         */
        rui = new IO(getApplicationContext()).loadSettings().getRui();

        /*
         * Setup UI
         */
        // Toolbar
        Toolbar toolbar = findViewById(R.id.support_toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.clear);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add metric");
        }
        // layout
        layout = findViewById(R.id.add_element_layout);
        // RMetric to UI
        rMetricToUI = new RMetricToUI(this, rui, true);
        rMetricToUI.setListener(this);

        /*
         * Check if the user actually wants to edit
         */
        if(getIntent().getSerializableExtra("metric") != null) {
            metric = (RMetric) getIntent().getSerializableExtra("metric");
            if(getSupportActionBar() != null) getSupportActionBar().setTitle("Edit metric");
            /*
             * The modified variable DOES NOT matter in the form, so always, always make sure it's true
             * so N.O. tags don't show up
             */
            metric.setModified(true);
            addMetricPreviewToToolbar();
            buildConfigLayout();

        } else {
            // add a RBoolean type, it's the default loaded type until the user selects a different one
            metric = new RBoolean(0, "Boolean", false);
            /*
             * The modified variable DOES NOT matter in the form, so always, always make sure it's true
             * so N.O. tags don't show up
             */
            metric.setModified(true);
            layout.addView(initMetricSelector());
        }

        // Sync UI with color preferences
        new UIHandler(this, toolbar).update();
    }

    /**
     * This method adds the metric type selector (where the user can choose which metric
     * he or she wants to create) below the toolbar.
     * @return the CardView to add to the layout
     */
    private CardView initMetricSelector() {
        RelativeLayout layout = new RelativeLayout(this);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);

        TextView elements = new TextView(this);
        elements.setTextColor(rui.getText());
        elements.setId(Utils.generateViewId());
        elements.setText(R.string.metric_type);
        elements.setLayoutParams(params);
        layout.addView(elements);

        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        Spinner types = new Spinner(this);
        types.setId(Utils.generateViewId());
        types.setSelection(0);
        types.getBackground().setColorFilter(rui.getText(), PorterDuff.Mode.SRC_ATOP);
        types.setOnItemSelectedListener(this);
        types.setPadding(Utils.DPToPX(this, 15), types.getPaddingTop(), types.getPaddingRight(), types.getPaddingBottom());
        types.setLayoutParams(params);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, METRIC_TYPES);
        types.setAdapter(adapter);
        layout.addView(types);

        CardView card = new CardView(this);
        card.setRadius(0);
        card.setContentPadding(Utils.DPToPX(this, 10), Utils.DPToPX(this, 10), Utils.DPToPX(this, 10), Utils.DPToPX(this, 10));
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(rui.getCardColor());
        card.addView(layout);

        return card;
    }

    /**
     * Adds the config text fields below the metric preview
     */
    private void buildConfigLayout() {
        // clear old config items (don't clear toolbar, preview metric, or metric type selector)
        for(int i = 3; i < this.layout.getChildCount(); i++) {
            this.layout.removeViewAt(i);
        }

        RelativeLayout layout = new RelativeLayout(this);
        layout.addView(getConfigField("Title", layout, 0));

        if(metric instanceof RCheckbox || metric instanceof RChooser) {
            layout.addView(getConfigField("Comma separated list", layout, 1));
        } else if(metric instanceof RCounter) {
            layout.addView(getConfigField("Increment", layout, 1));
        } else if(metric instanceof RSlider) {
            layout.addView(getConfigField("Minimum", layout, 1));
            layout.addView(getConfigField("Maximum", layout, 2));
        }

        this.layout.addView(getCardView(layout));
    }

    /**
     * Generates a configuration field for the layout with the specified settings.
     * The config field will detect the instance type of RMetric metric above and decide
     * what to update. Acceptable names:
     * -"title"
     * -"min"
     * -"max"
     * -"increment"
     * -"comma"
     */
    private TextInputLayout getConfigField(final String name, RelativeLayout layout, int position) {
        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint(name);
        inputLayout.setId(Utils.generateViewId());
        AppCompatEditText nameInput = new  AppCompatEditText(this);
        Utils.setInputTextLayoutColor(rui.getAccent(), inputLayout, nameInput);
        nameInput.setTextColor(rui.getText());
        nameInput.setHighlightColor(rui.getAccent());
        nameInput.setId(Utils.generateViewId());
        if(name.equalsIgnoreCase("minimum")) {
            nameInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            if(metric instanceof RSlider) nameInput.setText(String.valueOf(((RSlider)metric).getMin()));
        }
        else if(name.equalsIgnoreCase("maximum")) {
            nameInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            if(metric instanceof RSlider) nameInput.setText(String.valueOf(((RSlider)metric).getMax()));
        }
        else if(name.equalsIgnoreCase("increment")) {
            nameInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            if(metric instanceof RCounter) nameInput.setText(String.valueOf(((RCounter)metric).getIncrement()));
        }
        else if(name.startsWith("Comma separated list")) {
            if(metric instanceof RCheckbox) {
                StringBuilder text = new StringBuilder();
                RCheckbox checkbox = ((RCheckbox)metric);
                if(checkbox.getValues() != null) {
                    for(Object o : checkbox.getValues().keySet()) {
                        text.append(o).append(",");
                    }
                }
                if(text.toString().length() > 0) nameInput.setText(text.toString().substring(0, text.toString().length() - 1));
            } else if(metric instanceof RChooser) {
                StringBuilder text = new StringBuilder();
                RChooser chooser = ((RChooser)metric);
                if(chooser.getValues() != null) {
                    for(String s : chooser.getValues()) text.append(s).append(",");
                }
                if(text.toString().length() > 0) nameInput.setText(text.toString().substring(0, text.toString().length() - 1));
            }
        }
        else nameInput.setText(metric.getTitle());
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(name.equalsIgnoreCase("title")) {
                    metric.setTitle(charSequence.toString());
                } else if(name.equalsIgnoreCase("minimum") && metric instanceof RSlider) {
                    ((RSlider)metric).setMin((int)processTextAsNumber(charSequence, 0));
                } else if(name.equalsIgnoreCase("maximum") && metric instanceof RSlider) {
                    ((RSlider)metric).setMax((int)processTextAsNumber(charSequence, 100));
                } else if(name.equalsIgnoreCase("increment") && metric instanceof RCounter) {
                    ((RCounter)metric).setIncrement(processTextAsNumber(charSequence, 1));
                } else if(name.startsWith("Comma")) {
                    if(metric instanceof RCheckbox) {
                        String[] tokens = charSequence.toString().split(",");
                        LinkedHashMap<String, Boolean> hash = new LinkedHashMap<>();
                        for(String s : tokens) hash.put(s, false);
                        ((RCheckbox)metric).setValues(hash);
                    } else if(metric instanceof RChooser) {
                        String[] tokens = charSequence.toString().split(",");
                        ((RChooser)metric).setValues(tokens);
                    }
                }

                addMetricPreviewToToolbar();
                /*Toolbar tl = findViewById(R.id.toolbar);
                ViewGroup view = (ViewGroup) tl.getChildAt(0);
                RelativeLayout t = (RelativeLayout) view.getChildAt(0);
                TextView text = (TextView) t.getChildAt(0);
                text.setText(charSequence);
                metric.setTitle(charSequence.toString());*/
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        if(position > 0) params.addRule(RelativeLayout.BELOW, layout.getChildAt(position - 1).getId());
        inputLayout.setLayoutParams(params);
        inputLayout.addView(nameInput);
        inputLayout.requestFocus();
        return inputLayout;
    }

    /**
     * Adds the metric preview to the Toolbar
     */
    private void addMetricPreviewToToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(rui.getPrimaryColor());
        toolbar.removeAllViews();

        if(metric instanceof RBoolean) toolbar.addView(rMetricToUI.getBoolean((RBoolean)metric));
        else if(metric instanceof RCounter) toolbar.addView(rMetricToUI.getCounter((RCounter) metric));
        else if(metric instanceof RSlider) toolbar.addView(rMetricToUI.getSlider((RSlider) metric));
        else if(metric instanceof RChooser) toolbar.addView(rMetricToUI.getChooser((RChooser) metric));
        else if(metric instanceof RCheckbox) toolbar.addView(rMetricToUI.getCheckbox((RCheckbox) metric));
        else if(metric instanceof RStopwatch) toolbar.addView(rMetricToUI.getStopwatch((RStopwatch) metric));
        else if(metric instanceof RTextfield) toolbar.addView(rMetricToUI.getTextfield((RTextfield) metric));
        else if(metric instanceof RGallery) toolbar.addView(rMetricToUI.getGallery(true, 0, 0, ((RGallery)metric)));
    }

    /**
     * Called when the user selects one of the menu options
     * @param item the menu option that was selected
     * @return true if the event was consumed
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
         * User wants to return home, so pass don't return anything
         */
        if(item.getItemId() == android.R.id.home) {
            setResult(Constants.CANCELLED);
            finish();
            return true;
        }
        /*
         * User decided to save changes, return the metric
         */
        else if(item.getItemId() == R.id.add_element) {
            /*
             * Prevent some errors by making sure some metrics can't be exported with a null value set
             */
            if(metric instanceof RCheckbox) {
                if(((RCheckbox) metric).getValues() == null || ((RCheckbox) metric).getValues().size() == 0) {
                    Utils.showSnackbar(findViewById(R.id.add_element_layout), getApplicationContext(), "Can't create checkbox, no values defined.", true, 0);
                    return true;
                }
            }
            else if(metric instanceof RChooser) {
                if(((RChooser) metric).getValues() == null || ((RChooser) metric).getValues().length == 0) {
                    Utils.showSnackbar(findViewById(R.id.add_element_layout), getApplicationContext(), "Can't create chooser, no values defined.", true, 0);
                    return true;
                }
            }

            Intent result = new Intent();
            metric.setModified(false);
            result.putExtra("metric", metric);
            setResult(Constants.METRIC_CONFIRMED, result);
            finish();
            return true;
        }
        return false;
    }

    /**
     * Called when the user selects a metric type
     * @param adapterView the adapter containing all the choices
     * @param view the view that was tapped
     * @param i the position of the view
     * @param l id of the view
     */
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        ((TextView) adapterView.getChildAt(0)).setTextColor(rui.getText());
        /*
         * User selected a new metric, let's create it
         */
        String stringOfSelected = METRIC_TYPES[i];

        if(stringOfSelected.equals(METRIC_TYPES[0])) {
            metric = new RBoolean(0, "Boolean", false);
        } else if(stringOfSelected.equals(METRIC_TYPES[1])) {
            metric = new RCounter(0, "Counter", 1, 0);
        } else if(stringOfSelected.equals(METRIC_TYPES[2])) {
            metric = new RSlider(0, "Slider", 0, 100, 0);
        } else if(stringOfSelected.equals(METRIC_TYPES[3])) {
            metric = new RChooser(0, "Chooser", null, 0);
        } else if(stringOfSelected.equals(METRIC_TYPES[4])) {
            metric = new RCheckbox(0, "Checkbox", null);
        } else if(stringOfSelected.equals(METRIC_TYPES[5])) {
            metric = new RStopwatch(0, "Stopwatch", 0);
        } else if(stringOfSelected.equals(METRIC_TYPES[6])) {
            metric = new RTextfield(0, "Text field", "");
        } else if(stringOfSelected.equals(METRIC_TYPES[7])) {
            metric = new RGallery(0, "Gallery");
        }
        metric.setModified(true);
        addMetricPreviewToToolbar();
        buildConfigLayout();
    }

    /**
     * Generates a CardView from the layout
     * @param layout the layout to contain in the CardView
     * @return the CardView element
     */
    private CardView getCardView(View layout) {
        CardView card = new CardView(getApplicationContext());
        card.setBackgroundColor(rui.getCardColor());
        card.setRadius(rui.getFormRadius());
        card.setContentPadding(Utils.DPToPX(this, 8), Utils.DPToPX(this, 8), Utils.DPToPX(this, 8), Utils.DPToPX(this, 8));
        card.setUseCompatPadding(true);
        card.addView(layout);
        return card;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_element, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }

    /**
     * Refresh the toolbar to match the metric
     */
    @Override
    public void changeMade(RMetric metric) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    /**
     * Prevents the user from causing an integer overflow error by typing in a number that's too long
     * @param chars text input to process
     * @param defaultNumber the number to return if the CharSequence can't be processed
     * @return number, if overflowed, returns default
     */
    private double processTextAsNumber(CharSequence chars, int defaultNumber) {
        try {
            if(chars.equals("")) return defaultNumber;
            return Double.parseDouble(chars.toString());
        } catch(NumberFormatException e) {
            return defaultNumber;
        }
    }

}
