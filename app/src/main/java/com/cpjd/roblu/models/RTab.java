package com.cpjd.roblu.models;

import android.support.annotation.NonNull;

import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.utils.MatchType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

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
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;

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
     * This is a helper variable for teams who use the Red Device, Blue Device, etc. scouting system.
     * This variable can only be imported from the TBA.
     * Values 1-3 represent red alliance, 4-6 represent Blue alliance position.
     * However, use the redAlliance variable for display colors, because that's what the user can
     * change and modify. This should ONLY be used for sorting the4 Red Device, Blue Device checkout order.
     */
    private int alliancePosition;

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
    private int teamOrder = 0;

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
     * @param teamNumber only required for sorting, 0 is an OKAY input here
     * @param title unique title identifier
     * @param metrics elements and their included data for this tab
     * @param redAlliance false for blue alliance, true for red
     * @param won whether this match is won
     * @param time millisecond timestamp for when this match is scheduled for
     */
    public RTab(int teamNumber, String title, ArrayList<RMetric> metrics, boolean redAlliance, boolean won, long time) {
        this.metrics = metrics;
        this.title = title;
        this.redAlliance = redAlliance;
        this.won = won;
        this.time = time;
        this.alliancePosition = -1; // default to not found

        // Process MatchType
        String matchName = "team# "+teamNumber+" "+title.toLowerCase().trim();
        String[] tokens = matchName.split("\\s+");

        matchType = MatchType.getByName(tokens[2]);
        if(matchType.hasMatchOrder()) matchOrder = Integer.parseInt(tokens[3]);
        if(matchType.hasSubmatches()) subMatchOrder = Integer.parseInt(tokens[5]);
        teamOrder = Integer.parseInt(tokens[1]);
    }

    @Override
    public int compareTo(@NonNull RTab tab) {
        // Matches are different in some way
        if(this.matchType == tab.getMatchType()) {
            // Matches are exactly identical, sort by team number then
            if(this.matchOrder == tab.getMatchOrder() && this.subMatchOrder == tab.getSubMatchOrder()) return (this.getTeamOrder() - tab.getTeamOrder());

            if(this.matchOrder == tab.getMatchOrder()) return ((this.subMatchOrder) - (tab.getSubMatchOrder()));
            else return ((this.matchOrder) - (tab.getMatchOrder()));
        }
        else return ((this.matchType.getMatchTypeOrder()) - (tab.getMatchType().getMatchTypeOrder()));
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof RTab) &&
                (((RTab) other).getMatchType() == this.matchType) &&
                (((RTab) other).getMatchOrder() == this.matchOrder);
    }

    public boolean getSimpleMatchString(String match) {
        try {
            String adj = (title.toLowerCase().replaceAll("\\s+", "").replaceAll("Quals", "q").replaceAll("Quarters", "qu")
                    .replaceAll("Semis", "s").replaceAll("Finals", "f").replaceAll("Match", "m")).replace("predictions", "p");

            if(match.equals("pit") && adj.equals("pit")) return true;
            if(match.equals("p") && adj.equals("p")) return true;

            Pattern category = Pattern.compile("[a-zA-Z]+");
            Pattern specific = Pattern.compile("[a-zA-Z]+\\d+");
            Pattern specific2 = Pattern.compile("[a-zA-Z]+\\d+[a-zA-Z]+\\d+");

            if(category.matcher(match).matches()) {
                if(match.equals("qu") | match.equals("s")) match += "m";
                return adj.replaceAll("\\d+", "").equalsIgnoreCase(match);
            }
            else if(specific.matcher(match).matches() || specific2.matcher(match).matches())
                return adj.equalsIgnoreCase(match);
            else { // range
                int low = Integer.parseInt(match.substring(match.indexOf("(") + 1, match.indexOf("-")));
                int high = Integer.parseInt(match.substring(match.indexOf("-") + 1, match.indexOf(")")));

                if(low > high || (low < 0 || high < 0)) return false;

                for(int i = low; i <= high; i++) {
                    if(match.replaceAll("\\(\\d+-\\d+\\)", String.valueOf(i)).equalsIgnoreCase(adj)) return true;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}