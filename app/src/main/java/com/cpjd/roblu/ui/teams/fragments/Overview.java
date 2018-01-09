package com.cpjd.roblu.ui.teams.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cpjd.models.Team;
import com.cpjd.roblu.R;
import com.cpjd.roblu.ui.forms.Elements;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.teams.statistics.DATThread;
import com.cpjd.roblu.teams.statistics.StatsListener;
import com.cpjd.roblu.utils.Utils;

public class Overview extends Fragment implements StatsListener {
    private View view;
    private Elements els;
    private LinearLayoutCompat layout;
    private RTeam team;
    private REvent event;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.overview_tab, container, false);

        layout = (LinearLayoutCompat) view.findViewById(R.id.overview_layout);

        els = new Elements(getActivity(), new Loader(getActivity()).loadSettings().getRui(), null, false);

        Bundle bundle = this.getArguments();
        event = (REvent) bundle.getSerializable("event");
        team = new Loader(view.getContext()).loadTeam(event.getID(), bundle.getLong("team"));

        if(team.hasTBAInfo()) new DATThread(team.getNumber(), this);
        else {
            final String s = "Team name: "+team.getFullName()+"\n\nLocation: "+team.getLocation()+"\n\nRookie year: "+team.getRookieYear()+"\n\nMotto: "+team.getMotto();
            layout.addView(els.getInfoField("TBA.com information", s, team.getWebsite(),team.getNumber()), 0);
        }

        // Load the other card
        layout.addView(els.getInfoField("Other", "Last edited: "+ Utils.convertTime(team.getLastEdit())+"\nSize on disk: "+
                new Loader(view.getContext()).getTeamSize(event.getID(), team.getID())+" KB", "", 0));

        return view;
    }

    @Override
    public void retrievedDatabase(final Team team) {
        if(team.team_number == 0) return;

        this.team.setTBAInfo(team.nickname, team.location, team.motto, team.website, (int)team.rookie_year);
        new Loader(view.getContext()).saveTeam(this.team, event.getID());

        final String s = "Team name: "+team.nickname+"\n\nLocation: "+team.location+"\n\nRookie year: "+team.rookie_year+"\n\nMotto: "+team.motto;
        if(getActivity() == null) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                layout.addView(els.getInfoField("TBA.com information", s, team.website,(int) team.team_number), 0);
            }
        });
    }

}
