package com.github.jkaste03.uefaccsim.reporting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jkaste03.uefaccsim.model.competition.ClubSlot;
import com.github.jkaste03.uefaccsim.model.competition.Tie;

/**
 * Aggregates and exposes statistics for all clubs in a single round.
 * <p>
 * Keys in the internal map are club IDs, while values are the club's aggregated
 * round statistics.
 * </p>
 * matchesPerClub is used to store the number of matches each club plays in a
 * league-phase round, which allows normalisation of total matchup entries for
 * each club into participation counts.
 */
public class RoundStats {
    private final Map<Integer, ClubRoundStats> statsByRound = new HashMap<>();
    // For league-phase rounds we cache how many matches each club plays so we can
    // normalise total matchup entries for each club into participation counts.
    private int matchesPerClub = 0;

    /**
     * Merges statistics from another round stats instance into this one.
     *
     * @param other round statistics to merge in
     */
    public void mergeFrom(RoundStats other) {
        other.statsByRound
                .forEach((clubId, otherClubStats) -> getOrCreateClubRoundStats(clubId).mergeFrom(otherClubStats));
        // Preserve matchesPerClub if not already set locally. This ensures that
        // when merging per-thread aggregators the league-phase matches-per-club
        // value is propagated into the final aggregator.
        if (this.matchesPerClub == 0 && other.matchesPerClub > 0) {
            this.matchesPerClub = other.matchesPerClub;
        }
    }

    /**
     * Records matchups for all ties in the round.
     * <p>
     * Each tie produces one recorded matchup in both directions (A->B and B->A).
     * </p>
     *
     * @param ties ties that were drawn/played in the round
     */
    public void recordMatchup(List<? extends Tie> ties) {
        for (Tie tie : ties) {
            int club1Id = tie.getClubSlotA().getClubSimState().getId();
            int club2Id = tie.getClubSlotB().getClubSimState().getId();

            recordMatchup(club1Id, club2Id);
            recordMatchup(club2Id, club1Id);
        }
    }

    /**
     * Records a single matchup from one club to an opponent.
     *
     * @param fromClubId club ID that played the opponent
     * @param toClubId   opponent's club ID
     */
    private void recordMatchup(int fromClubId, int toClubId) {
        ClubRoundStats clubRoundStats = getOrCreateClubRoundStats(fromClubId);
        clubRoundStats.recordMatchup(toClubId);
    }

    /**
     * Records seeded/unseeded statistics for all relevant club slots.
     *
     * @param seeded   slots that are seeded
     * @param unseeded slots that are unseeded
     */
    public void recordSeeding(Set<ClubSlot> seeded, Set<ClubSlot> unseeded) {
        for (ClubSlot clubSlot : seeded) {
            recordSeeding(clubSlot, true);
        }
        for (ClubSlot clubSlot : unseeded) {
            recordSeeding(clubSlot, false);
        }
    }

    /**
     * Records the seeding status for a slot.
     * <p>
     * If the slot represents a tie, both underlying slots are traversed recursively
     * so that concrete clubs receive the correct seeding count.
     * </p>
     *
     * @param clubSlot slot to be recorded
     * @param isSeeded {@code true} if the slot/club is considered seeded
     */
    private void recordSeeding(ClubSlot clubSlot, boolean isSeeded) {
        if (clubSlot.isTie()) {
            recordSeeding(clubSlot.getTie().getClubSlotA(), isSeeded);
            recordSeeding(clubSlot.getTie().getClubSlotB(), isSeeded);
            return;
        }
        getOrCreateClubRoundStats(clubSlot.getClubSimState().getId())
                .recordSeeding(isSeeded);
    }

    /**
     * Records matchup statistics split on whether the club was seeded or unseeded
     * in the actual tie.
     *
     * @param ties   ties that were drawn/played in the round
     * @param seeded set of slots that were seeded in the draw
     */
    public void recordRoundStats(List<? extends Tie> ties, Set<ClubSlot> seeded) {
        for (Tie tie : ties) {
            int club1Id = tie.getClubSlotA().getClubSimState().getId();
            int club2Id = tie.getClubSlotB().getClubSimState().getId();

            boolean isClub1Seeded = seeded.contains(tie.getClubSlotA());
            getOrCreateClubRoundStats(club1Id)
                    .recordPerSeedingMatchup(isClub1Seeded, club2Id);

            getOrCreateClubRoundStats(club2Id)
                    .recordPerSeedingMatchup(!isClub1Seeded, club1Id);
        }
    }

    /**
     * Returns existing club statistics or creates a new one if the club is not
     * found in the map.
     *
     * @param clubId club ID
     * @return existing or newly created club statistics
     */
    private ClubRoundStats getOrCreateClubRoundStats(int clubId) {
        return statsByRound.computeIfAbsent(clubId, key -> new ClubRoundStats());
    }

    /**
     * Returns total participations for the club in the round.
     *
     * @param clubId club ID
     * @return number of recorded matchups
     */
    public int getParticipationCount(int clubId) {
        ClubRoundStats clubRoundStats = getOrCreateClubRoundStats(clubId);
        int totalMatchups = clubRoundStats.getMatchupCount();
        if (matchesPerClub > 0) {
            // Normalize: total matchup entries per club are matchesPerClub * participations
            return totalMatchups / matchesPerClub;
        }
        return totalMatchups;
    }

    /**
     * Sets matchesPerClub if it has not been set yet.
     *
     * @param m matches per club for this round
     */
    public void setMatchesPerClubIfAbsent(int m) {
        if (this.matchesPerClub == 0 && m > 0) {
            this.matchesPerClub = m;
        }
    }

    public int getMatchesPerClub() {
        return matchesPerClub;
    }

    /**
     * Returns participation count for the club filtered on seeding status.
     *
     * @param isSeeded {@code true} for seeded, {@code false} for unseeded
     * @param clubId   club ID
     * @return number of matchups in the selected seeding status
     */
    public int getPerSeedingParticipationCount(boolean isSeeded, int clubId) {
        ClubRoundStats clubRoundStats = getOrCreateClubRoundStats(clubId);
        return clubRoundStats.getPerSeedingParticipationCount(isSeeded);
    }

    /**
     * Returns the number of times the club was seeded or unseeded.
     *
     * @param isSeeded {@code true} for seeded count, {@code false} for unseeded
     *                 count
     * @param clubId   club ID
     * @return number of occurrences for the selected seeding status
     */
    public int getSeedingCount(boolean isSeeded, int clubId) {
        ClubRoundStats clubRoundStats = getOrCreateClubRoundStats(clubId);
        return clubRoundStats.getSeedingCount(isSeeded);
    }

    /**
     * Checks if at least one club in the round has recorded seeding data.
     *
     * @return {@code true} if seeding data exists
     */
    public boolean hasSeedingStats() {
        return statsByRound.values().stream().anyMatch(ClubRoundStats::hasSeedingData);
    }

    /**
     * Returns immutable matchup counts for a specific club.
     *
     * @param clubId club ID
     * @return map of opponent ID to matchup count
     */
    public Map<Integer, Integer> getMatchupCounts(int clubId) {
        ClubRoundStats clubRoundStats = getOrCreateClubRoundStats(clubId);
        return clubRoundStats.getMatchupCounts();
    }

    /**
     * Returns immutable matchup counts for a specific club filtered on seeding
     * status.
     *
     * @param isSeeded {@code true} for seeded, {@code false} for unseeded
     * @param clubId   club ID
     * @return map of opponent ID to matchup count for the selected seeding status
     */
    public Map<Integer, Integer> getPerSeedingMatchupCounts(boolean isSeeded, int clubId) {
        ClubRoundStats clubRoundStats = getOrCreateClubRoundStats(clubId);
        return clubRoundStats.getPerSeedingMatchupCounts(isSeeded);
    }

    /**
     * Returns a text representation of the entire round statistics.
     *
     * @return string representation of internal state
     */
    @Override
    public String toString() {
        return "RoundStats [statsByRound=" + statsByRound + ", matchesPerClub=" + matchesPerClub + "]";
    }
}
