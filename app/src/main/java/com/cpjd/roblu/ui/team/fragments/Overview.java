package com.cpjd.roblu.ui.team.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cpjd.models.Team;
import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.ui.forms.RMetricToUI;
import com.cpjd.roblu.ui.team.TBATeamInfoTask;
import com.cpjd.roblu.ui.team.TeamViewer;
import com.cpjd.roblu.utils.Utils;

import java.util.ArrayList;

/**
 * The overview tab will display some generic information about the team,
 * such as TBA info, size, meta information, etc.
 *
 * Keep in mind, TBA information is downloaded by TeamViewer.
 *
 * Parameters:
 * -"teamID" - the ID of the team
 * -"eventID" - the ID of the event
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class Overview extends Fragment implements TBATeamInfoTask.TBAInfoListener {

    private LinearLayoutCompat layout;
    private RMetricToUI rMetricToUI;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.overview_tab, container, false);

        Bundle bundle = this.getArguments();
        layout = view.findViewById(R.id.overview_layout);
        RTeam team = new IO(getActivity()).loadTeam(bundle.getInt("eventID"), TeamViewer.team.getID());
        rMetricToUI = new RMetricToUI(getActivity(), new IO(getActivity()).loadSettings().getRui(), true);

        /*
         * Do statistics generation!
         */

        for(int i = 0; i < team.getTabs().get(1).getMetrics().size(); i++) { // predictions form will be the same for every tab
            ArrayList<String> matchNames = new ArrayList<>();
            ArrayList<Double> values = new ArrayList<>();

            // Process all the values
            for(int j = 1; j < team.getTabs().size(); j++) {
                if(team.getTabs().get(j).getMetrics().get(i).isModified()) {
                    matchNames.add(team.getTabs().get(j).getTitle());
                    values.add(Double.parseDouble(team.getTabs().get(j).getMetrics().get(i).toString()));
                }
            }

            // Require at least two values for a chart
            if(values.size() <= 1) continue;

            // Determine the instance
            RMetric metric = team.getTabs().get(1).getMetrics().get(i);
            if(metric instanceof RCounter) {
                layout.addView(rMetricToUI.generateLineChart(metric.getTitle(), Utils.stringListToArray(matchNames), Utils.objectListToDoubleArray(values)));
            }

        }


        /*
         * Attempt to download TBA info for this team
         */

        if(!team.hasTBAInfo()) {
            new TBATeamInfoTask(team.getNumber(), this);
        } else {
            // TBA info card
            final String s = "Team name: "+ TeamViewer.team.getFullName()+"\n\nLocation: "+ TeamViewer.team.getLocation()+"\n\nRookie year: "+ TeamViewer.team.getRookieYear()+"\n\nMotto: "+ TeamViewer.team.getMotto();
            layout.addView(rMetricToUI.getInfoField("TBA.com information", s, TeamViewer.team.getWebsite(), TeamViewer.team.getNumber()), 0);
        }

        /*
         * Add UI cards to the layout
         */
        // "Other" card
        layout.addView(rMetricToUI.getInfoField("Other", "Last edited: "+ Utils.convertTime(team.getLastEdit())+"\nSize on disk: "+
                new IO(view.getContext()).getTeamSize(bundle.getInt("eventID"), team.getID())+" KB", "", 0));
        return view;
    }

    /**
     * This method is called when TBA information for the team is successfully downloaded
     * @param tbaTeam the TBA team model that contains downloaded information
     */

    @Override
    public void teamRetrieved(Team tbaTeam) {
        Log.d("RBS", "Downloaded TBA information for "+ tbaTeam.nickname);
        TeamViewer.team.setFullName(tbaTeam.name);
        TeamViewer.team.setLocation(tbaTeam.location);
        TeamViewer.team.setMotto(tbaTeam.motto);
        TeamViewer.team.setWebsite(tbaTeam.website);
        TeamViewer.team.setRookieYear((int)tbaTeam.rookie_year);
        new IO(getView().getContext()).saveTeam(getArguments().getInt("eventID"), TeamViewer.team);

        // TBA info card
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final String s = "Team name: "+ TeamViewer.team.getFullName()+"\n\nLocation: "+ TeamViewer.team.getLocation()+"\n\nRookie year: "+ TeamViewer.team.getRookieYear()+"\n\nMotto: "+ TeamViewer.team.getMotto();
                layout.addView(rMetricToUI.getInfoField("TBA.com information", s, TeamViewer.team.getWebsite(), TeamViewer.team.getNumber()), 0);
            }
        });
    }
}
