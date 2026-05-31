package com.github.jkaste03.uefaccsim.reporting;

/**
 * Holds matchup counters for a specific club against a specific opponent within
 * a round. This includes total matchups, seeded/unseeded breakdowns, and
 * "would-have-been" matchup counts that consider potential matchups had the
 * club not advanced from the previous round of a higher-ranked tournament or
 * been eliminated in the previous round of the same tournament. That is
 * relevant for certain statistics that consider the potential matchups, even if
 * those matchups did not actually occur.
 */
public class ClubRoundCounters {
    private int matchups;
    private int seededMatchups;
    private int unseededMatchups;

    // These represent counters for matchups that would happen, but didn't.
    // "Would-have-been matchups" are matchups that would have occurred had the club
    // not advanced from the previous round of a higher-ranked tournament or been
    // eliminated in the previous round of the same tournament. This is relevant for
    // certain statistics that consider the potential matchups, even if those
    // matchups did not actually occur.
    private int wouldHaveBeenSeededMatchups;
    private int wouldHaveBeenUnseededMatchups;

    public void incrementMatchups() {
        matchups++;
    }

    public void incrementSeededMatchups() {
        seededMatchups++;
    }

    public void incrementUnseededMatchups() {
        unseededMatchups++;
    }

    public void incrementWouldHaveBeenSeededMatchups() {
        wouldHaveBeenSeededMatchups++;
    }

    public void incrementWouldHaveBeenUnseededMatchups() {
        wouldHaveBeenUnseededMatchups++;
    }

    public int getMatchups() {
        return matchups;
    }

    public int getSeededMatchups() {
        return seededMatchups;
    }

    public int getUnseededMatchups() {
        return unseededMatchups;
    }

    public int getWouldHaveBeenSeededMatchups() {
        return wouldHaveBeenSeededMatchups;
    }

    public int getWouldHaveBeenUnseededMatchups() {
        return wouldHaveBeenUnseededMatchups;
    }

    /**
     * Merges matchup counts from another {@code ClubRoundCounters} instance into
     * this instance by summing up the respective counters. This is useful for
     * aggregating statistics across multiple simulations or rounds.
     *
     * @param other the other {@code ClubRoundCounters} instance to merge from
     */
    public void mergeFrom(ClubRoundCounters other) {
        if (other == null)
            return;
        this.matchups += other.matchups;
        this.seededMatchups += other.seededMatchups;
        this.unseededMatchups += other.unseededMatchups;
        this.wouldHaveBeenSeededMatchups += other.wouldHaveBeenSeededMatchups;
        this.wouldHaveBeenUnseededMatchups += other.wouldHaveBeenUnseededMatchups;
    }

    @Override
    public String toString() {
        return "ClubRoundCounters [matchups=" + matchups + ", seededMatchups=" + seededMatchups + ", unseededMatchups="
                + unseededMatchups + ", wouldHaveBeenSeededMatchups=" + wouldHaveBeenSeededMatchups
                + ", wouldHaveBeenUnseededMatchups=" + wouldHaveBeenUnseededMatchups + "]";
    }
}
