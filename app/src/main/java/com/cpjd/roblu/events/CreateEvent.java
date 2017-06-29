package com.cpjd.roblu.events;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.forms.EditForm;
import com.cpjd.roblu.forms.Predefined;
import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Text;

import java.util.ArrayList;
import java.util.Calendar;

/*******************************************************
 * Copyright (C) 2016 Will Davies wdavies973@gmail.com
 *
 * This file is part of Roblu
 *
 * Roblu cannot be distributed for a price or to people outside of your local robotics team.
 *******************************************************/

public class CreateEvent extends AppCompatActivity implements OnClickListener {

    private boolean editing;

    private ArrayList<Element> tempPit, tempMatch;

	// Event traits
    private EditText eventName;
    private EditText start, end;
    private int startYear, startMonth, startDay, endYear, endMonth, endDay;

    private boolean calendarModified;

    private RelativeLayout layout;

    private EditText keyText;
    private RUI rui;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        editing = getIntent().getBooleanExtra("editing", false);

        if(editing) setContentView(R.layout.activity_edit_event);
        else setContentView(R.layout.activity_create_event);

        layout = (RelativeLayout) findViewById(R.id.create_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(editing) setTitle("Edit event");
        else setTitle("Create event");

		Calendar currentDate = Calendar.getInstance();
		startYear = currentDate.get(Calendar.YEAR);
		startMonth = currentDate.get(Calendar.MONTH);
	    startDay = currentDate.get(Calendar.DAY_OF_MONTH);

        endYear = startYear; endMonth = startMonth; endDay = startDay;

        rui = new Loader(getApplicationContext()).loadSettings().getRui();

        start = (EditText) findViewById(R.id.editText2);
        end = (EditText) findViewById(R.id.editText1);
        if(!editing) {
            TextView t = (TextView) findViewById(R.id.event_create_form_label);
            t.setTextColor(rui.getAccent());
        }
		Calendar cp = Calendar.getInstance();
		String date = Text.getDay(cp.get(Calendar.DAY_OF_WEEK))+", "+Text.getMonth(cp.get(Calendar.MONTH))+" "+cp.get(Calendar.DAY_OF_MONTH) + ", " + cp.get(Calendar.YEAR);
		start.setText(date); end.setText(date);
		start.setFocusable(false); end.setFocusable(false);
		start.setOnClickListener(this); end.setOnClickListener(this);

        tempPit = new ArrayList<>(); tempMatch = new ArrayList<>();
        eventName = (EditText) findViewById(R.id.event_create_name_edit);


        Text.setInputTextLayoutColor(rui.getAccent(), rui.getText(), (TextInputLayout)findViewById(R.id.name_wrapper), (AppCompatEditText)findViewById(R.id.event_create_name_edit));
        Text.setInputTextLayoutColor(rui.getAccent(), rui.getText(), (TextInputLayout)findViewById(R.id.start_wrapper), (AppCompatEditText)findViewById(R.id.editText1));
        Text.setInputTextLayoutColor(rui.getAccent(), rui.getText(), (TextInputLayout)findViewById(R.id.end_wrapper), (AppCompatEditText)findViewById(R.id.editText2));

        if(editing) {
            RelativeLayout layout = (RelativeLayout) findViewById(R.id.create_layout);
            for(int i = 0; i < layout.getChildCount(); i++) {
                if(layout.getChildAt(i).getId() == R.id.form_type || layout.getChildAt(i).getId() == R.id.event_create_form_label) {
                    layout.removeViewAt(i);
                    i = 0;
                }
            }
            keyText = (EditText) findViewById(R.id.key_edit);
            keyText.setText(getIntent().getStringExtra("key"));
            eventName.setText(getIntent().getStringExtra("name"));
            Calendar temp = Calendar.getInstance();
            temp.setTimeInMillis(getIntent().getLongExtra("start", System.currentTimeMillis()));
            date = Text.getDay(temp.get(Calendar.DAY_OF_WEEK))+", "+Text.getMonth(temp.get(Calendar.MONTH))+" "+temp.get(Calendar.DAY_OF_MONTH) + ", " + temp.get(Calendar.YEAR);
            startYear = temp.get(Calendar.YEAR);
            startMonth = temp.get(Calendar.MONTH);
            startDay = temp.get(Calendar.DAY_OF_MONTH);
            start.setText(date);
            temp.setTimeInMillis(getIntent().getLongExtra("end", System.currentTimeMillis()));
            date = Text.getDay(temp.get(Calendar.DAY_OF_WEEK))+", "+Text.getMonth(temp.get(Calendar.MONTH))+" "+temp.get(Calendar.DAY_OF_MONTH) + ", " + temp.get(Calendar.YEAR);
            endYear = temp.get(Calendar.YEAR);
            endMonth = temp.get(Calendar.MONTH);
            endDay = temp.get(Calendar.DAY_OF_MONTH);

            end.setText(date);
        }

        new UIHandler(this, toolbar).update();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.event_create_actionbar, menu);
        new UIHandler(this, menu).updateMenu();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if(item.getItemId() == android.R.id.home) launchParent();

		else if(item.getItemId() == R.id.action_event_create_confirm) {
                if(getStartTime() > getEndTime()) {
                    View view = this.getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    Text.showSnackbar(layout, getApplicationContext(), "Event must start before it ends", true, rui.getPrimaryColor());
                    return false;
                }

                if(editing) {
                    Intent result = new Intent();
                    result.putExtra("name", eventName.getText().toString());
                    result.putExtra("start", getStartTime());
                    result.putExtra("end", getEndTime());
                    result.putExtra("key", keyText.getText().toString());
                    setResult(Constants.EVENT_INFO_EDITED, result);
                    finish();
                    return true;
                }

                Spinner spinner = (Spinner) findViewById(R.id.form_type);

                if(spinner.getSelectedItemPosition() == 0) { // Create form
                    Intent startView = new Intent(this, EditForm.class);
                    if(tempMatch.size() == 0 && tempPit.size() == 0) startView.putExtra("form", (String)null);
                    else {
                        startView.putExtra("form", new RForm(tempPit, tempMatch));
                    }
                    startActivityForResult(startView, Constants.GENERAL);
                }
                else if(spinner.getSelectedItemPosition() == 1) startActivityForResult(new Intent(this, Predefined.class), Constants.GENERAL);
                else if(spinner.getSelectedItemPosition() == 2) {
                    RSettings settings = new Loader(getApplicationContext()).loadSettings();
                    RForm form = settings.getMaster();
                    if(form == null) {
                        form = Text.createEmpty();
                        settings.setMaster(form);
                        new Loader(getApplicationContext()).saveSettings(settings);
                    }
                    REvent event = new REvent(eventName.getText().toString(), getStartTime(), getEndTime(),
                            new Loader(getApplicationContext()).getNewEventID());
                    new Loader(getApplicationContext()).saveEvent(event);
                    new Loader(getApplicationContext()).saveForm(form, event.getID());
                    setResult(Constants.MANUAL_CREATED);
                    Intent intent = new Intent();
                    intent.putExtra("eventID", event.getID());
                    setResult(Constants.MANUAL_CREATED, intent);
                    finish();
                } else {
                    REvent event = new REvent(eventName.getText().toString(), getStartTime(), getEndTime(),
                            new Loader(getApplicationContext()).getNewEventID());
                    new Loader(getApplicationContext()).saveEvent(event);
                    new Loader(getApplicationContext()).saveForm(Text.createEmpty(), event.getID());
                    Intent intent = new Intent();
                    intent.putExtra("eventID", event.getID());
                    setResult(Constants.MANUAL_CREATED, intent);
                    finish();
                }

			}
		return true;
	}

	@Override
	public void onClick(View v) {
        if(v.getId() == R.id.editText2) {
            final DatePickerDialog startPicker = new DatePickerDialog(CreateEvent.this, new OnDateSetListener() {
                public void onDateSet(DatePicker datepicker, int selectedyear, int selectedmonth, int selectedday) {
                    startYear = selectedyear;
                    startMonth = selectedmonth;
                    startDay = selectedday;
                    Calendar date = Calendar.getInstance();
                    date.clear();
                    date.set(selectedyear, selectedmonth, selectedday);
                    String dateStr = Text.getDay(date.get(Calendar.DAY_OF_WEEK)) + ", " + Text.getMonth(date.get(Calendar.MONTH)) + " " + date.get(Calendar.DAY_OF_MONTH) + ", " + date.get(Calendar.YEAR);
                    start.setText(dateStr);
                    calendarModified = true;
                }
            }, startYear, startMonth, startDay);
            startPicker.setTitle("Select date");
            startPicker.show();
        } else {
            final DatePickerDialog endPicker = new DatePickerDialog(CreateEvent.this, new OnDateSetListener() {
                public void onDateSet(DatePicker datepicker, int selectedyear, int selectedmonth, int selectedday) {
                    endYear = selectedyear;
                    endMonth = selectedmonth;
                    endDay = selectedday;
                    Calendar date = Calendar.getInstance();
                    date.clear();
                    date.set(selectedyear, selectedmonth, selectedday);
                    String dateStr = Text.getDay(date.get(Calendar.DAY_OF_WEEK)) + ", " + Text.getMonth(date.get(Calendar.MONTH)) + " " + date.get(Calendar.DAY_OF_MONTH) + ", " + date.get(Calendar.YEAR);
                    end.setText(dateStr);
                    calendarModified = true;
                }
            }, endYear, endMonth, endDay);
            endPicker.setTitle("Select date");
            endPicker.show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Constants.FORM_DISCARDED) { // user tapped back on manual edit form
            Bundle bundle = data.getExtras();
            tempPit = (ArrayList<Element>) bundle.getSerializable("tempPit");
            tempMatch = (ArrayList<Element>) bundle.getSerializable("tempMatch");
        }
        else if(resultCode == Constants.FORM_CONFIMRED) { // user tapped confirm on manual form
            Bundle bundle = data.getExtras();
            tempPit = (ArrayList<Element>) bundle.getSerializable("tempPit");
            tempMatch = (ArrayList<Element>) bundle.getSerializable("tempMatch");

            REvent event = new REvent(eventName.getText().toString(), getStartTime(), getEndTime(),
                    new Loader(getApplicationContext()).getNewEventID());
            RForm form = new RForm(tempPit, tempMatch);
            Loader loader = new Loader(getApplicationContext());
            loader.saveEvent(event);
            loader.saveForm(form, event.getID());
            Intent intent = new Intent();
            intent.putExtra("eventID", event.getID());
            setResult(Constants.MANUAL_CREATED, intent);
            finish();
        }
        else if(resultCode == Constants.PREDEFINED_CONFIMRED) {
            REvent event = new REvent(eventName.getText().toString(), getStartTime(), getEndTime(),
                    new Loader(getApplicationContext()).getNewEventID());
            RForm form = (RForm) data.getSerializableExtra("form");
            Loader loader = new Loader(getApplicationContext());
            loader.saveEvent(event);
            loader.saveForm(form, event.getID());
            Intent intent = new Intent();
            intent.putExtra("eventID", event.getID());
            setResult(Constants.MANUAL_CREATED, intent);
            finish();
        }
    }

    private void launchParent() {
        if((eventName.getText().toString().equals("") && (tempMatch.size() == 0 && tempPit.size() <= 2) && !calendarModified) || editing) {
            setResult(Constants.MANUAL_DISCARDED);
            finish();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Discard changes?");
        builder.setMessage("Really discard changes you've made to the event?");

        builder.setPositiveButton("Discard", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                setResult(Constants.MANUAL_DISCARDED);
                finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        if(dialog.getWindow() != null) dialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
        dialog.show();
    }

    private long getEndTime() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, endYear);
        c.set(Calendar.MONTH, endMonth);
        c.set(Calendar.DAY_OF_MONTH, endDay);
        return c.getTimeInMillis();
    }

    private long getStartTime() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, startYear);
        c.set(Calendar.MONTH, startMonth);
        c.set(Calendar.DAY_OF_MONTH, startDay);
        return c.getTimeInMillis();
    }
    @Override
    public void onBackPressed() {
        launchParent();
    }
}
