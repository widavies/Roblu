package com.cpjd.roblu.ui.teams.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;

import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.ui.teams.TeamViewer;
import com.cpjd.roblu.utils.Utils;

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
    private final RForm form;

    private boolean removing;
    private final boolean readOnly;

    private final boolean isConflict;
    private final RCheckout checkout;

    public TeamTabAdapter(FragmentManager fm, REvent event, RForm form, Context context, boolean readOnly) {
        super(fm);
        this.event = event;
        this.form = form;
        this.context = context;
        this.readOnly = readOnly;
        this.isConflict = false;
        this.checkout = null;
    }

    public TeamTabAdapter(FragmentManager fm, REvent event, RCheckout checkout, RForm form, Context context) {
        super(fm);
        this.form = form;
        this.context = context;
        this.readOnly = true;
        this.event = event;
        this.isConflict = true;
        this.checkout = checkout;
    }

    public boolean isPageRed(int page) {
        if(readOnly) return TeamViewer.team.getTabs().get(page).isRedAlliance();
        return page > 2 && TeamViewer.team.getTabs().get(page - 1).isRedAlliance();
    }

    @Override
    public Fragment getItem(int i) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("event", event);
        bundle.putSerializable("position", 0);
        if(isConflict) {
            bundle.putBoolean("isConflict", true);
            bundle.putLong("checkout", checkout.getID());
        }

        if (i == 0 && !readOnly) {
            Overview overview = new Overview();
            overview.setArguments(bundle);
            return overview;
        }
        else return loadMatch(i);

    }

    public boolean markWon(int position) {
        TeamViewer.team.getTabs().get(position).setWon(!TeamViewer.team.getTabs().get(position).isWon());
        new Loader(context).saveTeam(TeamViewer.team, event.getID());
        notifyDataSetChanged();
        return TeamViewer.team.getTabs().get(position).isWon();
    }

    public RTeam deleteTab(int position) {
        TeamViewer.team.removeTab(position);
        TeamViewer.team.updateEdit();
        new Loader(context).saveTeam(TeamViewer.team, event.getID());
        removing = true;
        notifyDataSetChanged();
        removing = false;
        return TeamViewer.team;
    }

    @Override
    public int getItemPosition(Object object) {
        if(removing) return PagerAdapter.POSITION_NONE;

        if(object instanceof Match) {
            Match m = (Match) object;
            m.load();

            return m.getPosition();
        }
        return 0;
    }

    private Fragment loadMatch(int position) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("event", event);
        bundle.putLong("team", TeamViewer.team.getID());
        bundle.putSerializable("form", form);
        bundle.putBoolean("readOnly", readOnly);
        bundle.putInt("position", position);
        if(isConflict) {
            bundle.putBoolean("isConflict", true);
            bundle.putLong("checkout", checkout.getID());
        }
        Match match = new Match();
        match.setArguments(bundle);
        return match;
    }

    public int createMatch(String name, boolean isRed) {
        int position = TeamViewer.team.addTab(Utils.createNew(form.getMatch()), name, isRed, false, 0);
        TeamViewer.team.getTabs().get(position).setModified(event.isCloudEnabled());
        TeamViewer.team.updateEdit();
        new SaveThread(context, event.getID(), TeamViewer.team);

        Bundle bundle = new Bundle();
        bundle.putSerializable("event", event);
        bundle.putLong("team", TeamViewer.team.getID());
        bundle.putSerializable("form", form);
        bundle.putBoolean("readOnly", false);
        bundle.putInt("position", position);

        Match match = new Match();
        match.setArguments(bundle);
        notifyDataSetChanged();
        return position + 1;
    }

     private boolean isWon(int position) {
        if(readOnly) return TeamViewer.team.getTabs().get(position).isWon();
        return TeamViewer.team.getTabs().get(position - 1).isWon();
    }

    private String getWinSuffix(int position) {
        if(isWon(position)) return " ★";
        else return "";
    }


    @Override
    public int getCount() {
        if(readOnly) return TeamViewer.team.getTabs().size();
        return TeamViewer.team.getTabs().size() + 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if(readOnly) return getWinSuffix(position)+" "+TeamViewer.team.getTabs().get(position).getTitle();

        if (position == 0) return "Overview";
        return getWinSuffix(position)+" "+TeamViewer.team.getTabs().get(position - 1).getTitle();
    }
}
