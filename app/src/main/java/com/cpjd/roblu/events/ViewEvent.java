package com.cpjd.roblu.events;

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
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RBackup;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ViewEvent extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private RUI rui;
    private String[] meta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event_picker);
        RSettings settings = new Loader(getApplicationContext()).loadSettings();
        rui = settings.getRui();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setTitle("View a Cloud Event");
        new UIHandler(this, toolbar).update();

        if(meta == null || meta.length == 0) return;

        ListView sharingView = (ListView) findViewById(R.id.listView1);

        List<Map<String, String>> data = new ArrayList<>();
        for (int i = 0; i < meta.length; i++) {
            Map<String, String> datum = new HashMap<>(2);
            datum.put("item", meta[i].split(":")[0]);
            data.add(datum);
        }
        SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), data, android.R.layout.simple_list_item_1, new String[] { "item", "description" },
                new int[] { android.R.id.text1}) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                text1.setTextColor(rui.getText());
                return view;
            }
        };
        sharingView.setAdapter(adapter);
        sharingView.setOnItemClickListener(this);

        sharingView.setAdapter(adapter);
        sharingView.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        RBackup backup = null; // get backup
        new ProcessBackup(backup).execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    private class ProcessBackup extends AsyncTask<Void, Void, REvent> {

        private final RBackup backup;

        ProcessBackup(RBackup backup) {
            this.backup = backup;
            backup.getEvent().setReadOnly(true);
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
            Bundle bundle = new Bundle();
            bundle.putLong("eventID", event.getID());
            Intent intent = new Intent();
            intent.putExtras(bundle);
            setResult(Constants.VIEW_EVENT_CONFIRMED, intent);
            finish();
        }
    }
}
