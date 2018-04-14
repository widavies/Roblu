package com.cpjd.roblu.ui.teamSearch;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.ui.events.EventDrawerManager;
import com.cpjd.roblu.ui.teams.LoadTeamsTask;
import com.cpjd.roblu.ui.teams.TeamsRecyclerAdapter;
import com.cpjd.roblu.ui.teams.TeamsRecyclerTouchHelper;
import com.cpjd.roblu.ui.teams.TeamsView;
import com.cpjd.roblu.utils.Constants;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;

/**
 * This is a slightly more specific TeamsView activity. It will load a list of teams, and allow the user to
 * select one. Then, this team is returned to the parent activity.
 *
 * @author Will Davies
 * @since 4.5.7
 * @version 1
 */
public class TeamSearch extends AppCompatActivity implements View.OnClickListener, TeamsRecyclerAdapter.TeamSelectedListener, View.OnLongClickListener, EventDrawerManager.EventSelectListener, LoadTeamsTask.LoadTeamsTaskListener  {

    private ArrayList<RTeam> teams;
    private RecyclerView rv;
    private TeamsRecyclerAdapter adapter;
    private FloatingActionButton searchButton;
    private ProgressBar bar;
    private LoadTeamsTask loadTeamsTask;
    private REvent event;
    private MaterialSearchView searchView;
    private String lastQuery;
    private RSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teams_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add Team");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        event = (REvent)getIntent().getSerializableExtra("event");
        settings = new IO(getApplicationContext()).loadSettings();

        // Progress bar, also make sure to hide it
        bar = findViewById(R.id.progress_bar);
        bar.setVisibility(View.GONE);

        // Recycler View, UI front-end to teams array
        rv = findViewById(R.id.teams_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);

        // Setup the team adapter
        adapter = new TeamsRecyclerAdapter(getApplicationContext(), this);
        rv.setAdapter(adapter);

        // Setup the UI gestures manager and link to recycler view
        TeamsRecyclerTouchHelper callback = new TeamsRecyclerTouchHelper(adapter);
        callback.setDisableSwipe(true);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);
        // Search view
        searchView = findViewById(R.id.search_view);
        searchView.setHintTextColor(Color.BLACK);
        searchView.setHint("Name, number, or match");

        // Search button
        searchButton = findViewById(R.id.fab);
        searchButton.setOnClickListener(this);
        searchButton.setOnLongClickListener(this);

        // Link the search button appearance to the scrolling behavior of the recycler view
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if(dy > 0 && searchButton.isShown()) searchButton.hide();
                if(dy < 0 && !searchButton.isShown()) searchButton.show();
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) searchButton.show();
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        // If the user closes the search bar, refresh the teams view with all the original items
        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {}
            @Override
            public void onSearchViewClosed() {
                executeLoadTeamsTask(TeamsView.SORT_TYPE.NUMERICAL, false);
                searchButton.setVisibility(FloatingActionButton.VISIBLE);
            }
        });

        // Listen for text in the search view, if text is found, complete teh search
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.closeSearch();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                lastQuery = newText;
                executeLoadTeamsTask(TeamsView.SORT_TYPE.SEARCH, false);
                return true;
            }
        });

        // Make general changes to UI, keep it synced with RUI
        new UIHandler(this, toolbar, searchButton, false).update();

        executeLoadTeamsTask(TeamsView.SORT_TYPE.NUMERICAL, true);
    }

    @Override
    public void onClick(View v) {
        if(!searchView.isSearchOpen() && event != null) {
            searchView.showSearch(true);
            searchButton.setVisibility(FloatingActionButton.INVISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @Override
    public void eventSelected(REvent event) {

    }

    @Override
    public void teamsListLoaded(final ArrayList<RTeam> teams, final boolean hideZeroRelevanceTeams) {
        this.teams = teams;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bar.setVisibility(View.INVISIBLE);
                rv.setVisibility(View.VISIBLE);
                adapter.setTeams(teams, hideZeroRelevanceTeams);

                int numTeams = 0;
                if(teams != null) numTeams = teams.size();

                StringBuilder subtitle = new StringBuilder(String.valueOf(numTeams));
                subtitle.append(" Team");
                if(numTeams != 1) subtitle.append("s");
                if(getSupportActionBar() != null) getSupportActionBar().setSubtitle(subtitle.toString());
            }
        });
    }

    @Override
    public void teamSelected(View v) {
        RTeam team = adapter.getTeams().get(rv.getChildLayoutPosition(v));
        getIntent().putExtra("teamID", team.getID());
        setResult(Constants.TEAM_SEARCHED, getIntent());
        finish();
    }

    @Override
    public void teamDeleted(RTeam team) {
        adapter.reAdd(team);
    }

    private void executeLoadTeamsTask(int filter, boolean reload) {
        if(loadTeamsTask != null) {
            loadTeamsTask.quit();
        }

        loadTeamsTask = new LoadTeamsTask(new IO(getApplicationContext()), this);
        loadTeamsTask.setTaskParameters(event.getID(), filter, lastQuery, "");
        if(!reload) loadTeamsTask.setTeams(teams);
        // Flag UI to look like it's loading something
        rv.setVisibility(View.GONE);
        bar.setVisibility(View.VISIBLE);
        bar.getIndeterminateDrawable().setColorFilter(settings.getRui().getAccent(), PorterDuff.Mode.MULTIPLY);
        // Start the actual loading
        loadTeamsTask.start();
    }
}
