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
    /*
     * Map of opponent club ID to the number of matchups against that opponent.
     */
    private final Map<Integer, Integer> matchupCounts = new HashMap<>();

    private int seededCount;
    private int unseededCount;

    /*
     * Maps of opponent club ID to matchup counts for seeded and unseeded matchups.
     */
    private final Map<Integer, Integer> seededMatchupCounts = new HashMap<>();
    private final Map<Integer, Integer> unseededMatchupCounts = new HashMap<>();

    /**
     * Records a single matchup against an opponent.
     *
     * @param toClubId opponent club ID
     */
    public void recordMatchup(int toClubId) {
        matchupCounts.merge(toClubId, 1, Integer::sum);
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
            seededMatchupCounts.merge(toClubId, 1, Integer::sum);
        } else {
            unseededMatchupCounts.merge(toClubId, 1, Integer::sum);
        }
    }

    /**
     * Merges statistics from another instance into this one.
     *
     * @param other source statistics to merge in
     */
    public void mergeFrom(ClubRoundStats other) {
        mergeCounts(matchupCounts, other.matchupCounts);
        mergeCounts(seededMatchupCounts, other.seededMatchupCounts);
        mergeCounts(unseededMatchupCounts, other.unseededMatchupCounts);
        seededCount += other.seededCount;
        unseededCount += other.unseededCount;
    }

    /**
     * Helper method that sums values from a source map into a target map.
     *
     * @param targetCounts map to be updated
     * @param sourceCounts map with values to add
     */
    private void mergeCounts(Map<Integer, Integer> targetCounts, Map<Integer, Integer> sourceCounts) {
        sourceCounts.forEach((id, count) -> targetCounts.merge(id, count, Integer::sum));
    }

    /**
     * Returns an immutable copy of total matchup counts per opponent.
     *
     * @return map of opponent ID to matchup count
     */
    public Map<Integer, Integer> getMatchupCounts() {
        return Map.copyOf(matchupCounts);
    }

    /**
     * Returns an immutable copy of matchup counts for the selected seeding status.
     *
     * @param isSeeded {@code true} for seeded, {@code false} for unseeded
     * @return map of opponent ID to matchup count for the selected seeding status
     */
    public Map<Integer, Integer> getPerSeedingMatchupCounts(boolean isSeeded) {
        return isSeeded ? Map.copyOf(seededMatchupCounts) : Map.copyOf(unseededMatchupCounts);
    }

    /**
     * Checks if there are recorded seeding data.
     *
     * @return {@code true} if seeding data exists
     */
    public boolean hasSeedingData() {
        return seededCount > 0 || unseededCount > 0 || !seededMatchupCounts.isEmpty()
                || !unseededMatchupCounts.isEmpty();
    }

    /**
     * Returns the total number of recorded matchups.
     *
     * @return total number of matchups
     */
    public int getParticipationCount() {
        return matchupCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Returns the number of recorded matchups for the selected seeding status.
     *
     * @param isSeeded {@code true} for seeded, {@code false} for unseeded
     * @return number of matchups for the selected seeding status
     */
    public int getPerSeedingParticipationCount(boolean isSeeded) {
        if (isSeeded) {
            return seededMatchupCounts.values().stream().mapToInt(Integer::intValue).sum();
        } else {
            return unseededMatchupCounts.values().stream().mapToInt(Integer::intValue).sum();
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
}
