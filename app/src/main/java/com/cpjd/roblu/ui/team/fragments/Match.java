package com.cpjd.roblu.ui.team.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.metrics.RBoolean;
import com.cpjd.roblu.models.metrics.RCheckbox;
import com.cpjd.roblu.models.metrics.RChooser;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RSlider;
import com.cpjd.roblu.models.metrics.RStopwatch;
import com.cpjd.roblu.models.metrics.RTextfield;
import com.cpjd.roblu.ui.forms.RMetricToUI;
import com.cpjd.roblu.ui.team.TeamViewer;

import java.util.ArrayList;
/**
 * Match manages the loading of one RTab object
 *
 * Bundle incoming parameters:
 * -"position" - the spacial position of this tab, with the leftmost position being '0'
 * -"editable" - whether the team data should be editable
 * -"form" - a reference to the form model, might be NULL
 * -"event" - a reference to the REvent model
 *
 * @version 2
 * @since 1.0.0
 * @author Will Davies
 *
 */
public class Match extends Fragment implements RMetricToUI.MetricListener {

    /**
     * Spacial position of this tab
     */
    private int position;

    private REvent event;
    private RForm form;

    /**
     * Utility for converting RMetric models into UI elements that can be interacted with
     */
    private RMetricToUI els;

    private LinearLayoutCompat layout;
    /**
     * True if editing is allowed for this team
     */
    private boolean editable;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.match_tab, container, false);

        layout = view.findViewById(R.id.match_layout);

        Bundle bundle = this.getArguments();
        event = (REvent) bundle.getSerializable("event");
        form = (RForm) bundle.getSerializable("form");
        position = bundle.getInt("position") - 1;
        editable = bundle.getBoolean("readOnly");

        if(!editable) position++;

        els = new RMetricToUI(getActivity(), new IO(getActivity()).loadSettings().getRui(), editable);
        els.setListener(this);

        load();

        return view;
    }

    public void load() {
        if(layout != null && layout.getChildCount() > 0) layout.removeAllViews();

        ArrayList<RMetric> elements;
        if(position == 0) elements = form.getPit();
        else elements = form.getMatch();

        for(RMetric s : elements) {
            for (RMetric e : TeamViewer.team.getTabs().get(position).getMetrics()) {
                if (e.getID() == s.getID()) {
                    loadMetric(e);
                }
            }
        }

        // Add edits card
        if(event.isCloudEnabled()
                && TeamViewer.team.getTabs().get(position).getEdits() != null
                && TeamViewer.team.getTabs().get(position).getEdits().size() != 0) {
            layout.addView(els.getEditHistory(TeamViewer.team.getTabs().get(position).getEdits()));
        }
    }

    private void loadMetric(RMetric e) {
        if (e instanceof RBoolean) layout.addView(els.getBoolean((RBoolean)e));
        else if (e instanceof RCheckbox) layout.addView(els.getCheckbox((RCheckbox)e));
        else if (e instanceof RChooser) layout.addView(els.getChooser((RChooser)e));
        else if (e instanceof RCounter) layout.addView(els.getCounter((RCounter) e));
        else if (e instanceof RGallery) layout.addView(els.getGallery(false, null, (RGallery) e));
        else if (e instanceof RSlider) layout.addView(els.getSlider((RSlider) e));
        else if (e instanceof RStopwatch) layout.addView(els.getStopwatch((RStopwatch) e));
        else if (e instanceof RTextfield) layout.addView(els.getTextfield((RTextfield) e));
        else Log.d("RBS", "Couldn't resolve metric with name: "+e.getTitle());

    }

    @Override
    public void changeMade() {
        TeamViewer.team.setLastEdit(System.currentTimeMillis());
        TeamViewer.team.setName(((RTextfield)TeamViewer.team.getTabs().get(0).getMetrics().get(0)).getText());
        TeamViewer.team.setNumber(Integer.parseInt(((RTextfield)TeamViewer.team.getTabs().get(0).getMetrics().get(1)).getText()));
        // Check if the action bar needs to changed
        getActivity().getActionBar().setTitle(TeamViewer.team.getName());
        getActivity().getActionBar().setSubtitle("#"+TeamViewer.team.getNumber());
        new IO(getActivity()).saveTeam(event.getID(), TeamViewer.team);
    }
    public int getPosition() {
        if(!editable) return position;
        return position + 1;
    }

}
