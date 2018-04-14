package com.cpjd.roblu.ui.pickList;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;

import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RPickList;

import java.util.ArrayList;

/**
 * Handles the tabs for the picklist
 *
 * @version 1
 * @since 4.5.7
 * @author Will Davies
 */
public class PickListAdapter extends FragmentStatePagerAdapter {

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

    private ArrayList<PickListFragment> fragments;

    public PickListAdapter(FragmentManager fm, Context context, REvent event) {
        super(fm);
        this.event = event;
        this.context = context;

        fragments = new ArrayList<>();

        // Create fragments
        for(int i = 0; i < PickList.pickLists.getPickLists().size(); i++) {
            fragments.add(loadList(i));
        }
    }

    @Override
    public Fragment getItem(int i) {
        return fragments.get(i);
    }

    /**
     * Deletes the tab at the specified position
     * @param position the position of the tab to delete
     */
    public void deleteList(int position) {
        PickList.pickLists.getPickLists().remove(position);
        fragments.remove(position);

        // Save
        new IO(context).savePickLists(event.getID(), PickList.pickLists);

        // The whole list needs to be regenerated
        fragments.clear();
        // Create fragments
        for(int i = 0; i < PickList.pickLists.getPickLists().size(); i++) {
            fragments.add(loadList(i));
        }

        removing = true;
        notifyDataSetChanged();
        removing = false;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        if(removing) return PagerAdapter.POSITION_NONE;
        if(object instanceof PickListFragment) return ((PickListFragment)object).getPosition();
        return 0;
    }

    public void reload() {
        notifyDataSetChanged();
        for(PickListFragment frag : fragments) {
            frag.load();
        }
    }

    private PickListFragment loadList(int position) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("event", event);
        bundle.putInt("position", position);
        PickListFragment fragment = new PickListFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public void createList(String title) {
        RPickList list = new RPickList(event.getID(), title);
        PickList.pickLists.getPickLists().add(list);

        // Save
        new IO(context).savePickLists(event.getID(), PickList.pickLists);

        fragments.add(loadList(getCount() - 1));

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return PickList.pickLists.getPickLists() != null ? PickList.pickLists.getPickLists().size() : 0;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return PickList.pickLists.getPickLists().get(position).getTitle();
    }
}
