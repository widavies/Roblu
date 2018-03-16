package com.cpjd.roblu.ui.events;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RBackup;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.sync.cloud.EventDepacker;
import com.cpjd.roblu.sync.cloud.Service;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.ui.dialogs.FastDialogBuilder;
import com.cpjd.roblu.ui.tba.TBAEventSelector;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class shows a list of different ways an event can be created.
 * It will also handle the importing of a backup file and help with TBA imports
 *
 * Note: If the TBA import is selected, we'll outsource the work to the TBAEventSelector, which
 * will return an Event reference that we can then generate an REvent, RTeams, and RMatches from
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class EventCreateMethodPicker extends AppCompatActivity implements AdapterView.OnItemClickListener, EventDepacker.EventDepackerListener {

    /*
     * Items on the list and their descriptions
     */
    private final String items[] = { "Import from TheBlueAlliance.com", "Import from Roblu Cloud", "Import read only from Roblu Cloud", "Import from backup file", "Create event"};
    private final String sub_items[] = {"Import the event from an online database.", "Import an event from Roblu Cloud for use with multiple Roblu Master apps", "View and access the scouting data of a different team who has opted for publicly available scouting information.", "Import the event and all information from a previously exported backup file.","Create the event manually."};

    /**
     * Reference to the user's color and UI preferences
     */
    private RUI rui;
    private ProgressDialog d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event_picker);

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
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.clear);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create event");
            getSupportActionBar().setSubtitle("Choose an option");
        }

        /*
         * Bind choices to UI and setup an adapter
         */
        ListView sharingView = findViewById(R.id.listView1);
        List<Map<String, String>> data = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            Map<String, String> datum = new HashMap<>(2);
            datum.put("item", items[i]);
            datum.put("description", sub_items[i]);
            data.add(datum);
        }
        SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), data, android.R.layout.simple_list_item_2, new String[] { "item", "description" },
                new int[] { android.R.id.text1, android.R.id.text2 }) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                text1.setTextColor(rui.getText());
                text1 = view.findViewById(android.R.id.text2);
                text1.setTextColor(rui.getText());
                return view;
            }
        };
        sharingView.setAdapter(adapter);
        sharingView.setOnItemClickListener(this);

        /*
         * sync general UI settings
         */
        new UIHandler(this, toolbar).update();
    }

    /**
     * This method is called when an item from the list is selected
     * @param parent the parent adapter
     * @param view the view that was selected
     * @param position the vertical position of the selected view
     * @param id the ID of the view
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        /*
         * User selected manual event creation
         */
        if(position == 4) {
            startActivityForResult(new Intent(this, EventEditor.class), Constants.GENERAL);
        }
        /*
         * User selected TBA event import
         */
        else if(position == 0) {
            Intent intent = new Intent(this, TBAEventSelector.class);
            startActivityForResult(intent, Constants.GENERAL);
        }
        /*
         * User selected Roblu Cloud event import
         */
        else if(position == 1) {
            /*
             * First, check if an active cloud event already exists and warn the user
             * that they will be disabling that
             */
            if(doesActiveEventExist()) {
               new FastDialogBuilder()
                       .setTitle("Warning")
                       .setMessage("It looks like an active cloud event already exists on your device. Importing a new event from Roblu Cloud will disable the local" +
                               " event from cloud syncing. Do you want to proceed?")
                       .setPositiveButtonText("Yes")
                       .setNegativeButtonText("No")
                       .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                           @Override
                           public void accepted() {
                               importRobluCloudEvent(-1);
                           }

                           @Override
                           public void denied() {}

                           @Override
                           public void neutral() {}
                       }).build(EventCreateMethodPicker.this);
            } else importRobluCloudEvent(-1);
        }
        /*
         * User selected the read only event option, first get their username
         */
        else if(position == 2) {
            importPublicRobluCloudEvent();
        }
        /*
         * User selected import from backup file
         */
        else if(position == 3) {
            /*
             * Open a file chooser where the user can select a backup file to use.
             * We'll listen to a result in onActivityResult() and import the backup file there
             */
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(
                        Intent.createChooser(intent, "Select a .roblubackup file"),
                        Constants.FILE_CHOOSER);
            } catch (android.content.ActivityNotFoundException ex) {
                Utils.showSnackbar(findViewById(R.id.activity_create_event_picker), getApplicationContext(), "No file manager found", true, rui.getPrimaryColor());
            }
        }
    }

    /**
     * Imports an event from Roblu Cloud, given one exists
     *
     * @param teamNumber -1 to disable
     */
    private void importRobluCloudEvent(int teamNumber) {
        d = new ProgressDialog(EventCreateMethodPicker.this);
        d.setTitle("Get ready!");
        d.setMessage("Roblu is importing an event from Roblu Cloud...");
        d.setCancelable(false);
        d.show();

        // Stop the background service so it won't interfere
        IntentFilter serviceFilter = new IntentFilter();
        serviceFilter.addAction(Constants.SERVICE_ID);
        Intent serviceIntent = new Intent(this, Service.class);
        stopService(serviceIntent);
        EventDepacker dp = new EventDepacker(new IO(getApplicationContext()), teamNumber);
        dp.setListener(this);
        dp.start();
    }

    private void importPublicRobluCloudEvent() {
        // check for an internet connection
        if(!Utils.hasInternetConnection(getApplicationContext())) {
            Utils.showSnackbar(findViewById(R.id.advsettings), getApplicationContext(), "You are not connected to the internet", true, 0);
            return;
        }
            /*
             * We need to make sure that this thread has access to the internet
             */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        final RSettings settings = new IO(getApplicationContext()).loadSettings();

        RUI rui = settings.getRui();

        AlertDialog.Builder builder = new AlertDialog.Builder(EventCreateMethodPicker.this);
        LinearLayout layout = new LinearLayout(EventCreateMethodPicker.this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // this is the team code input edit text
        final AppCompatEditText input = new AppCompatEditText(EventCreateMethodPicker.this);
        Utils.setInputTextLayoutColor(rui.getAccent(),null, input);
        input.setHighlightColor(rui.getAccent());
        input.setHintTextColor(rui.getText());
        input.setTextColor(rui.getText());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(30);
        input.setFilters(FilterArray);
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                importRobluCloudEvent(Integer.parseInt(input.getText().toString()));
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { dialog.cancel(); }
        });
        TextView view = new TextView(EventCreateMethodPicker.this);
        view.setTextSize(Utils.DPToPX(getApplicationContext(), 5));
        view.setPadding(Utils.DPToPX(getApplicationContext(), 18), Utils.DPToPX(getApplicationContext(), 18), Utils.DPToPX(getApplicationContext(), 18), Utils.DPToPX(getApplicationContext(), 18));
        view.setText("FRC team number:");
        view.setTextColor(rui.getText());
        AlertDialog dialog = builder.create();
        dialog.setCustomTitle(view);
        if(dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = rui.getAnimation();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
        }
        dialog.show();
        dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(rui.getAccent());
        dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());
    }

    /**
     * Receives result data from child activities
     * @param requestCode the request code of the child activities
     * @param resultCode the result code of the child activity
     * @param data any result data returned from the activity
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*
         * The user selected a backup file, let's attempt to import it here
         */
        if(requestCode == Constants.FILE_CHOOSER) {
            // this means the user didn't select a file, no point in returning an error message
            if(data == null) return;

            try {
                IO io = new IO(getApplicationContext());
                RBackup backup = io.convertBackupFile(data.getData());
                if(!backup.getFileVersion().equals(IO.PREFIX)) {
                    Utils.showSnackbar(findViewById(R.id.activity_create_event_picker), getApplicationContext(), "Invalid backup file. Backup was created with an older version of Roblu.", true, rui.getPrimaryColor());
                    return;
                }
                /*
                 * Create the event, we're not gonna use an AsyncTask because the user is just
                 * watching the event import anyway, and it will only freeze the UI for a couple hundred
                 * milliseconds.
                 */
                REvent event = backup.getEvent();
                event.setCloudEnabled(false);
                event.setID(io.getNewEventID());
                io.saveEvent(event);
                io.saveForm(event.getID(), backup.getForm());
                if(backup.getTeams() != null) {
                    for(RTeam team : backup.getTeams()) {
                        for(RTab tab : team.getTabs()) {
                            for(RMetric metric : tab.getMetrics()) {
                                if(metric instanceof RGallery && ((RGallery) metric).getImages() != null) {
                                    // Add images to the current gallery
                                    for(int i = 0; i < ((RGallery) metric).getImages().size(); i++) {
                                        ((RGallery) metric).getPictureIDs().add(io.savePicture(event.getID(), ((RGallery) metric).getImages().get(i)));
                                    }
                                    ((RGallery) metric).setImages(null);
                                }
                            }
                        }
                        io.saveTeam(event.getID(), team);
                    }
                }
                Utils.showSnackbar(findViewById(R.id.activity_create_event_picker), getApplicationContext(), "Successfully imported event from backup", false, rui.getPrimaryColor());
                Intent intent = new Intent();
                intent.putExtra("eventID", event.getID());
                setResult(Constants.NEW_EVENT_CREATED, intent);
                finish();

            } catch(Exception e) {
                Utils.showSnackbar(findViewById(R.id.activity_create_event_picker), getApplicationContext(), "Invalid backup file", true, rui.getPrimaryColor());
            }
        }

        /*
         * The user created an event manually with EventEditor, we actually don't need to do anything but auto-finish our class
         * with a result code letting the TeamsView class now to refresh the event list
         */
        else if(resultCode == Constants.NEW_EVENT_CREATED) {
            Bundle b = data.getExtras();
            Intent intent = new Intent();
            if(b != null) intent.putExtras(b);
            setResult(Constants.NEW_EVENT_CREATED, intent);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            setResult(Constants.CANCELLED);
            finish();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        setResult(Constants.CANCELLED);
        finish();
    }

    @Override
    public void errorOccurred(String message) {
        Utils.showSnackbar(findViewById(R.id.activity_create_event_picker), getApplicationContext(), message, false, rui.getPrimaryColor());
        d.dismiss();
    }

    @Override
    public void success(final REvent event) {
        d.dismiss();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Start the background service
                IntentFilter serviceFilter = new IntentFilter();
                serviceFilter.addAction(Constants.SERVICE_ID);
                Intent serviceIntent = new Intent(EventCreateMethodPicker.this, Service.class);
                startService(serviceIntent);

                Intent intent = new Intent();
                intent.putExtra("eventID", event.getID());
                setResult(Constants.NEW_EVENT_CREATED, intent);
                finish();
            }
        });
    }

    /**
     * Returns true if an active event is being synced with the cloud
     * @return true an active event was found
     */
    private boolean doesActiveEventExist() {
        REvent[] events = new IO(getApplicationContext()).loadEvents();
        if(events != null && events.length > 0) {
            for(REvent event : events) {
                if(event.isCloudEnabled()) return true;
            }
            return false;
        } else return false;
    }

}
