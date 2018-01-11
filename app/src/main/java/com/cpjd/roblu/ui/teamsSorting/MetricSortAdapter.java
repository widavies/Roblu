package com.cpjd.roblu.ui.teamsSorting;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.ui.forms.TeamMetricProcessor;

import java.util.ArrayList;

/**
 * This is the back-end to the MetricSortFragment array, it handles 3 of them
 *
 * @since 3.0.0
 * @version 2
 * @author Will Davies
 */
public class MetricSortAdapter extends FragmentPagerAdapter {

    /**
     * A reference is kept to RForm because the RMetric in the form are going to be transferred to a UI array
     * that the user can select from to select a sort method
     */
    private final RForm form;
    /**
     * Other sort methods that aren't RMetric based, lots of potential for new methods
     */
    private final ArrayList<RMetric> otherSortMethods;
    /**
     * A reference is kept to eventID because for sort method TeamMetricProcessor.PROCESS_METHOD.IN_MATCH, matches need
     * to be loaded from an event
     */
    private final int eventID;

    MetricSortAdapter(FragmentManager fm, RForm form, int eventID) {
        super(fm);
        this.form = form;
        this.eventID = eventID;

        otherSortMethods = new ArrayList<>();
        otherSortMethods.add(new RSort("By wins", TeamMetricProcessor.PROCESS_METHOD.MATCH_WINS, "Sort teams by how many matches they've won"));
        otherSortMethods.add(new RSort("By match", TeamMetricProcessor.PROCESS_METHOD.IN_MATCH, "Sort teams by a specific match"));
        otherSortMethods.add(new RSort("By # of matches", TeamMetricProcessor.PROCESS_METHOD.RESET, "Sort teams by how many matches they contain"));
    }

    @Override
    public Fragment getItem(int i) {
        Bundle bundle = new Bundle();

        if(i == 0) {
            MetricSortFragment frag = new MetricSortFragment();
            bundle.putInt("processMethod", TeamMetricProcessor.PROCESS_METHOD.PIT);
            bundle.putSerializable("metrics", form.getPit());
            frag.setArguments(bundle);
            return frag;
        }
        else if(i == 1) {
            MetricSortFragment frag = new MetricSortFragment();
            bundle.putInt("processMethod", TeamMetricProcessor.PROCESS_METHOD.PREDICTIONS);
            bundle.putSerializable("metrics", form.getMatch());
            frag.setArguments(bundle);
            return frag;
        }
        else if(i == 2) {
            MetricSortFragment frag = new MetricSortFragment();
            bundle.putInt("processMethod", TeamMetricProcessor.PROCESS_METHOD.MATCHES);
            bundle.putSerializable("metrics", form.getMatch());
            frag.setArguments(bundle);
            return frag;
        }
        else if(i == 3) {
            MetricSortFragment frag = new MetricSortFragment();
            bundle.putInt("processMethod", -1); // this actually includes all OTHER methods (PROCESS_METHOD.IN_MATCH, PROCESS_METHOD.MATCH_WINS, etc.)
            bundle.putInt("eventID", eventID);
            bundle.putSerializable("metrics", otherSortMethods);
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
