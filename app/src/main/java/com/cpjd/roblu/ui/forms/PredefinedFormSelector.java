package com.cpjd.roblu.ui.forms;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.RForm;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Allows the user to selected a predefined form that they want to use.
 *
 * Predefined forms are loaded from /assets/predefinedForms.
 * Here's the file import requirements (this is what should be typed on each line, ALWAYS SPECIFY ALL PARAMETERS, INCLUDED DEFAULTS):
 *
 * -boolean,title,defaultValue
 * -counter,title,increment,defaultValue,
 * -slider,title,min,max,increment,defaultValue
 * -chooser,title,option1:option2:option3,defaultSelectedIndex
 * -checkbox,title,option1:option2:option3
 * -stopwatch,title,defaultValue
 * -textfield,title,defaultValue
 * -gallery,title
 * -DEFAULT (add team name and team number).
 *
 * Separate the file like this:
 * PIT
 * DEFAULTS
 * metric1...
 * metric2...
 * MATCH
 * metric1...
 * metric2...
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class PredefinedFormSelector extends AppCompatActivity implements OnItemClickListener {

	private final String items[] = { "FRC 2016", "FRC 2017"};

	private final String sub_items[] = { "Ramparts, walls, moat, cheval de frise, etc." , "Gears, hopper, load station, shots, etc."};

	private HashMap<Integer, RForm> forms;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_predefined);

        /*
         * Setup UI
         */
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Predefined forms");
        }
        // Bind predefined forms to the list
		ListView sharingView = findViewById(R.id.listView1);
		List<Map<String, String>> data = new ArrayList<>();
		for (int i = 0; i < items.length; i++) {
			Map<String, String> datum = new HashMap<>(2);
			datum.put("item", items[i]);
			datum.put("description", sub_items[i]);
			data.add(datum);
		}
		SimpleAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2, new String[] { "item", "description" },
				new int[] { android.R.id.text1, android.R.id.text2 });
		sharingView.setAdapter(adapter);
		sharingView.setOnItemClickListener(this);

		/*
		 * Load predefined forms
		 */
		forms = new HashMap<>();
        forms.put(2016, processForm(2016));
        forms.put(2017, processForm(2017));

        // Sync UI with user settings
        new UIHandler(this, toolbar).update();
	}

	private RForm processForm(int year) {
	    RForm form = new RForm(null, null);
        ArrayList<RMetric> metrics = new ArrayList<>();
	    try {
            AssetManager am = getAssets();
            InputStream is = am.open("predefinedForms"+ File.separator+year +".txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            int ID = 0;
            while((line = br.readLine()) != null) {
                if(line.equals("PIT")) {
                    continue;
                }
                else if(line.equals("MATCH")) {
                   form.setPit((ArrayList<RMetric>)metrics.clone());
                   metrics.clear();
                   continue;
                }
                else if(line.equals("DEFAULTS")) {
                    metrics.add(new RTextfield(0, "Team name", false, true, ""));
                    metrics.add(new RTextfield(1, "Team number", true, true, ""));
                    ID = 2;
                }

                /*
                 * Process file
                 */
                String[] tokens = line.split(",");
                if(tokens[0].equals("counter")) {
                    metrics.add(new RCounter(ID, tokens[1], Integer.parseInt(tokens[3]), Integer.parseInt(tokens[2])));
                    ID++;
                }
                else if(tokens[0].equals("chooser")) {
                    metrics.add(new RChooser(ID, tokens[1], tokens[2].split(":"), Integer.parseInt(tokens[3])));
                    ID++;
                }
                else if(tokens[0].equals("slider")) {
                    metrics.add(new RSlider(ID, tokens[1], Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]), Integer.parseInt(tokens[4])));
                    ID++;
                }
                else if(tokens[0].equals("checkbox")) {
                    LinkedHashMap<String, Boolean> temp = new LinkedHashMap<>();
                    for(String s : tokens[2].split(":")) temp.put(s, false);
                    metrics.add(new RCheckbox(ID, tokens[1], temp));
                    ID++;
                }
                else if(tokens[0].equals("textfield")) {
                    metrics.add(new RTextfield(ID, tokens[1], ""));
                    ID++;
                }
                else if(tokens[0].equals("stopwatch")) {
                    metrics.add(new RStopwatch(0, tokens[1], Double.parseDouble(tokens[2])));
                    ID++;
                }
                else if(tokens[0].equals("boolean")) {
                    metrics.add(new RBoolean(0, tokens[1], Boolean.parseBoolean(tokens[2])));
                    ID++;
                }
                else if(tokens[0].equals("gallery")) {
                    metrics.add(new RGallery(0, tokens[1]));
                    ID++;
                }
            }
            form.setMatch(metrics);
            return form;

        } catch(IOException e) {
	        Log.d("RBS", "Failed to process form: "+e.getMessage());
            return null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(Constants.CANCELLED);
            finish();
            return true;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        setResult(Constants.PREDEFINED_FORM_SELECTED);
        finish();
    }

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        RForm form = null;

        switch (position) {
            case 0:
                form = forms.get(2016);
                break;
            case 1:
                form = forms.get(2017);
                break;
        }
        if(form == null) return;

        Bundle bundle = new Bundle();
        Intent intent = new Intent();
        bundle.putSerializable("form", form);
        intent.putExtras(bundle);
        setResult(Constants.PREDEFINED_FORM_SELECTED, intent);
        finish();
	}
}
