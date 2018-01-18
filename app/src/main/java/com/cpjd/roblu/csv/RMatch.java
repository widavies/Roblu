package com.cpjd.roblu.csv;

import android.support.annotation.NonNull;

import com.cpjd.roblu.utils.Utils;

import lombok.Data;

/**
 * This class is a sorting utility to assist in CSV generation
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
@Data
public class RMatch implements Comparable<RMatch> {

    private long score;
    private String matchName;

    public RMatch(String matchName) {
        this.matchName = matchName;

        score = Utils.getMatchSortScore(matchName);
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
        return ((Long)score).compareTo(match.getScore());
    }
}
