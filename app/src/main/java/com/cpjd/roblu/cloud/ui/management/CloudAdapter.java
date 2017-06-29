package com.cpjd.roblu.cloud.ui.management;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by Will Davies on 6/17/2017.
 */

public class CloudAdapter extends FragmentPagerAdapter {

    CloudAdapter(FragmentManager fm) {
        super(fm);

    }

    @Override
    public Fragment getItem(int i) {
        if(i == 0) return new ManageFragment();
        else return new ManageFragment();
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if(position == 0) return "Manage";
        else return "Members";
    }
}
