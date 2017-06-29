package com.cpjd.roblu.forms;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.ui.UIHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*******************************************************
 * Copyright (C) 2016 Will Davies wdavies973@gmail.com
 *
 * This file is part of Roblu
 *
 * Roblu cannot be distributed for a price or to people outside of your local robotics team.
 *******************************************************/

// Predefined update
public class Predefined extends AppCompatActivity implements OnItemClickListener {

	private final String items[] = { "FRC 2016", "FRC 2017"};

	private final String sub_items[] = { "Ramparts, walls, moat, cheval de frise, etc." , "Gears, hopper, load station, shots, etc."};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_predefined);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		setTitle("Predefined forms");
		ListView sharingView = (ListView) findViewById(R.id.listView1);

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

        new UIHandler(this, toolbar).update();
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(Constants.PREDEFINED_DISCARDED);
            finish();
            return true;
        }

        return false;
    }

    @Override
    public void onBackPressed() {

    }

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        RForm form = null;

        switch (position) {
            case 0:
                form = new PForms(2016).getForm();
                break;
            case 1:
                form = new PForms(2017).getForm();
                break;
        }
        if(form == null) return;

        Bundle bundle = new Bundle();
        Intent intent = new Intent();
        bundle.putSerializable("form", form);
        intent.putExtras(bundle);
        setResult(Constants.PREDEFINED_CONFIMRED, intent);
        finish();
	}
}
