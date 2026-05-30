package com.github.jkaste03.uefaccsim.reporting;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds statistics for a single club within a specific round.
 * <p>
 * The class stores:
 * </p>
 * <ul>
 * <li>total matchup counts against opponents,</li>
 * <li>the number of times the club was seeded,</li>
 * <li>matchup counts split between seeded and unseeded.</li>
 * </ul>
 */
public class ClubRoundStats {
    /**
     * Map of opponent club ID to matchup counters. This allows tracking how many
     * times the club faced each opponent, as well as seeding context for those
     * matchups.
     */
    private final Map<Integer, ClubRoundCounters> opponentCounters = new HashMap<>();
    private int seededCount;
    private int unseededCount;

    /**
     * Records a single matchup against an opponent.
     *
     * @param toClubId opponent club ID
     */
    public void recordMatchup(int toClubId) {
        getOrCreateOpponentCounters(toClubId).incrementMatchups();
    }

    /**
     * Records whether the club was seeded or unseeded in the draw.
     *
     * @param isSeeded {@code true} if the club was seeded
     */
    public void recordSeeding(boolean isSeeded) {
        if (isSeeded) {
            seededCount++;
        } else {
            unseededCount++;
        }
    }

    /**
     * Records a matchup with seeding context.
     *
     * @param isFromClubSeeded {@code true} if the club was seeded in the matchup
     * @param toClubId         opponent club ID
     */
    public void recordPerSeedingMatchup(boolean isFromClubSeeded, int toClubId) {
        if (isFromClubSeeded) {
            getOrCreateOpponentCounters(toClubId).incrementSeededMatchups();
        } else {
            getOrCreateOpponentCounters(toClubId).incrementUnseededMatchups();
        }
    }

    /**
     * Records a "would have been" matchup with seeding context.
     * <p>
     * A "Would have been" matchup refer to a matchup that would have happened had
     * not the club been eliminated in the previous round. This is relevant for
     * certain statistics that consider such potential matchups, even if those
     * matchups did not actually occur due to elimination.
     * <p>
     *
     * @param isFromClubSeeded {@code true} if the club was seeded in the matchup
     * @param toClubId         opponent club ID
     */
    public void recordPerSeedingWouldHaveBeenMatchup(boolean isFromClubSeeded, int toClubId) {
        if (isFromClubSeeded) {
            getOrCreateOpponentCounters(toClubId).incrementWouldHaveBeenSeededMatchups();
        } else {
            getOrCreateOpponentCounters(toClubId).incrementWouldHaveBeenUnseededMatchups();
        }
    }

    /**
     * Merges statistics from another instance into this one.
     *
     * @param other source statistics to merge in
     */
    public void mergeFrom(ClubRoundStats other) {
        other.opponentCounters.forEach((id, counters) -> getOrCreateOpponentCounters(id).mergeFrom(counters));
        seededCount += other.seededCount;
        unseededCount += other.unseededCount;
    }

    /**
     * Returns existing opponent counters or creates a new one if needed.
     *
     * @param opponentId opponent club ID
     * @return existing or newly created opponent counters
     */
    private ClubRoundCounters getOrCreateOpponentCounters(int opponentId) {
        return opponentCounters.computeIfAbsent(opponentId, id -> new ClubRoundCounters());
    }

    /**
     * Returns an immutable copy of matchup counts per opponent.
     *
     * @return map of opponent ID to matchup counts
     */
    public Map<Integer, ClubRoundCounters> getOpponentCounts() {
        return Map.copyOf(opponentCounters);
    }

    /**
     * Checks if there are recorded seeding data.
     *
     * @return {@code true} if seeding data exists
     */
    public boolean hasSeedingData() {
        return seededCount > 0 || unseededCount > 0;
    }

    /**
     * Returns the total number of recorded matchups.
     *
     * @return total number of matchups
     */
    public int getMatchupCount() {
        return opponentCounters.values().stream().mapToInt(ClubRoundCounters::getMatchups).sum();
    }

    /**
     * Returns the number of recorded matchups for the selected seeding status.
     *
     * @param isSeeded {@code true} for seeded, {@code false} for unseeded
     * @return number of matchups for the selected seeding status
     */
    public int getPerSeedingParticipationCount(boolean isSeeded) {
        if (isSeeded) {
            return opponentCounters.values().stream().mapToInt(ClubRoundCounters::getSeededMatchups).sum();
        } else {
            return opponentCounters.values().stream().mapToInt(ClubRoundCounters::getUnseededMatchups).sum();
        }
    }

    /**
     * Returns the number of times the club was seeded or unseeded.
     *
     * @param isSeeded {@code true} for seeded count, {@code false} for unseeded
     *                 count
     * @return number of occurrences for the selected seeding status
     */
    public int getSeedingCount(boolean isSeeded) {
        return isSeeded ? seededCount : unseededCount;
    }

    @Override
    public String toString() {
        return "ClubRoundStats [opponentCounters=" + opponentCounters + ", seededCount=" + seededCount
                + ", unseededCount=" + unseededCount + "]";
    }
}
