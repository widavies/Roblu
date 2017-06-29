package com.cpjd.roblu.teams.customsort;

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
import com.cpjd.roblu.forms.ElementTouchHelper;
import com.cpjd.roblu.forms.ElementsAdapter;
import com.cpjd.roblu.forms.elements.ESTextfield;
import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.utils.Constants;

import java.util.ArrayList;

public class MetricFragment extends Fragment implements SelectListener {

    private RecyclerView rv;

    private ArrayList<Element> elements;
    private int tabID;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.metric_tab, container, false);

        Bundle bundle = this.getArguments();

        elements = (ArrayList<Element>) bundle.getSerializable("elements");
        if(elements != null) {
            for(Element e : elements) {
                if(e instanceof ESTextfield) {
                    elements.remove(e);
                    break;
                }
            }
        }
        tabID = bundle.getInt("tabID");

        rv = (RecyclerView) view.findViewById(R.id.metric_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(view.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        ElementsAdapter adapter = new ElementsAdapter(view.getContext(), this);
        rv.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new ElementTouchHelper(adapter, true);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        adapter.pushElements(elements);
        return view;
    }

    @Override
    public void onItemClick(View v) {
        int position = rv.getChildLayoutPosition(v);
        String sortToken = tabID+":"+elements.get(position).getID();
        Intent result = new Intent();
        result.putExtra("sortToken", sortToken);
        getActivity().setResult(Constants.CUSTOM_SORT_CONFIRMED, result);
        getActivity().finish();
    }
}
