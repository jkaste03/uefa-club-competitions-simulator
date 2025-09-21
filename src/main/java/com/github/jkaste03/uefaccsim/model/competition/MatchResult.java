package com.github.jkaste03.uefaccsim.model.competition;

/**
 * Immutable result of a single match (leg) or extra time.
 *
 * @param homeGoals   number of goals scored by the home team (non-negative)
 * @param awayGoals   number of goals scored by the away team (non-negative)
 * @param inExtraTime true if the result was recorded in extra time; false if in
 *                    regular time
 */
public record MatchResult(int homeGoals, int awayGoals, boolean inExtraTime) {
    /**
     * Canonical constructor with basic validation.
     * 
     * @throws IllegalArgumentException if either goal count is negative
     */
    public MatchResult {
        if (homeGoals < 0 || awayGoals < 0) {
            throw new IllegalArgumentException("Goals must be non-negative");
        }
    }
}
