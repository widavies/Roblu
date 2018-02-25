package com.cpjd.roblu.csv;

import android.support.annotation.NonNull;

import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.utils.MatchType;

import java.util.ArrayList;

import lombok.Data;

/**
 * RMatch is not an official model, it's just a helper class for CSV exports
 *
 * @version 2
 * @since 4.0.0
 * @author Will Davies
 */
@Data
public class RMatch implements Comparable<RMatch> {

    private ArrayList<RTeam> teams;
    private String matchName;

    /*
     * These are variables to assist with sorting, it makes sorting way, way faster.
     */
    private MatchType matchType;
    private int matchOrder = 0;
    private int subMatchOrder = 0;

    /**
     * @param matchName The name of this match
     */
    public RMatch(String matchName) {
        this.matchName = matchName;
        this.teams = new ArrayList<>();

        matchName = matchName.toLowerCase().trim();
        String[] tokens = matchName.split("\\s+");

        matchType = MatchType.getByName(tokens[0]);
        if(matchType.hasMatchOrder()) matchOrder = Integer.parseInt(tokens[1]);
        if(matchType.hasSubmatches()) subMatchOrder = Integer.parseInt(tokens[3]);
    }

    public String getAbbreviatedName() {
        String abbreviation;
        String matchName = this.matchName.toLowerCase();
        String[] tokens = matchName.split("\\s+");

        if(matchName.startsWith("predictions")) abbreviation = "P";
        else if(matchName.startsWith("quals")) abbreviation = tokens[1];
        else if(matchName.startsWith("quarters")) abbreviation = "Q"+tokens[1]+"M"+tokens[3];
        else if(matchName.startsWith("semis")) abbreviation = "S"+tokens[1]+"M"+tokens[3];
        else if(matchName.startsWith("finals")) abbreviation = "F"+tokens[1];
        else abbreviation = "null";

        return abbreviation;
    }

    @Override
    public int compareTo(@NonNull RMatch match) {
        if(this.matchType == match.getMatchType())
            if(this.matchOrder == match.getMatchOrder()) return (this.subMatchOrder - match.getSubMatchOrder());
            else return (this.matchOrder - match.getMatchOrder());
        else return (this.matchType.getMatchTypeOrder() - match.getMatchType().getMatchTypeOrder());
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof RMatch) &&
                (((RMatch) other).getMatchType() == this.matchType) &&
                (((RMatch) other).getMatchOrder() == this.matchOrder);
    }
}
