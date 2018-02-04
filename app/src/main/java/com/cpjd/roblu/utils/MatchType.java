package com.cpjd.roblu.utils;

/**
 * A utility used for sorting matches
 */
public enum MatchType {
    PIT("PIT",                 1, false, false),
    PREDICTIONS("PREDICTIONS", 2, false, false),
    QUALS("Quals",             3, true, false),
    MATCH("Match",             4, true,  false),
    QUARTERS("Quarters",       5, true,  true),
    SEMIS("Semis",             6, true,  true),
    FINALS("Finals",           7, true,  false);

    private String name;
    private int matchTypeOrder;
    private boolean hasMatchOrder;
    private boolean hasSubmatches;

    MatchType(String name, int matchTypeOrder, boolean hasMatchOrder, boolean hasSubmatches) {
        this.name           = name;
        this.matchTypeOrder = matchTypeOrder;
        this.hasMatchOrder  = hasMatchOrder;
        this.hasSubmatches  = hasSubmatches;
    }

    public boolean hasMatchOrder() {
        return hasMatchOrder;
    }

    public boolean hasSubmatches() {
        return hasSubmatches;
    }

    public static MatchType getByName(String matchName) {
        for(MatchType value : values()) {
            if(value.getName().equalsIgnoreCase(matchName))
                return value;
        }
        return null;
    }

    private String getName() {
        return name;
    }

    public int getMatchTypeOrder() {
        return matchTypeOrder;
    }
}
