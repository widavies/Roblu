package com.cpjd.roblu.models;

import android.support.annotation.NonNull;

import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.utils.Utils;

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
     * Roblu Scouter ONLY helper variables
     */
    /**
     * Used for displaying some helpful information in the MyMatches view
     * @see com.cpjd.roblu.ui.mymatches.MyMatches
     */
    private ArrayList<RTeam> teammates, opponents;


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
    }

    @Override
    public int compareTo(@NonNull RTab tab) {
        return ((Long) Utils.getMatchSortScore(title)).compareTo(Utils.getMatchSortScore(tab.getTitle()));
    }
}