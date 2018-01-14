package com.cpjd.roblu.ui.team.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.ui.forms.RMetricToUI;
import com.cpjd.roblu.utils.Utils;

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
public class Overview extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.overview_tab, container, false);

        Bundle bundle = this.getArguments();
        LinearLayoutCompat layout = view.findViewById(R.id.overview_layout);
        RTeam team = new IO(getActivity()).loadTeam(bundle.getInt("eventID"), bundle.getInt("teamID"));
        RMetricToUI rMetricToUI = new RMetricToUI(getActivity(), new IO(getActivity()).loadSettings().getRui(), true);

        /*
         * Add UI cards to the layout
         */
        // TBA info card
        final String s = "Team name: "+ team.getFullName()+"\n\nLocation: "+ team.getLocation()+"\n\nRookie year: "+ team.getRookieYear()+"\n\nMotto: "+ team.getMotto();
        layout.addView(rMetricToUI.getInfoField("TBA.com information", s, team.getWebsite(), team.getNumber()), 0);
        // "Other" card
        layout.addView(rMetricToUI.getInfoField("Other", "Last edited: "+ Utils.convertTime(team.getLastEdit())+"\nSize on disk: "+
                new IO(view.getContext()).getTeamSize(bundle.getInt("eventID"), team.getID())+" KB", "", 0));
        return view;
    }

}
