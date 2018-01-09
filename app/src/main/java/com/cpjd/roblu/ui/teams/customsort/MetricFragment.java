package com.cpjd.roblu.ui.teams.customsort;

import android.app.Dialog;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cpjd.roblu.R;
import com.cpjd.roblu.ui.forms.ElementTouchHelper;
import com.cpjd.roblu.ui.forms.ElementsAdapter;
import com.cpjd.roblu.ui.forms.ElementsProcessor;
import com.cpjd.roblu.forms.elements.ESTextfield;
import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;

public class MetricFragment extends Fragment implements SelectListener {

    private RecyclerView rv;

    private ArrayList<Element> elements;
    private long eventID;
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
        eventID = bundle.getLong("eventID");

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
        if(elements.get(position).getID() == -1) {
            final Dialog d = new Dialog(getActivity());
            d.setTitle("Select match");
            d.setContentView(R.layout.event_import_dialog);
            final Spinner spinner = (Spinner) d.findViewById(R.id.type);
            TextView t = (TextView) d.findViewById(R.id.spinner_tip);
            t.setText(R.string.match);
            final String[] values = getMatches();
            if(values == null) {
                Toast.makeText(getActivity(), "Error occured while loading matches. Do any matches exist?", Toast.LENGTH_LONG).show();
                return;
            }
            ArrayAdapter<String> adp = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, values);
            adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adp);
            Button button = (Button) d.findViewById(R.id.button7);
            button.setText(R.string.select);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent result = new Intent();
                    result.putExtra("sortToken", ElementsProcessor.OTHER+":-1:"+values[spinner.getSelectedItemPosition()]);
                    getActivity().setResult(Constants.CUSTOM_SORT_CONFIRMED, result);
                    getActivity().finish();
                    d.dismiss();
                }
            });
            if(d.getWindow() != null) d.getWindow().getAttributes().windowAnimations = new Loader(getActivity()).loadSettings().getRui().getAnimation();
            d.show();
            return;
        }

        String sortToken = tabID+":"+elements.get(position).getID();
        Intent result = new Intent();
        result.putExtra("sortToken", sortToken);
        getActivity().setResult(Constants.CUSTOM_SORT_CONFIRMED, result);
        getActivity().finish();
    }

    private String[] getMatches() {
        RTeam[] local = new Loader(getActivity()).getTeams(eventID);
        if(local == null || local.length == 0) return null;

        ArrayList<RTab> tabs = new ArrayList<>();

        RForm form = new Loader(getActivity()).loadForm(eventID);

        for(RTeam team : local) {
            team.verify(form);
            // check if the match already exists
            if(team.getTabs() == null || team.getTabs().size() == 0) continue;
            for(RTab tab : team.getTabs()) {
                if(tab.getTitle().equalsIgnoreCase("pit") || tab.getTitle().equalsIgnoreCase("predictions")) continue;
                boolean found = false;
                for(RTab temp : tabs) {
                    if(temp.getTitle().equalsIgnoreCase(tab.getTitle())) {
                        found = true;
                        break;
                    }
                }
                if(!found) tabs.add(tab);
            }
        }

        if(tabs.size() == 0) return null;

        Collections.sort(tabs);

        // Convert to String[]
        String[] values = new String[tabs.size()];
        for(int i = 0; i < tabs.size(); i++) {
            values[i] = tabs.get(i).getTitle();
        }

        return values;
    }
}
