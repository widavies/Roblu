package com.cpjd.roblu.ui.mymatches;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.ui.team.TeamViewer;
import com.cpjd.roblu.utils.Constants;

import java.util.ArrayList;

/**
 * Displays the matches that the team is currently in.
 *
 * How it works:
 * -Load the team number from settings
 * -Loads the local team model for our team (it has to exist)
 * -Loads the matches from this model
 * -Separates the matches (RTABs) into a recycler view
 * -If a user taps on a match, we'll display a list of teams that the user can quickly
 * navigate to
 *
 * This is accessed by long pressing the search button, but you can easily bind this activity
 * to basically any other UI element.
 *
 * All that we need passed in to this class is the eventID of the active event
 *
 * @since 3.5.9
 * @author Will Davies
 */
public class MyMatches extends AppCompatActivity {
    // recyclerview displays "matches" nicely in cards in a scrollable view
    private RecyclerView rv;
    // the active eventID, used for loading the local team model
    private int eventID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setup ui stuff
        setContentView(R.layout.mymatches);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // access the eventID, if no eventID is passed in to this class, the app will crash
        eventID = getIntent().getIntExtra("eventID", 0);

        // load the number from settings
        int number = new IO(getApplicationContext()).loadSettings().getTeamNumber();
        if(number == 0) { // the user hasn't changed their number yet, and since no team is #0, we have to stop the activity
            Toast.makeText(getApplicationContext(), "No team number found. Set it in settings.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        /*
         * we have a reference to the team's number but not the team'd ID (we need the team's ID to load).
         *
         * so the problem here is that there is a potential for the user to have created multiple teams with
         * their own team number (not likely, but possible). Currently, this code will just load the first
         * team that matches our number that it comes across, but you could modify this code to do a "smart select"
         * that looks of data contained within each team (num of matches, size, last edit, etc.). for now, the first
         * team we come across should be fine and work 99% of the time
         */
        RTeam[] local = new IO(getApplicationContext()).loadTeams(eventID);
        RTeam myTeam = null;
        for(RTeam team : local) { // search through locally stored teams until we find one that matches our number
            if(team.getNumber() == number) {
                myTeam = team;
                break;
            }
        }

        if(myTeam == null) { // team will be null if it was not contained in the event, if no team, force close this activity
            Toast.makeText(getApplicationContext(), "Team is missing from event, please add it", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

         if(myTeam.getTabs() == null || myTeam.getTabs().size() <= 2) { // we found a team, but it doesn't contain any matches, so we can't load "my matches", force close
             Toast.makeText(getApplicationContext(), "Team does not contain any match data, please add some.", Toast.LENGTH_LONG).show();
             finish();
             return;
         }

         // for more on verification, visit the RTeam class, basically, to be safe, we want to sync the form and the team before we play around with any of them
        myTeam.verify(new IO(getApplicationContext()).loadForm(eventID));

        // next, we need to split apart the RTab array within our team, we want one RCheckout model per match
        ArrayList<RCheckout> toSave = new ArrayList<>(); // we'll use this array for storing info, one RChecklist per match

        for(int i = 0; i < myTeam.getTabs().size(); i++) {
            if(i < 2) continue; // we don't care about pit or predictions tabs, so skip them since they are always at index 0 & 1
            // create a new team with only one tab, wrap it in a checkout, and add it to our array
            RTeam team = new RTeam(myTeam.getName(), myTeam.getNumber(), myTeam.getID());
            team.addTab(myTeam.getTabs().get(i));
            toSave.add(new RCheckout(team));
        }

        // next, we need to look through our local teams list again and search for other teams in the same match as us, then we can add them to the either the teammates or opponents array
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

        // we've got everything we need, let's load it into the UI
        rv = (RecyclerView) findViewById(R.id.recycler);
        // manages the layout laoding
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false); // prevents a weird rendering issues

        // don't forget to sync our activity ui with the RUI settings
        new UIHandler(this, toolbar).update();
    }

    // if user taps the back button, just force close the activity, nothing to return
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
        setResult(Constants.CANCELLED);
        finish();
    }

    // user tapped on a match, let's load the "jump to team" dialog
    public void checkoutClicked(final View v) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() { // must be run on UI thread since it's a dialog and it's in a callback
                final RCheckout checkout = null;
                final Dialog d = new Dialog(MyMatches.this);
                d.setTitle("Open team ");
                d.setContentView(R.layout.event_import_dialog);
                final Spinner spinner = d.findViewById(R.id.type); // load up a chooser with 6 teams into it (3 teammates, 3 opponents)
                String[] values = new String[6];
                for(int i = 0; i < values.length; i++) {
                     if(i < 3) values[i] = checkout.getTeam().getTabs().get(0).getTeammates().get(i).getNumber() + " (Teammate)";
                     else values[i] = checkout.getTeam().getTabs().get(0).getOpponents().get(i - 3).getNumber() + " (Opponent)";
                }
                TextView t = d.findViewById(R.id.spinner_tip);
                t.setText(R.string.team);
                ArrayAdapter<String> adp = new ArrayAdapter<>(MyMatches.this, android.R.layout.simple_list_item_1, values);
                adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adp);
                Button button = d.findViewById(R.id.button7);
                button.setText(R.string.open);
                // launch teamviewer if the user selects a team and taps "open"
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = spinner.getSelectedItemPosition(); // 0,1,2,3,4,5
                        int ID;
                        // select the right team to load, since first 3 teams are our teammates, and last 3 are our opponents, we have to choose the correct array to pull from
                        if(pos < 3) ID = checkout.getTeam().getTabs().get(0).getTeammates().get(pos).getID();
                        else ID = checkout.getTeam().getTabs().get(0).getOpponents().get(pos - 3).getID();
                        Intent intent = new Intent(MyMatches.this, TeamViewer.class);
                        intent.putExtra("team", new IO(getApplicationContext()).loadTeam(eventID, ID).getID());
                        intent.putExtra("event", new IO(getApplicationContext()).loadEvent(eventID));
                        startActivity(intent);
                        d.dismiss();
                    }
                });
                // sync animation with ui settings
                if(d.getWindow() != null) d.getWindow().getAttributes().windowAnimations = new IO(getApplicationContext()).loadSettings().getRui().getAnimation();
                d.show(); // show the dialog
            }
        });

    }
}
