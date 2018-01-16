package com.cpjd.roblu.models;

import android.support.annotation.NonNull;

import com.cpjd.roblu.models.metrics.RBoolean;
import com.cpjd.roblu.models.metrics.RCheckbox;
import com.cpjd.roblu.models.metrics.RChooser;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RSlider;
import com.cpjd.roblu.models.metrics.RStopwatch;
import com.cpjd.roblu.models.metrics.RTextfield;
import com.cpjd.roblu.ui.teams.TeamsView;
import com.cpjd.roblu.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import lombok.Data;

/**
 * Welcome to the belly of the beast! (Not really)
 * This class models what data a team should store.
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
@Data
@SuppressWarnings("unused")
public class RTeam implements Serializable, Comparable<RTeam> {

    /**
     * Unique identifier for this team. No duplicate IDs allowed. Completely arbitrary.
     */
    private int ID;

    /**
     * Name of this team, can be really anything. Usually will be pulled from the Blue Alliance.
     */
    private String name;

    /**
     * Number of this team
     */
    private int number;

    /**
     * Time stamp of last edit made to this team. Also used for resolving merge conflicts
     */
    private long lastEdit;

    /**
     * Stores the scouting data. See RTab for more info.
     * tabs.get(0) is always the PIT tab
     * tabs.get(1) is always the Predictions tab
     */
    private ArrayList<RTab> tabs;

    /**
     * tabs.get(page) is the page that the user looked at last
     */
    private int page;

    /**
     * In order to make the user base happier by downloading less data,
     * TBA data is only downloaded once. Roblu can infer whether the download
     * happened by checking the below data.
     */
    private String fullName, location, motto, website;
    private int rookieYear;

    /**
     * Stores a SORT_TYPE variable for how this team should return compareTo() if it's included in a sorting operation
     * @see com.cpjd.roblu.ui.teams.TeamsView.SORT_TYPE
     * If filter == SORT_TYPE.CUSTOM_SORT or filter == SORT_TYPE.SEARCH, then customRelevance will be used instead
     * of other internal RTeam variables.
     */
    private transient int filter;
    /**
     * Custom double that is used for custom sorting. Double gives more flexibility to TeamMetricProcessor for values
     * that can be stored.
     */
    private transient double customRelevance;
    /**
     * If desired, a filterTag can be attached that will display extra information when this team is sorted.
     * For example, searching for a match to find teams that contain the queried match might want this tag to
     * say "Contains matches..."
     */
    private transient String filterTag;

    /**
     * Creates a new RTeam with default values
     * @param name the team's name
     * @param number the team's number
     * @param ID the arbitrary, unique identifier for this team
     */
    public RTeam(String name, int number, int ID) {
        this.name = name;
        this.number = number;
        this.ID = ID;
        this.page = 1; // set default page to PIT

        lastEdit = 0;
    }

    /**
     * verify() makes sure that the form and team are synchronized. Here's what it does:
     * <p>
     * PIT:
     * -If the user modified the form and ADDED elements, then we'll make sure to add them to this team's form copy
     * -If the user modified the form and REMOVED elements, then we'll make sure to remove them from this team's form copy
     * -If the user changed any item titles, change them right away
     * -If the user changed any default values, reset all the values on all elements that have NOT been modified
     * -If the user changed the order of any elements, change the order
     * <p>
     * MATCH:
     * -If the user modified the match form and ADDED elements, then we'll add those to EVERY match profile
     * -If the user modified the match form and REMOVED elements, then we'll remove those from EVERY match profile
     * -If the user changed any item titles, change them on ALL match profiles
     * -If the user changed any default values, reset all the values of EVERY match that have NOT been modified
     * -If the user changed the order of any elements, change the order
     * <p>
     * PREMISE:
     * -PIT and MATCH form arrays may NOT be null, only empty
     * <p>
     * NULLS to check for:
     * -If the team has never been opened before, set the PIT values, matches don't need to be set until creation.
     */
    public void verify(RForm form) {
        // Check for null or missing Pit & Predictions tabs
        if(this.tabs == null || this.tabs.size() == 0) {
            this.tabs = new ArrayList<>();
            addTab(new RTab("Pit", Utils.duplicateRMetricArray(form.getPit()) , false, false, 0));
            addTab(new RTab("Predictions",Utils.duplicateRMetricArray(form.getMatch()) , false, false, 0));
            return;
        }

        // Check to make sure the team name and number have been inserted into the form
        for(RMetric m : this.getTabs().get(0).getMetrics()) {
            if(m.getID() == 0) ((RTextfield)m).setText(name); // team name
            else if(m.getID() == 1) ((RTextfield)m).setText(String.valueOf(number)); // team number
        }

        // Remove elements that aren't on the form
        ArrayList<RMetric> temp = form.getPit(); // less if statements, just switches between PIT or MATCH depending on what needs to be verified
        for (int i = 0; i < tabs.size(); i++) {
            if(!tabs.get(i).getTitle().equalsIgnoreCase("Pit")) temp = form.getMatch();
            for (int j = 0; j < tabs.get(i).getMetrics().size(); j++) {
                boolean found = false;
                if(temp.size() == 0) {
                    tabs.get(i).getMetrics().clear();
                    break;
                }
                for (int k = 0; k < temp.size(); k++) {
                    if (tabs.get(i).getMetrics().get(j).getID() == temp.get(k).getID()) found = true;
                    if (k == temp.size() - 1 && !found) {
                        tabs.get(i).getMetrics().remove(j);
                        j = 0;
                        break;
                    }
                }
            }
        }

        // Add elements that are on the form, but not in this team
        temp = form.getPit();
        for (int i = 0; i < tabs.size(); i++) {
            if(!tabs.get(i).getTitle().equalsIgnoreCase("Pit")) temp = form.getMatch();
            for (int j = 0; j < temp.size(); j++) {
                boolean found = false;
                if(tabs.get(i).getMetrics().size() == 0) {
                    tabs.get(i).getMetrics().add(temp.get(j).clone());
                    continue;
                }
                for (int k = 0; k < tabs.get(i).getMetrics().size(); k++) {
                    if (tabs.get(i).getMetrics().get(k).getID() == temp.get(j).getID()) found = true;
                    if (k == tabs.get(i).getMetrics().size() - 1 && !found) {
                        tabs.get(i).getMetrics().add(temp.get(j).clone());
                        j = 0;
                        break;
                    }
                }
            }
        }

        // Update item names
        temp = form.getPit();
        for (int i = 0; i < tabs.size(); i++) {
            if(!tabs.get(i).getTitle().equalsIgnoreCase("PIT")) temp = form.getMatch();
            for (int j = 0; j < temp.size(); j++) {
                for (int k = 0; k < tabs.get(i).getMetrics().size(); k++) {
                    if (temp.get(j).getID() == tabs.get(i).getMetrics().get(k).getID()) {
                        tabs.get(i).getMetrics().get(k).setTitle(temp.get(j).getTitle());
                        break;
                    }
                }
            }
        }

        // Update default values for non-modified values, also check for some weird scenarios
        temp = form.getPit();
        for (int i = 0; i < tabs.size(); i++) {
            if(!tabs.get(i).getTitle().equalsIgnoreCase("PIT")) temp = form.getMatch();
            for (int j = 0; j < temp.size(); j++) {
                for (int k = 0; k < tabs.get(i).getMetrics().size(); k++) {
                    if (temp.get(j).getID() == tabs.get(i).getMetrics().get(k).getID()) {
                        RMetric e = temp.get(j);
                        RMetric s = tabs.get(i).getMetrics().get(k);

                        if (e instanceof RBoolean && !s.isModified() && s instanceof RBoolean)
                            ((RBoolean) s).setValue(((RBoolean) e).isValue());
                        else if (e instanceof RCheckbox && s instanceof RCheckbox) {
                            if (!s.isModified()) {
                                if(((RCheckbox) e).getValues() != null) ((RCheckbox) s).setValues(((RCheckbox) e).getValues());
                            }
                            if (!((RCheckbox) s).getValues().equals(((RCheckbox) e).getValues())) {
                                ((RCheckbox) s).setValues(((RCheckbox) e).getValues());
                                ((RCheckbox) s).setValues(((RCheckbox) e).getValues());
                            }
                        }
                        //else if (e instanceof RTextfield && !s.isModified()) ((RTextfield) s).setText(((RTextfield) e).getText());
                        else if (e instanceof RChooser && s instanceof RChooser) {
                            if (!s.isModified())
                                ((RChooser) s).setSelectedIndex(((RChooser) e).getSelectedIndex());
                            if (!Arrays.equals(((RChooser) s).getValues(), ((RChooser) e).getValues())) {
                                ((RChooser) s).setValues(((RChooser) e).getValues());
                                ((RChooser) s).setSelectedIndex(((RChooser) e).getSelectedIndex());
                            }
                        } else if (e instanceof RStopwatch && !s.isModified() && s instanceof RStopwatch)
                            ((RStopwatch) s).setTime(((RStopwatch) e).getTime());
                        else if (e instanceof RSlider && !s.isModified() && s instanceof RSlider) {
                            ((RSlider) s).setMax(((RSlider) e).getMax());
                            ((RSlider) s).setValue(((RSlider) e).getValue());
                            ((RSlider) s).setMin(((RSlider) e).getMin());

                        } else if (e instanceof RCounter && s instanceof RCounter) {
                            ((RCounter) s).setIncrement(((RCounter) e).getIncrement());
                            if (!s.isModified())
                                ((RCounter) s).setValue(((RCounter) e).getValue());
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Checks if their is locally stored TBA data, if there is, we don't need to redownload any more data
     * @return true if we've got locally stored TBA data
     */
    public boolean hasTBAInfo() {
        return fullName != null || location != null || motto != null || website != null || rookieYear > 0;
    }

    /**
     * Adds the tab to the team
     * @param tab the new RTab to add
     * @return index of sorted, newly added tab
     */
    public int addTab(RTab tab) {
        tabs.add(tab);
        Collections.sort(tabs);
        for(int i = 0; i < tabs.size(); i++) if(tabs.get(i).getTitle().equals(tab.getTitle())) return i;
        return 1;
    }
    /**
     * Deletes the tab from the RTabs array
     * @param position the index or position of the tab to delete
     */
    public void removeTab(int position) {
        tabs.remove(position);
    }

    /**
     * Shortcut method to get the elements from the specified tab
     * @param page the tab index
     * @return the form elements from that index
     */
    public ArrayList<RMetric> getMetrics(int page) {
        return tabs.get(page).getMetrics();
    }
    /**
     * Removes all the tab except the PIT and PREDICTIONS tabs
     */
    public void removeAllTabsButPIT() {
        if(tabs == null || tabs.size() == 0) return;

        RTab pit = tabs.get(0);
        RTab predictions = tabs.get(1);
        tabs.clear();
        tabs.add(pit);
        tabs.add(predictions);
    }
    /**
     * Returns the number of matches this team is in
     * @return returns the number of matches this team contains
     */
    public int getNumMatches() {
        if(tabs == null) return 0;
        else return tabs.size() - 2;
    }

    /**
     * Returns the sorting behavior for the team
     * @param team the team that is being compared to this one
     * @return a value representing which one should be sorted higher
     */
    @Override
    public int compareTo(@NonNull RTeam team) {
        if(filter == TeamsView.SORT_TYPE.ALPHABETICAL) return name.compareTo(team.getName());
        else if(filter == TeamsView.SORT_TYPE.NUMERICAL) return ((Integer)number).compareTo(team.getNumber());
        else if(filter == TeamsView.SORT_TYPE.LAST_EDIT) return ((Long)lastEdit).compareTo(team.getLastEdit());
        else if(filter == TeamsView.SORT_TYPE.SEARCH || filter == TeamsView.SORT_TYPE.CUSTOM_SORT) return ((Double)customRelevance).compareTo(team.getCustomRelevance());
        else return 0;
    }
}
