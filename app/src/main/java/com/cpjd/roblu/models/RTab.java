package com.cpjd.roblu.models;

import android.support.annotation.NonNull;

import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.utils.MatchType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import lombok.Data;

/**
 * This class stores information about either PIT, Prediction, or Match data.
 * It represents one physical instance of a form to represent a match tab, a predictions tab, or pit tab
 *
 * @version 2
 * @since 3.5.0
 * @author Will Davies
 */
@Data
public class RTab implements Serializable, Comparable<RTab> {
    /**
     * RTabs are identified by their title, no duplicate titles!
     */
    private String title;
    /**
     * Stores metrics and data for each metric
     */
    private ArrayList<RMetric> metrics;
    /**
     * FALSE for blue alliance, TRUE for red alliance
     */
    private boolean redAlliance;
    /**
     * Whether this match has been one, not applicable for PIT or PREDICTIONS
     */
    private boolean won;
    /**
     * The time this match will occur (Unix time), not applicable for PIT or PREDICTIONS
     */
    private long time;

    /**
     * Non-crucial data that stores a list of all edits made to this tab in the format <editor-name, timestamp>
     */
    private LinkedHashMap<String, Long> edits;

    /*
     * These are variables to assist with sorting, it makes sorting way, way faster.
     */
    private MatchType matchType;
    private int matchOrder = 0;
    private int subMatchOrder = 0;

    /*
     * Roblu Scouter ONLY helper variables
     */
    /**
     * Used for displaying some helpful information in the MyMatches view
     * @see com.cpjd.roblu.ui.mymatches.MyMatches
     */
    private ArrayList<RTeam> teammates, opponents;

    /**
     * The empty constructor is required for de-serialization
     */
    @SuppressWarnings("unused")
    public RTab() {}

    /**
     * Creates a RTab model, or a representation of a pit, predictions, or match tab
     * @param title unique title identifier
     * @param metrics elements and their included data for this tab
     * @param redAlliance false for blue alliance, true for red
     * @param won whether this match is won
     * @param time millisecond timestamp for when this match is scheduled for
     */
    public RTab(String title, ArrayList<RMetric> metrics, boolean redAlliance, boolean won, long time) {
        this.metrics = metrics;
        this.title = title;
        this.redAlliance = redAlliance;
        this.won = won;
        this.time = time;

        // Process MatchType
        String matchName = title.toLowerCase().trim();
        String[] tokens = matchName.split("\\s+");

        matchType = MatchType.getByName(tokens[0]);
        if(matchType.hasMatchOrder()) matchOrder = Integer.parseInt(tokens[1]);
        if(matchType.hasSubmatches()) subMatchOrder = Integer.parseInt(tokens[3]);
    }

    @Override
    public int compareTo(@NonNull RTab tab) {
        if(this.matchType == tab.getMatchType())
            if(this.matchOrder == tab.getMatchOrder()) return (this.subMatchOrder - tab.getSubMatchOrder());
            else return (this.matchOrder - tab.getMatchOrder());
        else return (this.matchType.getMatchTypeOrder() - tab.getMatchType().getMatchTypeOrder());
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof RTab) &&
                (((RTab) other).getMatchType() == this.matchType) &&
                (((RTab) other).getMatchOrder() == this.matchOrder);
    }
}