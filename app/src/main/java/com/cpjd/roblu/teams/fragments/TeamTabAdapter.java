package com.cpjd.roblu.teams.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;

import com.cpjd.roblu.forms.SaveThread;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.utils.Text;

/*******************************************************
 * Copyright (C) 2016 Will Davies wdavies973@gmail.com
 *
 * This file is part of Roblu
 *
 * Roblu cannot be distributed for a price or to people outside of your local robotics team.
 *******************************************************/

// Tab adapter for the 3 match tabs
public class TeamTabAdapter extends FragmentStatePagerAdapter {

    private final Context context;

    // vars
    private final REvent event;
    private RTeam team;
    private final RForm form;

    private boolean removing;
    private boolean readOnly;

    public TeamTabAdapter(FragmentManager fm, REvent event, RTeam team, RForm form, Context context, boolean readOnly) {
        super(fm);
        this.event = event;
        this.team = team;
        this.form = form;
        this.context = context;
        this.readOnly = readOnly;
    }

    public boolean isPageRed(int page) {
        return page > 2 && team.getTabs().get(page - 1).isRedAlliance();
    }

    @Override
    public Fragment getItem(int i) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("event", event);
        bundle.putSerializable("team", team);
        bundle.putSerializable("form", form);
        bundle.putSerializable("position", 0);

        if (i == 0 && !readOnly) {
            Overview overview = new Overview();
            overview.setArguments(bundle);
            return overview;
        }
        else return loadMatch(i);

    }

    public boolean markWon(int position) {
        team.getTabs().get(position).setWon(!team.getTabs().get(position).isWon());
        new Loader(context).saveTeam(team, event.getID());
        notifyDataSetChanged();
        return team.getTabs().get(position).isWon();
    }

    public RTeam deleteTab(int position) {
        team.removeTab(position);
        team.updateEdit();
        new Loader(context).saveTeam(team, event.getID());
        removing = true;
        notifyDataSetChanged();
        removing = false;
        return team;
    }

    public void setTeam(RTeam team) {
        this.team = team;
    }

    @Override
    public int getItemPosition(Object object) {
        if(removing) return PagerAdapter.POSITION_NONE;

        if(object instanceof Match) {
            Match m = (Match) object;
            m.setTeam(team);
            m.load();

            return m.getPosition();
        }
        return 0;
    }

    private Fragment loadMatch(int position) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("event", event);
        bundle.putSerializable("team", team);
        bundle.putSerializable("form", form);
        bundle.putBoolean("readOnly", true);
        bundle.putInt("position", position);

        Match match = new Match();
        match.setArguments(bundle);
        return match;
    }

    public int createMatch(String name, boolean isRed) {
        int position = team.addTab(Text.createNew(form.getMatch()), name, isRed, false, 0);
        team.updateEdit();
        new SaveThread(context, event.getID(), team);

        Bundle bundle = new Bundle();
        bundle.putSerializable("event", event);
        bundle.putSerializable("team", team);
        bundle.putSerializable("form", form);
        bundle.putInt("position", position);

        Match match = new Match();
        match.setArguments(bundle);
        notifyDataSetChanged();
        return position + 1;
    }

    private boolean isWon(int position) {
        return team.getTabs().get(position - 1).isWon();
    }

    private String getWinSuffix(int position) {
        if(isWon(position)) return " ★";
        else return "";
    }


    @Override
    public int getCount() {
        return team.getTabs().size() + 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if(readOnly) return getWinSuffix(position)+" "+team.getTabs().get(position ).getTitle();

        if (position == 0) return "Overview";
        return getWinSuffix(position)+" "+team.getTabs().get(position - 1).getTitle();
    }
}
