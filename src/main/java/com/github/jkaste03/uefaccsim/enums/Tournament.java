package com.github.jkaste03.uefaccsim.enums;

/**
 * Enum representing different tournaments in UEFA competitions.
 */
public enum Tournament {
    CONFERENCE_LEAGUE("Conference League"), // ordinal = 0
    EUROPA_LEAGUE("Europa League"), // ordinal = 1
    CHAMPIONS_LEAGUE("Champions League");// ordinal = 2

    /**
     * The full name of the tournament associated with the enum constant.
     */
    private final String tournamentName;

    /**
     * Constructor to initialize the enum constant with the tournament's full name.
     *
     * @param tournamentName the full name of the tournament
     */
    Tournament(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    /**
     * Gets the full name of the tournament.
     *
     * @return the full name of the tournament
     */
    public String getTournamentName() {
        return tournamentName;
    }

    @Override
    public String toString() {
        return tournamentName;
    }
}
