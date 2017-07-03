package com.cpjd.roblu.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cpjd.roblu.R;
import com.cpjd.roblu.cloud.ui.CheckoutListener;
import com.cpjd.roblu.cloud.ui.fragments.AssignmentsTouchHelper;
import com.cpjd.roblu.cloud.ui.fragments.CheckoutAdapter;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.teams.TeamViewer;
import com.cpjd.roblu.ui.UIHandler;

import java.util.ArrayList;

/**
 * Displays the matches that the team is currently in.
 *
 * For this class to work, we need access to:
 * -FRC team number
 * -Checkouts list
 *
 * @since 3.5.9
 * @author Will Davies
 */
public class MyMatches extends AppCompatActivity implements CheckoutListener {

    private ArrayList<RCheckout> toSave;
    private CheckoutAdapter adapter;
    private RecyclerView rv;
    private long eventID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mymatches);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        eventID = getIntent().getLongExtra("eventID", 0);

        int number = new Loader(getApplicationContext()).loadSettings().getTeamNumber();
        if(number == 0) {
            Toast.makeText(getApplicationContext(), "No team number found. Set it in settings.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        RTeam[] local = new Loader(getApplicationContext()).getTeams(eventID);
        RTeam myTeam = null;
        for(RTeam team : local) {
            if(team.getNumber() == number) {
                myTeam = team;
                break;
            }
        }
        if(myTeam == null || myTeam.getTabs() == null || myTeam.getTabs().size() == 0) {
            Toast.makeText(getApplicationContext(), "Team is missing from event, please add it", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        myTeam.verify(new Loader(getApplicationContext()).loadForm(eventID));

        toSave = new ArrayList<>(); // we'll use this array for storing info, one RChecklist per match

        for(int i = 0; i < myTeam.getTabs().size(); i++) {
            if(i < 2) continue;
            RTeam team = new RTeam(myTeam.getName(), myTeam.getNumber(), myTeam.getID());
            team.addTab(myTeam.getTabs().get(i));
            toSave.add(new RCheckout(eventID, team));
        }

        // Now, set teammates and opponents
        for(RCheckout checkout : toSave) {
            ArrayList<RTeam> teammates = new ArrayList<>();
            ArrayList<RTeam> opponents = new ArrayList<>();
            for(RTeam team : local) {
                for(RTab tab : team.getTabs()) {
                    if(tab.getTitle().equalsIgnoreCase(checkout.getTeam().getTabs().get(0).getTitle())) {
                        if(checkout.getTeam().getTabs().get(0).isRedAlliance() == tab.isRedAlliance()) teammates.add(team);
                        else opponents.add(team);
                    }
                }
            }
            checkout.getTeam().getTabs().get(0).setTeammates(teammates);
            checkout.getTeam().getTabs().get(0).setOpponents(opponents);
        }

        rv = (RecyclerView) findViewById(R.id.recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        adapter = new CheckoutAdapter(getApplicationContext(), eventID, CheckoutAdapter.MYMATCHES, this);
        rv.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new AssignmentsTouchHelper(adapter, CheckoutAdapter.MYMATCHES);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        adapter.setCheckouts(toSave);

        new UIHandler(this, toolbar).update();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }

    @Override
    public void checkoutClicked(final View v) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final RCheckout checkout = adapter.getCheckouts().get(rv.getChildAdapterPosition(v));
                final Dialog d = new Dialog(MyMatches.this);
                d.setTitle("Open team ");
                d.setContentView(R.layout.event_import_dialog);
                final Spinner spinner = (Spinner) d.findViewById(R.id.type);
                String[] values = new String[6];
                for(int i = 0; i < values.length; i++) {
                     if(i < 3) values[i] = checkout.getTeam().getTabs().get(0).getTeammates().get(i).getNumber() + " (Teammate)";
                     else values[i] = checkout.getTeam().getTabs().get(0).getOpponents().get(i - 3).getNumber() + " (Opponent)";
                }
                TextView t = (TextView) d.findViewById(R.id.spinner_tip);
                t.setText("Team: ");
                ArrayAdapter<String> adp = new ArrayAdapter<>(MyMatches.this, android.R.layout.simple_list_item_1, values);
                adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adp);
                Button button = (Button) d.findViewById(R.id.button7);
                button.setText("Open");
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = spinner.getSelectedItemPosition();
                        long ID;
                        if(pos < 3) ID = checkout.getTeam().getTabs().get(0).getTeammates().get(pos).getID();
                        else ID = checkout.getTeam().getTabs().get(0).getOpponents().get(pos - 3).getID();
                        Intent intent = new Intent(MyMatches.this, TeamViewer.class);
                        intent.putExtra("team", new Loader(getApplicationContext()).loadTeam(eventID, ID));
                        intent.putExtra("event", new Loader(getApplicationContext()).getEvent(eventID));
                        startActivity(intent);
                        d.dismiss();
                    }
                });
                if(d.getWindow() != null) d.getWindow().getAttributes().windowAnimations = new Loader(getApplicationContext()).loadSettings().getRui().getAnimation();
                d.show();
            }
        });

    }
}
