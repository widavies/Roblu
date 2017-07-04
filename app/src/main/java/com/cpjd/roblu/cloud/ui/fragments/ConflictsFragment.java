package com.cpjd.roblu.cloud.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cpjd.roblu.R;
import com.cpjd.roblu.cloud.ui.CheckoutListener;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.teams.TeamViewer;

import java.util.ArrayList;

public class ConflictsFragment extends Fragment implements CheckoutListener {

    private RecyclerView rv;
    private CheckoutAdapter adapter;
    private long eventID;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.assignments_tab, container, false);

        Bundle bundle = this.getArguments();
        eventID = bundle.getLong("eventID");

        rv = (RecyclerView) view.findViewById(R.id.assign_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(view.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        adapter = new CheckoutAdapter(view.getContext(), eventID, CheckoutAdapter.CONFLICTS, this);
        rv.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new AssignmentsTouchHelper(adapter, CheckoutAdapter.CONFLICTS);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        ArrayList<RCheckout> checkouts = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            RCheckout checkout = new RCheckout(0, new Loader(getActivity()).loadTeam(0, i), "Will Davies", System.currentTimeMillis());
            if(i == 1 || i == 3) checkout.setConflictType("Local copy already edited");
            else checkout.setConflictType("Not found in local repository");
            checkouts.add(checkout);
        }
        adapter.setCheckouts(checkouts);

        return view;
    }

    @Override
    public void checkoutClicked(View v) {
        RCheckout checkout = adapter.getCheckout(rv.getChildAdapterPosition(v));
        RForm form = new Loader(getActivity()).loadForm(eventID);
        if(checkout.getConflictType().equals("Local copy already edited")) {
            RTeam conflict = checkout.getTeam().duplicate();
            conflict.verify(form);

            RTeam localCopy = new Loader(getActivity()).loadTeam(eventID, conflict.getID());
            localCopy.verify(form);

            RTeam temp = new RTeam("Resolving", checkout.getTeam().getNumber(), checkout.getTeam().getID());
            for(int i = 0; i < conflict.getTabs().size(); i++) {
                for(int j = 0; j < localCopy.getTabs().size(); j++) {
                    if(conflict.getTabs().get(i).getTitle().equalsIgnoreCase(localCopy.getTabs().get(j).getTitle())) {
                        temp.addTab(localCopy.getTabs().get(j).duplicate());
                        break;
                    }
                }
                temp.addTab(conflict.getTabs().get(i).duplicate());
            }

            for(int i = 0; i < temp.getTabs().size(); i++) if(i % 2 == 0) temp.getTabs().get(i).setTitle(temp.getTabs().get(i).getTitle()+" (local)");

            Intent intent = new Intent(getActivity(), TeamViewer.class);
            intent.putExtra("event", new Loader(getActivity()).getEvent(eventID));
            intent.putExtra("team", temp);
            intent.putExtra("readOnly", true);
            startActivity(intent);
        } else {
            Intent intent = new Intent(getActivity(), TeamViewer.class);
            intent.putExtra("event", new Loader(getActivity()).getEvent(eventID));
            intent.putExtra("team", checkout.getTeam());
            intent.putExtra("readOnly", true);
            startActivity(intent);
        }
    }
}