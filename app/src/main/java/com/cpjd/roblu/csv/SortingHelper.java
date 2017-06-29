package com.cpjd.roblu.csv;

import android.support.annotation.NonNull;

import com.cpjd.roblu.utils.Text;

class SortingHelper implements Comparable<SortingHelper> {

    private final long score;

    private final String matchName;

    SortingHelper(String matchName) {
        this.matchName = matchName;

        score = Text.getMatchScore(matchName);
    }

    String getAbbreviatedName() {
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


    public String getName() {
        return matchName;
    }

    private long getScore() {
        return score;
    }

    @Override
    public int compareTo(@NonNull SortingHelper helper) {
        return ((Long)score).compareTo(helper.getScore());
    }
}
