package com.github.jkaste03.uefaccsim.model.competition;

/**
 * Fixed structural values in the UEFA competition format.
 */
public final class CompetitionFormatRules {

    private CompetitionFormatRules() {
        // Utility class.
    }

    /**
     * Number of clubs that participate in the knockout round playoff.
     */
    public static final int KO_ROUND_PLAYOFF_CLUB_COUNT = 16;

    /**
     * Excluding rebalancing, the number of ties in the UCL Q1 CP round is 16. If
     * the number of ties is less than 16, losers of ties need to jump to the UECL
     * Q3 CP round.
     */
    public static final int UCL_Q1_CP_TIES_WITHOUT_REBALANCING = 16;
}