package com.cpjd.roblu.sync.cloud.ui.fragments;

import android.content.Intent;
import android.os.AsyncTask;
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
import com.cpjd.roblu.sync.cloud.ui.CheckoutListener;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.ui.team.TeamViewer;
import com.cpjd.roblu.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;

public class ConflictsFragment extends Fragment implements CheckoutListener {

    private RecyclerView rv;
    private CheckoutAdapter adapter;
    private long eventID;
    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.assignments_tab, container, false);

        Bundle bundle = this.getArguments();
        eventID = bundle.getLong("eventID");

        rv = (RecyclerView) view.findViewById(R.id.assign_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(view.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        adapter = new CheckoutAdapter(view.getContext(), CheckoutAdapter.CONFLICTS, this);
        rv.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new CheckoutsTouchHelper(adapter, CheckoutAdapter.CONFLICTS, eventID);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        forceUpdate();

        return view;
    }

    public void forceUpdate() {
        new LoadCheckouts().execute();
    }

    @Override
    public void checkoutClicked(View v) {
        RCheckout checkout = adapter.getCheckout(rv.getChildAdapterPosition(v));
        RForm form = new Loader(getActivity()).loadForm(eventID);
        if(checkout.getConflictType().equals("edited")) {
            RTeam conflict = checkout.getTeam();
            conflict.verify(form);

            RTeam localCopy = new Loader(getActivity()).loadTeam(eventID, conflict.getID());
            localCopy.verify(form);

            RTeam temp = new RTeam("Resolving "+conflict.getName(), checkout.getTeam().getNumber(), checkout.getTeam().getID());
            for(int i = 0; i < conflict.getTabs().size(); i++) {
                for(int j = 0; j < localCopy.getTabs().size(); j++) {
                    if(conflict.getTabs().get(i).getTitle().equalsIgnoreCase(localCopy.getTabs().get(j).getTitle())) {
                        temp.addTab(localCopy.getTabs().get(j));
                        temp.addTab(conflict.getTabs().get(i));
                        break;
                    }
                }
            }

            for(int i = 0; i < temp.getTabs().size(); i++) if(i % 2 == 0) temp.getTabs().get(i).setTitle(temp.getTabs().get(i).getTitle()+" (local)");

            temp.setID(-1);
            new Loader(view.getContext()).saveTeam(temp, eventID);

            Intent intent = new Intent(getActivity(), TeamViewer.class);
            intent.putExtra("event", new Loader(getActivity()).getEvent(eventID));
            intent.putExtra("team", temp.getID());
            intent.putExtra("isSpecialConflict", true);
            intent.putExtra("readOnly", true);
            startActivityForResult(intent, Constants.GENERAL);
        } else {
            Intent intent = new Intent(getActivity(), TeamViewer.class);
            intent.putExtra("event", new Loader(getActivity()).getEvent(eventID));
            intent.putExtra("isConflict", true);
            intent.putExtra("checkout", checkout.getID());
            intent.putExtra("readOnly", true);
            startActivity(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        new Loader(view.getContext()).deleteTeam(-1, eventID);
    }

    private class LoadCheckouts extends AsyncTask<Void, Void, ArrayList<RCheckout>> {

        private Loader l;

        public LoadCheckouts() {
            l = new Loader(view.getContext());
        }

        @Override
        public ArrayList<RCheckout> doInBackground(Void... params) {
            RCheckout[] conflicts = l.loadCheckoutConflicts();
            if(conflicts == null || conflicts.length == 0) return null;

            return new ArrayList<>(Arrays.asList(conflicts));
        }

        @Override
        public void onPostExecute(ArrayList<RCheckout> checkouts) {
            if(adapter != null) {
                adapter.removeAll();
                adapter.setCheckouts(checkouts);
            }
        }
    }
}
