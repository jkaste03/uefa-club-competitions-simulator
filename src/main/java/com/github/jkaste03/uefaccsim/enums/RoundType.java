package com.github.jkaste03.uefaccsim.enums;

/**
 * Enum representing different round types in UEFA competitions.
 */
public enum RoundType {
    Q1("Q1"),
    Q2("Q2"),
    Q3("Q3"),
    PLAYOFF("Play-off"),
    LEAGUE_PHASE("League Phase"),
    KO_ROUND_PLAYOFF("Knockout Round Play-offs"),
    ROUND_OF_16("Round of 16"),
    QUARTER_FINAL("Quarter-finals"),
    SEMI_FINAL("Semi-finals"),
    FINAL("Final");

    /**
     * The full name of the round associated with the enum constant.
     */
    private final String roundName;

    /**
     * Constructor to initialize the enum constant with the round's full name.
     *
     * @param roundName the full name of the round
     */
    RoundType(String roundName) {
        this.roundName = roundName;
    }

    /**
     * Gets the full name of the round.
     *
     * @return the full name of the round
     */
    public String getRoundName() {
        return roundName;
    }

    @Override
    public String toString() {
        return roundName;
    }
}
