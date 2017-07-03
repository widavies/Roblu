package com.cpjd.roblu.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.widget.Toast;

import com.cpjd.roblu.R;
import com.cpjd.roblu.cloud.ui.fragments.AssignmentsTouchHelper;
import com.cpjd.roblu.cloud.ui.fragments.CheckoutAdapter;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;

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
public class MyMatches extends AppCompatActivity {

    private ArrayList<RCheckout> toSave;
    private CheckoutAdapter adapter;
    private RecyclerView rv;
    private long eventID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.assignments_tab);

        eventID = getIntent().getLongExtra("eventID", 0);

        int number = new Loader(getApplicationContext()).loadSettings().getTeamNumber();
        RTeam[] local = new Loader(getApplicationContext()).getTeams(eventID);
        RTeam myTeam = null;
        for(RTeam team : local) {
            if(team.getNumber() == number) {
                myTeam = team;
                break;
            }
        }

        myTeam.verify(new Loader(getApplicationContext()).loadForm(eventID));

        if(myTeam == null) {
            Toast.makeText(getApplicationContext(), "No team number found. Set it in settings.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        toSave = new ArrayList<>(); // we'll use this array for storing info, one RChecklist per match

        if(myTeam.getTabs() == null || myTeam.getTabs().size() == 0) return;
        for(int i = 0; i < myTeam.getTabs().size(); i++) {
            if(i < 2) continue;
            RTeam temp = myTeam.duplicate();
            temp.removeAllTabsBut(i);
            toSave.add(new RCheckout(eventID, temp));
        }

        // Now, set teammates and opponents
        for(RCheckout checkout : toSave) {
            ArrayList<RTeam> teammates = new ArrayList<>();
            ArrayList<RTeam> opponents = new ArrayList<>();
            for(RTeam team : local) {
                for(RTab tab : team.getTabs()) {
                    if(tab.getTitle().equalsIgnoreCase(checkout.getTeam().getTabs().get(0).getTitle())) {
                        if(checkout.getTeam().getTabs().get(0).isRedAlliance() && tab.isRedAlliance()) teammates.add(team);
                        else opponents.add(team);
                    }
                }
            }
            checkout.getTeam().getTabs().get(0).setTeammates(teammates);
            checkout.getTeam().getTabs().get(0).setOpponents(opponents);
        }

        rv = (RecyclerView) findViewById(R.id.metric_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        adapter = new CheckoutAdapter(getApplicationContext(), eventID, CheckoutAdapter.MYMATCHES);
        rv.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new AssignmentsTouchHelper(adapter, CheckoutAdapter.MYMATCHES);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        adapter.setCheckouts(toSave);
    }

}
