package com.cpjd.roblu.events;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.cpjd.models.Event;
import com.cpjd.roblu.R;
import com.cpjd.roblu.forms.EditForm;
import com.cpjd.roblu.forms.Predefined;
import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RBackup;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.tba.APIEventSelect;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Text;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateEventPicker extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private final String items[] = { "Create event", "Import from TheBlueAlliance.com", "Import from backup file", "View Roblu Cloud Event"};
    private final String sub_items[] = {"Create the event manually.", "Import the event from an online database.\nRecommended", "Import the event and all information from a previously exported backup file.","View event from Roblu Cloud if it's available"};

    private long tempEventID;
    private Event tempEvent;

    private RelativeLayout layout;
    private RUI rui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event_picker);

        layout = (RelativeLayout) findViewById(R.id.activity_create_event_picker);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.clear);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setTitle("Create event");
        RSettings settings = new Loader(getApplicationContext()).loadSettings();
        rui = settings.getRui();

        ListView sharingView = (ListView) findViewById(R.id.listView1);

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
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                text1.setTextColor(rui.getText());
                text1 = (TextView) view.findViewById(android.R.id.text2);
                text1.setTextColor(rui.getText());
                return view;
            }
        };
        sharingView.setAdapter(adapter);
        sharingView.setOnItemClickListener(this);

        if(!settings.isPremium()) Text.loadAd((AdView)findViewById(R.id.adView));

        new UIHandler(this, toolbar).update();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(position == 0) {
            startActivityForResult(new Intent(this, CreateEvent.class), Constants.GENERAL);
        }
        if(position == 1) {
            Intent intent = new Intent(this, APIEventSelect.class);
            startActivityForResult(intent, Constants.GENERAL);
        }
        if(position == 2) {
            openFileChooser();
        }
        if(position == 3) {
            startActivityForResult(new Intent(this, ViewEvent.class), Constants.GENERAL);
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a .roblubackup file"),
                    Constants.FILE_CHOOSER);
        } catch (android.content.ActivityNotFoundException ex) {
            Text.showSnackbar(layout, getApplicationContext(), "No file manager found", true, rui.getPrimaryColor());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            setResult(Constants.PICKER_CANCELLED);
            finish();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        setResult(Constants.PICKER_CANCELLED);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == Constants.FILE_CHOOSER) {
            if(data == null) return;

            Loader l = new Loader(getApplicationContext());
            RBackup backup = l.convertBackupFile(data.getData());
            if(backup == null) {
                Text.showSnackbar(layout, getApplicationContext(), "Invalid backup file", true, rui.getPrimaryColor());
            } else {
                if(!backup.getFileVersion().equals(Loader.PREFIX)) {
                    Text.showSnackbar(layout, getApplicationContext(), "Invalid backup file. Backup was created with an older version of Roblu.", true, rui.getPrimaryColor());
                    return;
                }
                new ProcessBackup(backup).execute();
                finish();
            }
            return;
        }
        if(resultCode == Constants.MANUAL_CREATED){
            Intent result = new Intent();
            Bundle b = new Bundle();
            b.putLong("eventID", data.getLongExtra("eventID", 0));
            result.putExtras(b);
            setResult(Constants.PICKER_EVENT_CREATED, result);
            finish();
        }
        else if(resultCode == Constants.EVENT_IMPORTED) createImportedEvent((Event) data.getSerializableExtra("event"));
        else if(requestCode == Constants.REQUEST_IMPORT_FORM && resultCode == Constants.FORM_CONFIMRED) {
            Bundle bundle = data.getExtras();
            ArrayList<Element> tempPit = (ArrayList<Element>) bundle.getSerializable("tempPit");
            ArrayList<Element> tempMatch = (ArrayList<Element>) bundle.getSerializable("tempMatch");
            RForm form = new RForm(tempPit, tempMatch);
            Loader loader = new Loader(getApplicationContext());
            loader.saveForm(form, tempEventID);
            new ProcessMatches(tempEventID, tempEvent, form).execute();
            Intent result = new Intent();
            Bundle b = new Bundle();
            b.putLong("eventID", tempEventID);
            result.putExtras(b);
            setResult(Constants.PICKER_EVENT_CREATED, result);
            finish();
        } else if(requestCode == Constants.REQUEST_IMPORTED_PREDEFINED && resultCode == Constants.PREDEFINED_CONFIMRED) {
            RForm form = (RForm) data.getSerializableExtra("form");
            Loader loader = new Loader(getApplicationContext());
            loader.saveForm(form, tempEventID);
            new ProcessMatches(tempEventID, tempEvent, form).execute();
            Intent result = new Intent();
            Bundle b = new Bundle();
            b.putLong("eventID", tempEventID);
            result.putExtras(b);
            setResult(Constants.PICKER_EVENT_CREATED, result);
            finish();
        }
        else if((requestCode == Constants.REQUEST_IMPORT_FORM && resultCode == Constants.FORM_DISCARDED) || (requestCode == Constants.REQUEST_IMPORTED_PREDEFINED && resultCode == Constants.PREDEFINED_DISCARDED) ) {
            RForm form = Text.createEmpty();
            new Loader(getApplicationContext()).saveForm(form, tempEventID);
            new ProcessMatches(tempEventID, tempEvent, form).execute();
            Intent result = new Intent();
            Bundle b = new Bundle();
            b.putLong("eventID", tempEventID);
            result.putExtras(b);
            setResult(Constants.PICKER_EVENT_CREATED, result);
            finish();
        } else if(resultCode == Constants.VIEW_EVENT_CONFIRMED) {
            Bundle bundle = data.getExtras();
            bundle.putLong("eventID", bundle.getLong("eventID"));
            Intent intent = new Intent();
            intent.putExtras(bundle);
            setResult(Constants.PICKER_EVENT_CREATED, intent);
            finish();
        }
    }

    private void createImportedEvent(Event e) {
        tempEvent = e;

        Loader l = new Loader(getApplicationContext());

        REvent event = new REvent(e.name, e.getTimeInMillis(e.start_date), e.getTimeInMillis(e.end_date), l.getNewEventID());
        event.setKey(e.key);
        tempEventID = event.getID();

        l.saveEvent(event);
        for(int i = 0; i < e.teams.length; i++) {
            RTeam team = new RTeam(e.teams[i].nickname,(int)e.teams[i].team_number,l.getNewTeamID(event.getID()));
            l.saveTeam(team, event.getID());
        }

        final CharSequence[] items = { " Create form manually", " Predefined form ", " Use master form", " No form (create later) "};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Setup a form:");
        builder.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if(item == 0) {
                    Intent intent = new Intent(CreateEventPicker.this, EditForm.class);
                    startActivityForResult(intent, Constants.REQUEST_IMPORT_FORM);
                } else if(item == 1) {
                    Intent intent = new Intent(CreateEventPicker.this, Predefined.class);
                    startActivityForResult(intent, Constants.REQUEST_IMPORTED_PREDEFINED);
                } else if(item == 2){
                    RSettings settings = new Loader(getApplicationContext()).loadSettings();
                    RForm form = settings.getMaster();
                    if(form == null) {
                        form = Text.createEmpty();
                        settings.setMaster(form);
                        new Loader(getApplicationContext()).saveSettings(settings);
                    }
                    new ProcessMatches(tempEventID, tempEvent, form).execute();
                    new Loader(getApplicationContext()).saveForm(form, tempEventID);
                    Bundle b = new Bundle();
                    Intent result = new Intent();
                    b.putLong("eventID", tempEventID);
                    result.putExtras(b);
                    setResult(Constants.PICKER_EVENT_CREATED, result);
                    finish();
                } else {
                    RForm form = Text.createEmpty();
                    new ProcessMatches(tempEventID, tempEvent, form).execute();
                    new Loader(getApplicationContext()).saveForm(form, tempEventID);
                    Bundle b = new Bundle();
                    Intent result = new Intent();
                    b.putLong("eventID", tempEventID);
                    result.putExtras(b);
                    setResult(Constants.PICKER_EVENT_CREATED, result);
                    finish();
                }
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        if(dialog.getWindow() != null) dialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
        dialog.show();
    }

    private class ProcessMatches extends AsyncTask<Void, Void, Void> {

        private final long eventID;
        private final Event e;
        private final RForm form;

        ProcessMatches(long eventID, Event e, RForm form) {
            this.eventID = eventID;
            this.e = e;
            this.form = form;
        }

        protected Void doInBackground(Void... params) {
            Loader l = new Loader(getApplicationContext());

            RTeam[] teams = l.getTeams(eventID);

            Collections.sort(Arrays.asList(e.matches));

            if (teams == null || teams.length == 0) return null;

            int result;
            for (RTeam t : teams) {
                t.verify(form);

                for (int j = 0; j < e.matches.length; j++) {
                    result = e.matches[j].doesMatchContainTeam(t.getNumber());
                    if (result > 0) {
                        String name = "Match";
                        // process the correct match name
                        switch (e.matches[j].comp_level) {
                            case "qm":
                                name = "Quals " + e.matches[j].match_number;
                                break;
                            case "qf":
                                name = "Quarters " + e.matches[j].set_number + " Match " + e.matches[j].match_number;
                                break;
                            case "sf":
                                name = "Semis " + e.matches[j].set_number + " Match " + e.matches[j].match_number;
                                break;
                            case "f":
                                name = "Finals " + e.matches[j].match_number;
                        }
                        boolean isRed = result == com.cpjd.main.Constants.CONTAINS_TEAM_RED;

                        t.addTab(Text.createNew(form.getMatch()), name, isRed, e.matches[j].isOnWinningAlliance(t.getNumber()));
                        l.saveTeam(t, eventID);
                    }
                }
            }
            return null;
        }

    }

    private class ProcessBackup extends AsyncTask<Void, Void, REvent> {

        private final RBackup backup;

        ProcessBackup(RBackup backup) {
            this.backup = backup;
        }

        protected REvent doInBackground(Void... params) {
            Loader l = new Loader(getApplicationContext());
            REvent event = backup.getEvent();
            event.setID(l.getNewEventID());
            l.saveEvent(event);
            new Loader(getApplicationContext()).saveImages(event.getID(), backup.getImages());
            l.saveForm(backup.getForm(), event.getID());
            if(backup.getTeams() != null) for(RTeam team : backup.getTeams()) l.saveTeam(team, event.getID());
            return event;
        }

        protected void onPostExecute(REvent event) {
            Text.showSnackbar(layout, getApplicationContext(), "Succesfully imported event from backup", false, rui.getPrimaryColor());
            Bundle bundle = new Bundle();
            bundle.putLong("eventID", event.getID());
            Intent intent = new Intent();
            intent.putExtras(bundle);
            setResult(Constants.PICKER_EVENT_CREATED, intent);
            finish();
        }
    }
}
