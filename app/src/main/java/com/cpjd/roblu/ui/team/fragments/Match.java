package com.cpjd.roblu.ui.team.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.metrics.RBoolean;
import com.cpjd.roblu.models.metrics.RCalculation;
import com.cpjd.roblu.models.metrics.RCheckbox;
import com.cpjd.roblu.models.metrics.RChooser;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RDivider;
import com.cpjd.roblu.models.metrics.RFieldDiagram;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RSlider;
import com.cpjd.roblu.models.metrics.RStopwatch;
import com.cpjd.roblu.models.metrics.RTextfield;
import com.cpjd.roblu.ui.forms.RMetricToUI;
import com.cpjd.roblu.ui.team.TeamViewer;

import java.util.ArrayList;
import java.util.LinkedHashMap;

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

    private View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.match_tab, container, false);

        layout = view.findViewById(R.id.match_layout);

        Bundle bundle = this.getArguments();
        event = (REvent) bundle.getSerializable("event");
        form = (RForm) bundle.getSerializable("form");
        position = bundle.getInt("position") - 1;
        editable = bundle.getBoolean("editable");

        if(!editable) position++;

        // This is used for generating the metric cards, note the "event.getID() == -1", this flags it as editable if previewing,
        // instead of editable being true, because editable is used as a flag for several other UI things
        els = new RMetricToUI(getActivity(), new IO(getActivity()).loadSettings().getRui(), editable || event.getID() == -1);
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
        if(e instanceof RBoolean) layout.addView(els.getBoolean((RBoolean)e));
        else if(e instanceof RCheckbox) layout.addView(els.getCheckbox((RCheckbox)e));
        else if(e instanceof RChooser) layout.addView(els.getChooser((RChooser)e));
        else if(e instanceof RCounter) layout.addView(els.getCounter((RCounter) e));
        else if(e instanceof RGallery) layout.addView(els.getGallery(false, position, event.getID(), (RGallery) e));
        else if(e instanceof RSlider) layout.addView(els.getSlider((RSlider) e));
        else if(e instanceof RStopwatch) layout.addView(els.getStopwatch((RStopwatch) e, false));
        else if(e instanceof RTextfield) layout.addView(els.getTextfield((RTextfield) e));
        else if(e instanceof RDivider) layout.addView(els.getDivider((RDivider)e));
        else if(e instanceof RFieldDiagram) layout.addView(els.getFieldDiagram(position, (RFieldDiagram)e));
        else if(e instanceof RCalculation) layout.addView(els.getCalculationMetric(TeamViewer.team.getTabs().get(position).getMetrics(), ((RCalculation)e)));
        else Log.d("RBS", "Couldn't resolve metric with name: "+e.getTitle());
    }

    /**
     * This method is called when a change is made to a metric
     * @param metric the modified metric
     */
    @Override
    public void changeMade(RMetric metric) {
        /*
         * Notify any calculation metrics that a change was made
         */
        for(int i = 0; i < layout.getChildCount(); i++) {
            CardView cv = (CardView) layout.getChildAt(i);
            if(cv.getTag() != null && cv.getTag().toString().split(":")[0].equals("CALC")) {
                // We've discovered a calculation metric, we have access to the ID, so acquire a new copy of the view
                int ID = Integer.parseInt(cv.getTag().toString().split(":")[1]);
                for(RMetric m : TeamViewer.team.getTabs().get(position).getMetrics()) {
                    if(m.getID() == ID) {
                        // Get the calculation
                        String value = m.getTitle()+"\nValue: "+((RCalculation)m).getValue(TeamViewer.team.getTabs().get(position).getMetrics());
                        // Set the text
                        RelativeLayout rl = (RelativeLayout) cv.getChildAt(0);
                        TextView tv = (TextView) rl.getChildAt(0);
                        tv.setText(value);
                        break;
                    }
                }
            }
        }

        // set the metric as modified - this is a critical line, otherwise scouting data will get deleted
        metric.setModified(true);

        /*
         * Check the team name and team number metrics to see if the action bar needs to be updated
         */
        boolean init = false; // since team name and  team number are updated from the team model
        // without the user's control, make sure not to update team's timestamp if it's only the team name or number metric
        if(metric instanceof RTextfield) {
            if(((RTextfield) metric).isOneLine() && ((RTextfield) metric).isNumericalOnly() && !((RTextfield) metric).getText().equals("")) {
                TeamViewer.team.setNumber(Integer.parseInt(((RTextfield) metric).getText()));
                ((TeamViewer)getActivity()).setActionBarSubtitle("#"+TeamViewer.team.getNumber());
                init = true;
            }
            if(((RTextfield) metric).isOneLine() && !((RTextfield) metric).isNumericalOnly() && !((RTextfield) metric).getText().equals("")) {
                TeamViewer.team.setName(((RTextfield) metric).getText());
                ((TeamViewer)getActivity()).setActionBarTitle(TeamViewer.team.getName());
                init = true;
            }
        }
        if(!init) {
            TeamViewer.team.setLastEdit(System.currentTimeMillis());

            // Add local device to edit history list
            if(event.isCloudEnabled()) {
                LinkedHashMap<String, Long> edits = TeamViewer.team.getTabs().get(position).getEdits();
                if(edits == null) TeamViewer.team.getTabs().get(position).setEdits(new LinkedHashMap<String, Long>());
                TeamViewer.team.getTabs().get(position).getEdits().put("me", System.currentTimeMillis());
            }

        }
        // save the team
        new IO(view.getContext()).saveTeam(event.getID(), TeamViewer.team);
    }
    public int getPosition() {
        if(!editable) return position;
        return position + 1;
    }

}
