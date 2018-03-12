package com.cpjd.roblu.ui.events;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.cpjd.models.Event;
import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.tba.UnpackTBAEvent;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.ui.dialogs.FastDialogBuilder;
import com.cpjd.roblu.ui.forms.FormViewer;
import com.cpjd.roblu.ui.forms.PredefinedFormSelector;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;

/**
 * EventEditor can be used for the manual creation or editing of an REvent object.
 * It's recommended that the user uses the TBA event important option instead.
 *
 * Parameters (in incoming Intent):
 * -"editing" - true to edit and event, false to create an event
 * -"key" - the tba key (available while not editing also, for the TBA import)
 * -"name" - the event name (available while not editing also, for the TBA import)
 * -"tbaEvent" - extra data that should be included when an event is created
 *
 * Also: requests from TBAEventSelector should use editing==true since the user is essentially just editing a couple things off data
 * that's already been downloaded
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class EventEditor extends AppCompatActivity {

    /**
     * This boolean represents which mode the EventEditor is in, if true, an already existing event
     * is being edited, if false, a new REvent is being created
     */
    private boolean editing;

    /**
     * The user is allowed to work on edited and form and go back to the event page without losing their progress,
     * EventEditor doesn't need access to the form, it just keeps a reference to it for it's children purposes
     */
    private RForm tempFormHolder;

    /**
     * UI element where the event name can be typed
     */
    private EditText eventName;
    /**
     * UI element where the user can manually change TheBlueAlliance event key
     */
    private EditText tbaKeyText;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * Load dependencies
		 */
        editing = getIntent().getBooleanExtra("editing", false);
        RUI rui = new IO(getApplicationContext()).loadSettings().getRui();

        // decide whether to use create event or edit event UI scheme
        if(editing) setContentView(R.layout.activity_edit_event);
        else setContentView(R.layout.activity_create_event);

        /*
         * Setup UI
         */
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.clear);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if(editing) getSupportActionBar().setTitle("Edit event");
            else getSupportActionBar().setTitle("Create event");
        }


        // event name
        eventName = findViewById(R.id.event_create_name_edit);

        /*
         * Bind user color preferences to the UI elements
         */
        Utils.setInputTextLayoutColor(rui.getAccent(), (TextInputLayout)findViewById(R.id.name_wrapper), (AppCompatEditText)findViewById(R.id.event_create_name_edit));

        /*
         * Setup editing/non-editing UI specifics
         */
        if(!editing) {
            TextView t = findViewById(R.id.event_create_form_label);
            t.setTextColor(rui.getAccent());
            if(getIntent().getSerializableExtra("tbaEvent") != null) {
                /*
                 * This item will be set if this activity is called form the TBAEventSelector activity, all it's saying is that
                 * all the data within this Event model should be included when creating the REvent
                 */
                Event event = (Event) getIntent().getSerializableExtra("tbaEvent");
                eventName.setText(event.name);
                findViewById(R.id.switch1).setVisibility(View.VISIBLE);
            }
        } else {
            RelativeLayout layout = findViewById(R.id.create_layout);
            for(int i = 0; i < layout.getChildCount(); i++) {
                if(layout.getChildAt(i).getId() == R.id.form_type || layout.getChildAt(i).getId() == R.id.event_create_form_label) {
                    layout.removeViewAt(i);
                    i = 0;
                }
            }
            tbaKeyText = findViewById(R.id.key_edit);
            tbaKeyText.setText(getIntent().getStringExtra("key"));
            eventName.setText(getIntent().getStringExtra("name"));
        }

        // General UI syncing
        new UIHandler(this, toolbar).update();
	}

    /**
     * User selected the confirm button (or home, but we don't care about the home button as much as the event confirm button)
     * @param item the UI item that was tapped
     * @return true if the event was consumed
     */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
		    launchParent();
		    return true;
        }
        // user clicked the confirm button
        else if(item.getItemId() == R.id.action_event_create_confirm) {
		    /*
		     * User was editing an event and selected confirm, return the data to the parent calling activity
		     * to be set to the REvent and saved
		     */
            if(editing) {
                Intent result = new Intent();
                result.putExtra("name", eventName.getText().toString());
                result.putExtra("key", tbaKeyText.getText().toString());
                setResult(Constants.EVENT_INFO_EDITED, result);
                finish();
            }
            /*
             * User was creating a new event and selected confirm, decide which form dialog needs to be launched
             */
            else {
                Spinner spinner = findViewById(R.id.form_type);

                /*
                 * User selected "Create custom form" option
                 */
                if(spinner.getSelectedItemPosition() == 0) {
                    Intent startView = new Intent(this, FormViewer.class);
                    // Package a form with any old data that the user was working on (might be null)
                    startView.putExtra("form", tempFormHolder);
                    startView.putExtra("ignoreDiscard", true);
                    startActivityForResult(startView, Constants.GENERAL);
                }
                /*
                 * User selected "Import predefined form" option
                 */
                else if(spinner.getSelectedItemPosition() == 1)  {
                    startActivityForResult(new Intent(this, PredefinedFormSelector.class), Constants.GENERAL);
                }
                /*
                 * User selected "Use master form" option
                 */
                else if(spinner.getSelectedItemPosition() == 2) {
                    /*
                     * Create the event
                     */
                    IO io = new IO(getApplicationContext());
                    RForm masterForm = io.loadSettings().getMaster();
                    createEvent(masterForm);
                }
                /*
                 * User selected "No form (create form later)" option
                 */
                else {
                    /*
                     * Create the event
                     */
                    createEvent(Utils.createEmpty());
                }

            }
        }
        return true;
	}

    /**
     * A result has been received from a child activity.
     * 1 of 3 things happened:
     * -User exited FormViewer activity
     * -User created a form successfully with FormViewer activity
     * -User selected a predefined form successfully
     * @param requestCode the code the child activity was started with
     * @param resultCode the code the child activity returned
     * @param data any data included with the return
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*
         * User tapped back on the custom form editor (essentially a discard, expect if they re-enter
         * the custom form editor, their data will be saved because this activity keeps track of it)
         */
        if(resultCode == Constants.CANCELLED) {
            if(data != null && data.getExtras() != null && data.getExtras().getSerializable("form") != null) tempFormHolder = (RForm) data.getExtras().getSerializable("form");
        }
        /*
         * User tapped confirm on the custom form editor, so let's create an event with form they created
         */
        else if(resultCode == Constants.FORM_CONFIRMED) {
            Bundle bundle = data.getExtras();
            tempFormHolder = (RForm) bundle.getSerializable("form");

            /*
             * Create the event!
             */
            createEvent(tempFormHolder);
        }
        /*
         * User tapped a predefined form, so let's create an event with the specified predefined form
         */
        else if(resultCode == Constants.PREDEFINED_FORM_SELECTED) {
            /*
             * Create the event!
             */
            tempFormHolder = (RForm) data.getExtras().getSerializable("form");
            createEvent(tempFormHolder);
        }
    }

    /**
     * This method is more of a courtesy method to the source code reader,
     * an event will get created from EventEditor whenever this method is called, no exceptions.
     * @param form the form to save with the event
     */
    private void createEvent(RForm form) {
        IO io = new IO(getApplicationContext());
        REvent event = new REvent(io.getNewEventID(), eventName.getText().toString());

        io.saveEvent(event); // we call this twice because the /event/ dir is required for the form to save
        io.saveForm(event.getID(), form);

        /*
         * We need to check if the user included any TBA information that we should include in this event creation
         */
        if(!editing && getIntent().getSerializableExtra("tbaEvent") != null) {
            ProgressDialog d = ProgressDialog.show(this, "Hold on tight!", "Generating team profiles from event...", true);
            d.setCancelable(false);
            event.setKey(((Event)getIntent().getSerializableExtra("tbaEvent")).key);
            io.saveEvent(event);
            UnpackTBAEvent unpackTBAEvent = new UnpackTBAEvent((Event)getIntent().getSerializableExtra("tbaEvent"), event.getID(), this, d);
            if(((Switch)findViewById(R.id.switch1)).isChecked()) unpackTBAEvent.setRandomize(true);
            unpackTBAEvent.execute();
        }

        /*
         * If we started the UnpackTask, then we have to wait to return, if not, return now
         */
        else {
            io.saveEvent(event);
            Intent result = new Intent();
            result.putExtra("eventID", event.getID());
            setResult(Constants.NEW_EVENT_CREATED, result);
            finish();
        }
    }

    /**
     * Launch parent asks for user confirmation before discarding any data they might have started on
     */
    private void launchParent() {
        // if this statement is true, the user didn't enter enough information for a "discard changes?" dialog to be required, so exit automatically
        if((getIntent().getSerializableExtra("tbaEvent") != null) || eventName.getText().toString().equals("")
                && (tempFormHolder == null || ((tempFormHolder.getPit() == null) || tempFormHolder.getPit().size() <= 2) && (tempFormHolder.getMatch() == null || tempFormHolder.getMatch().size() == 0))) {
            setResult(Constants.EVENT_DISCARDED);
            finish();
        } else {
            new FastDialogBuilder()
                    .setTitle("Discard changes?")
                    .setMessage("Really discard changes you've made to the event?")
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
                    }).build(EventEditor.this);
        }
    }

    @Override
    public void onBackPressed() {
        launchParent();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.event_create_actionbar, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }


}
