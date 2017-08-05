package com.cpjd.roblu.cloud.ui.fragments;

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
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RCheckout;

import java.util.ArrayList;
import java.util.Arrays;

public class InboxFragment extends Fragment {

    private CheckoutAdapter adapter;
    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.assignments_tab, container, false);

        Bundle bundle = this.getArguments();

        RecyclerView rv = (RecyclerView) view.findViewById(R.id.assign_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(view.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        adapter = new CheckoutAdapter(view.getContext(), bundle.getLong("eventID"));
        rv.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new CheckoutsTouchHelper(adapter, CheckoutAdapter.INBOX);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        forceUpdate();

        return view;
    }

    public void forceUpdate() {
        new LoadCheckouts().execute();
    }

    private class LoadCheckouts extends AsyncTask<Void, Void, ArrayList<RCheckout>> {

        private Loader l;

        public LoadCheckouts() {
            l = new Loader(view.getContext());
        }

        @Override
        public ArrayList<RCheckout> doInBackground(Void... params) {
            RCheckout[] conflicts = l.loadCheckouts();
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
