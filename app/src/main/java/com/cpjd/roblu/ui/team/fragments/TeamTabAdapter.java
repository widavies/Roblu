package com.cpjd.roblu.ui.team.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;

import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.ui.team.TeamViewer;
import com.cpjd.roblu.utils.HandoffStatus;
import com.cpjd.roblu.utils.Utils;

import java.util.Random;

/**
 * Handles the tabs for an RTab model
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class TeamTabAdapter extends FragmentStatePagerAdapter {
    /**
     * Context reference
     */
    private Context context;
    /**
     * Reference to the event that contains this team
     */
    private REvent event;
    /**
     * The form reference
     */
    private RForm form;
    /**
     * True if scouting data should be editable
     */
    private boolean editable;
    /**
     * Helper variable
     */
    private boolean removing;

    public TeamTabAdapter(FragmentManager fm, REvent event, RForm form, Context context, boolean editable) {
        super(fm);
        this.event = event;
        this.form = form;
        this.context = context;
        this.editable = editable;
    }

    @Override
    public Fragment getItem(int i) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("event", event);
        bundle.putSerializable("position", 0);
        if (i == 0 && editable) {
            Overview overview = new Overview();
            overview.setArguments(bundle);
            return overview;
        }
        else return loadMatch(i);

    }

    /**
     * Checks to see what alliance the team was on for this particular match
     * @param page the page index of the match
     * @return true if the team was on the red alliance for this match
     */
    public boolean isPageRed(int page) {
        if(!editable) return TeamViewer.team.getTabs().get(page).isRedAlliance();
        return page > 2 && TeamViewer.team.getTabs().get(page - 1).isRedAlliance();
    }

    /**
     * Marks the match as won
     * @param position the position of the match to mark as won
     * @return boolean representing match status (won or lost)
     */
    public boolean markWon(int position) {
        TeamViewer.team.getTabs().get(position).setWon(!TeamViewer.team.getTabs().get(position).isWon());
        TeamViewer.team.setLastEdit(System.currentTimeMillis());
        notifyDataSetChanged();
        return TeamViewer.team.getTabs().get(position).isWon();
    }

    /**
     * Deletes the tab at the specified position
     * @param position the position of the tab to delete
     */
    public void deleteTab(int position) {
        TeamViewer.team.removeTab(position);
        TeamViewer.team.setLastEdit(System.currentTimeMillis());
        removing = true;
        notifyDataSetChanged();
        removing = false;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
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
        bundle.putSerializable("form", form);
        bundle.putBoolean("editable", editable);
        bundle.putInt("position", position);
        Match match = new Match();
        match.setArguments(bundle);
        return match;
    }

    /**
     * Creates a new match with the specified parameter,
     * this method is used by the manual match creator, however,
     * most users will likely just use TBA.com importing.
     * @param name the name of the match
     * @param isRed true if this team is on the red alliance
     * @return the position of the sorted, created match
     */
    public int createMatch(String name, boolean isRed) {
        RTab tab = new RTab(TeamViewer.team.getNumber(), name, Utils.duplicateRMetricArray(form.getMatch()), isRed, false, 0);
        int position = TeamViewer.team.addTab(tab);
        TeamViewer.team.setLastEdit(System.currentTimeMillis());
        new IO(context).saveTeam(event.getID(), TeamViewer.team);

        // If these event is cloud synced, a new checkout needs to be packaged
        if(event.isCloudEnabled()) {
            RTeam newTeam = new RTeam(TeamViewer.team.getName(), TeamViewer.team.getNumber(), TeamViewer.team.getID());
            newTeam.addTab(tab);
            RCheckout checkout = new RCheckout(newTeam);
            /*
             * It would require a lot more code to check all devices and be sure that a new ID is
             * valid, so generate a random one. The chances of an error occurring are so low, this is acceptable (somewhat :\)
             */
            checkout.setID(new Random().nextInt(Integer.MAX_VALUE - 50_000) + 20_000);
            checkout.setStatus(HandoffStatus.AVAILABLE);
            new IO(context).savePendingCheckout(checkout);
        }

        Bundle bundle = new Bundle();
        bundle.putSerializable("event", event);
        bundle.putSerializable("form", form);
        bundle.putBoolean("readOnly", false);
        bundle.putInt("position", position);

        Match match = new Match();
        match.setArguments(bundle);
        notifyDataSetChanged();
        return position + 1;
    }

    /**
     * Checks if the match at the specified position is won
     * @param position the position of the tab to check
     * @return true if the team won in the specified match
     */
    private boolean isWon(int position) {
        if(!editable) return TeamViewer.team.getTabs().get(position).isWon();
        return TeamViewer.team.getTabs().get(position - 1).isWon();
    }

    /**
     * Gets the title suffix for each match tab, a won match will add a star
     * to the match tab
     * @param position the position of the match to get the win suffix for
     * @return the suffix to add to the tab title
     */
    private String getWinSuffix(int position) {
        if(isWon(position)) return " ★";
        else return "";
    }

    @Override
    public int getCount() {
        if(!editable) return TeamViewer.team.getTabs().size();
        return TeamViewer.team.getTabs().size() + 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if(!editable) return getWinSuffix(position)+" "+TeamViewer.team.getTabs().get(position).getTitle();

        if (position == 0) return "Overview";
        return getWinSuffix(position)+" "+TeamViewer.team.getTabs().get(position - 1).getTitle();
    }
}
