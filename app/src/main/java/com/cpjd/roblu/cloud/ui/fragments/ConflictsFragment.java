package com.cpjd.roblu.cloud.ui.fragments;

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
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RCheckout;

import java.util.ArrayList;

public class ConflictsFragment extends Fragment {

    private RecyclerView rv;
    private CheckoutAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.assignments_tab, container, false);

        Bundle bundle = this.getArguments();

        rv = (RecyclerView) view.findViewById(R.id.assign_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(view.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        adapter = new CheckoutAdapter(view.getContext(), bundle.getLong("eventID"), CheckoutAdapter.CONFLICTS);
        rv.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new AssignmentsTouchHelper(adapter, CheckoutAdapter.CONFLICTS);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        ArrayList<RCheckout> checkouts = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            checkouts.add(new RCheckout(0, new Loader(getContext()).loadTeam(0, i), "Will Davies", System.currentTimeMillis()));
        }
        adapter.setElements(checkouts);

        return view;
    }

}
