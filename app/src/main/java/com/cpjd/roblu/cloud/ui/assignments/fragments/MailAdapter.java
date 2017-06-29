package com.cpjd.roblu.cloud.ui.assignments.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class MailAdapter extends FragmentPagerAdapter {

    public MailAdapter(FragmentManager fm) {
        super(fm);
    }
    @Override
    public Fragment getItem(int i) {
        if(i == 0) {
            AssignmentsFragment inbox = new AssignmentsFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable("inbox", true);
            inbox.setArguments(bundle);
            return inbox;
        }
        else {
            AssignmentsFragment outbox = new AssignmentsFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable("inbox", false);
            outbox.setArguments(bundle);
            return outbox;
        }
    }
    @Override
    public int getCount() {
        return 2    ;
    }
    @Override
    public CharSequence getPageTitle(int position) {
        if(position == 0) return "Inbox";
        else return "Outbox";
    }
}
