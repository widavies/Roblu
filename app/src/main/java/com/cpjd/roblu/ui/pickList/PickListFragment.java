package com.cpjd.roblu.ui.pickList;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.ui.team.TeamViewer;
import com.cpjd.roblu.ui.teamSearch.TeamSearch;
import com.cpjd.roblu.ui.teams.TeamsRecyclerAdapter;
import com.cpjd.roblu.ui.teams.TeamsRecyclerTouchHelper;
import com.cpjd.roblu.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;

import lombok.Getter;

/*
 * Loads a list of teams in the pick list to the fragment
 */
public class PickListFragment extends Fragment implements TeamsRecyclerAdapter.TeamSelectedListener {

    // The horizontal position of this tab
    @Getter
    private int position;
    @Getter
    private TeamsRecyclerAdapter adapter;
    private RecyclerView rv;
    private REvent event;
    private View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.pick_list_tab, container, false);

        Bundle bundle = this.getArguments();
        position = bundle.getInt("position");
        event = (REvent) bundle.getSerializable("event");

        // Load teams
        ArrayList<Integer> teamIDS = PickList.pickLists.getPickLists().get(position).getTeamIDs();
        // Load teams to array
        ArrayList<RTeam> teams = new ArrayList<>();
        IO io = new IO(view.getContext());
        for(int teamID : teamIDS) {
            teams.add(io.loadTeam(event.getID(), teamID));
        }

        // Recycler View, UI front-end to teams array
        rv = view.findViewById(R.id.teams_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(view.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);

        // Setup the team adapter
        adapter = new TeamsRecyclerAdapter(view.getContext(), this);
        adapter.setTeams(teams);
        rv.setAdapter(adapter);

        // Setup the UI gestures manager and link to recycler view
        TeamsRecyclerTouchHelper callback = new TeamsRecyclerTouchHelper(adapter);
        callback.setEnableRearrangement(true);
        callback.setSwapListener(new TeamsRecyclerTouchHelper.SwapListener() {
            @Override
            public void teamsSwapped(int firstPosition, int secondPosition) {
                Collections.swap(PickList.pickLists.getPickLists().get(position).getTeamIDs(), firstPosition, secondPosition);
                new IO(view.getContext()).savePickLists(event.getID(), PickList.pickLists);
            }
        });
        callback.setDisableDialog(true);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setBackgroundColor(new IO(view.getContext()).loadSettings().getRui().getButtons());
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open the "team" search view
                Intent intent = new Intent(getActivity(), TeamSearch.class);
                intent.putExtra("event", event);
                intent.putExtra("position", position);
                startActivityForResult(intent, Constants.TEAM_SEARCH);
            }
        });

        return view;
    }

    public void load() {
        // Load teams
        ArrayList<Integer> teamIDS = PickList.pickLists.getPickLists().get(position).getTeamIDs();
        // Load teams to array
        ArrayList<RTeam> teams = new ArrayList<>();
        IO io = new IO(view.getContext());
        for(int teamID : teamIDS) {
            teams.add(io.loadTeam(event.getID(), teamID));
        }

        adapter.setTeams(teams);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void teamSelected(View v) {
        RTeam team = adapter.getTeams().get(rv.getChildLayoutPosition(v));
        Intent startView = new Intent(getActivity(), TeamViewer.class);
        startView.putExtra("teamID", team.getID());
        startView.putExtra("eventID", event.getID());
        startView.putExtra("editable", true);
        startView.putExtra("originalList", position);
        startActivityForResult(startView, Constants.GENERAL);
    }

    @Override
    public void teamDeleted(RTeam team) {
        // Remove from pick lists array
        for(int i = 0; i < PickList.pickLists.getPickLists().get(position).getTeamIDs().size(); i++) {
            if(PickList.pickLists.getPickLists().get(position).getTeamIDs().get(i) == team.getID()) {
                new IO(view.getContext()).savePickLists(event.getID(), PickList.pickLists);
                PickList.pickLists.getPickLists().get(position).getTeamIDs().remove(i);
                new IO(view.getContext()).savePickLists(event.getID(), PickList.pickLists);
                return;
            }
        }
    }
}
