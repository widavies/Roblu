package com.cpjd.roblu.cloud.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class MailAdapter extends FragmentPagerAdapter {

    private final long eventID;

    private ConflictsFragment conflicts;
    private InboxFragment inbox;

    public MailAdapter(FragmentManager fm, long eventID) {
        super(fm);
        this.eventID = eventID;
    }
    @Override
    public Fragment getItem(int i) {
        Bundle bundle = new Bundle();
        bundle.putLong("eventID", eventID);
        if(i == 0) {
            conflicts = new ConflictsFragment();
            conflicts.setArguments(bundle);
            return conflicts;
        } else {
            inbox = new InboxFragment();
            inbox.setArguments(bundle);
            return inbox;
        }
    }

    public void forceUpdate(boolean updateConflicts) {
        if(updateConflicts) conflicts.forceUpdate();
        else inbox.forceUpdate();
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if(position == 0) return "Merge conflicts";
        else return "Inbox";
    }
}
