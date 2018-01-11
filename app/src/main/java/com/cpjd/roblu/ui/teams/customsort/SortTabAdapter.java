package com.cpjd.roblu.ui.teams.customsort;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.cpjd.roblu.ui.forms.TeamMetricProcessor;
import com.cpjd.roblu.forms.elements.ESort;
import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.models.RForm;

import java.util.ArrayList;

class SortTabAdapter extends FragmentPagerAdapter {

    private final RForm form;
    private final ArrayList<Element> other;
    private final long eventID;

    SortTabAdapter(FragmentManager fm, RForm form, long eventID) {
        super(fm);
        this.form = form;
        this.eventID = eventID;

        other = new ArrayList<>();
        other.add(new ESort("By wins", "Sort teams by how many matches they've won", 0));
        other.add(new ESort("By match", "Sort teams by a specific match", -1));
        other.add(new ESort("By # of matches", "Sort teams by how many matches they contain", 1));
    }

    @Override
    public Fragment getItem(int i) {
        Bundle bundle = new Bundle();
        bundle.putLong("eventID", eventID);
        if(i == 0 || i == 1) bundle.putSerializable("elements", form.getMatch());
        else if(i == 2) bundle.putSerializable("elements", form.getPit());
        else bundle.putSerializable("elements", other);

        if(i == 0) {
            MetricFragment frag = new MetricFragment();
            bundle.putSerializable("tabID", TeamMetricProcessor.MATCHES);
            frag.setArguments(bundle);
            return frag;
        }
        if(i == 1) {
            MetricFragment frag = new MetricFragment();
            bundle.putSerializable("tabID", TeamMetricProcessor.PREDICTIONS);
            frag.setArguments(bundle);
            return frag;
        }
        if(i == 2) {
            MetricFragment frag = new MetricFragment();
            bundle.putSerializable("tabID", TeamMetricProcessor.MATCHES);
            frag.setArguments(bundle);
            return frag;
        }
        if(i == 3) {
            MetricFragment frag = new MetricFragment();
            bundle.putSerializable("tabID", TeamMetricProcessor.OTHER);
            frag.setArguments(bundle);
            return frag;
        }
        return null;
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if(position == 0) return "By match metric";
        if(position == 1) return "By predictions";
        if(position == 2) return "By PIT metric";
        else return "Other";
    }
}
