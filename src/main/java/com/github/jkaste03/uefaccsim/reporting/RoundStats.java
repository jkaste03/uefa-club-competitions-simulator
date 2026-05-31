package com.github.jkaste03.uefaccsim.reporting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.model.competition.ClubSlot;
import com.github.jkaste03.uefaccsim.model.competition.KnockoutTie;
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
    public void recordMatchups(List<? extends Tie> ties) {
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
    public void recordPerSeedingMatchups(List<? extends Tie> ties, Set<ClubSlot> seeded) {
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
     * For each tie, records "would-have-been" matchup statistics between the
     * alternative participant (if available) and the actual participant from the
     * opposite side.
     * <p>
     * "Would-have-been matchups" are matchups that would have occurred had clubs
     * not advanced from the previous round of a higher-ranked tournament or been
     * eliminated in the previous round of the same tournament. This is relevant for
     * certain statistics that consider the potential matchups, even if those
     * matchups did not actually occur.
     * <p>
     * For each side of a tie-backed slot, if the alternative participant from the
     * previous round exists, we record a "would-have-been" matchup against the
     * actual participant from the opposite side.
     * </p>
     *
     * @param ties   ties that were drawn in the round
     * @param seeded set of slots that were seeded in the draw; this determines
     *               whether the source side is counted as seeded or unseeded
     */
    public void recordPerSeedingWouldHaveBeenMatchups(List<KnockoutTie> ties, Set<ClubSlot> seeded) {
        //
        for (KnockoutTie tie : ties) {
            ClubSlot slotA = tie.getClubSlotA();
            ClubSlot slotB = tie.getClubSlotB();

            Integer actualA = resolveActualParticipantId(slotA, tie.getTournament());
            Integer alternativeA = resolveAlternativeParticipantId(slotA, tie.getTournament());
            Integer actualB = resolveActualParticipantId(slotB, tie.getTournament());
            Integer alternativeB = resolveAlternativeParticipantId(slotB, tie.getTournament());

            boolean isSlotASeeded = seeded.contains(slotA);
            boolean isSlotBSeeded = seeded.contains(slotB);

            // Only tie-backed slots have an alternative participant from the previous
            // round. Record that alternative against the actual participant from the
            // opposite side.
            if (slotA.isTie() && alternativeA != null && actualB != null) {
                getOrCreateClubRoundStats(alternativeA)
                        .recordPerSeedingWouldHaveBeenMatchup(isSlotASeeded, actualB);
            }
            if (slotB.isTie() && alternativeB != null && actualA != null) {
                getOrCreateClubRoundStats(alternativeB)
                        .recordPerSeedingWouldHaveBeenMatchup(isSlotBSeeded, actualA);
            }
        }
    }

    /**
     * Resolves the participant id for a slot based on whether they are the winner
     * or not.
     *
     * @param slot   slot that may represent a concrete club or an unresolved tie
     * @param winner whether to resolve for the winner or loser
     * @return participant id, or {@code null} if not decided yet
     */
    private Integer resolveToId(ClubSlot slot, boolean winner) {
        if (slot.isClub()) {
            return slot.getClubSimState().getId();
        }
        KnockoutTie tie = slot.getTie();
        Boolean aWinner = tie.isClubAWinner();
        if (aWinner == null) {
            return null;
        }
        return aWinner == winner ? tie.getClubSlotA().getClubSimState().getId()
                : tie.getClubSlotB().getClubSimState().getId();
    }

    /**
     * Resolves the actual participant represented by a slot in the current round.
     *
     * For tie-backed slots, whether winner or loser advances is determined by
     * tournament hierarchy, mirroring ClubSlot.resolveSlot(tournament).
     */
    private Integer resolveActualParticipantId(ClubSlot slot, Tournament currentRoundTournament) {
        if (slot.isClub()) {
            return slot.getClubSimState().getId();
        }
        Tournament sourceTournament = slot.getTie().getTournament();
        boolean sourceIsHigherLevel = sourceTournament.compareTo(currentRoundTournament) > 0;
        return sourceIsHigherLevel ? resolveToId(slot, false) : resolveToId(slot, true);
    }

    /**
     * Resolves the non-selected (alternative) participant represented by a slot in
     * the current round.
     */
    private Integer resolveAlternativeParticipantId(ClubSlot slot, Tournament currentRoundTournament) {
        if (!slot.isTie()) {
            return null;
        }
        Tournament sourceTournament = slot.getTie().getTournament();
        boolean sourceIsHigherLevel = sourceTournament.compareTo(currentRoundTournament) > 0;
        return sourceIsHigherLevel ? resolveToId(slot, true) : resolveToId(slot, false);
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
     * Checks if the club has recorded "would-have-been" matchups.
     * <p>
     * "Would-have-been matchups" are matchups that would have occurred had clubs
     * not advanced from the previous round of a higher-ranked tournament or been
     * eliminated in the previous round of the same tournament. This is relevant for
     * certain statistics that consider the potential matchups, even if those
     * matchups did not actually occur.
     * 
     * @param clubId club ID
     * @return {@code true} if the club has "would-have-been" matchups
     */
    public boolean clubHasWouldHaveBeenStats(int clubId) {
        ClubRoundStats clubRoundStats = getOrCreateClubRoundStats(clubId);
        return clubRoundStats.hasWouldHaveBeenSeedingData();
    }

    /**
     * Returns immutable matchup counts for a specific club.
     *
     * @param clubId club ID
     * @return map of opponent ID to matchup counts for the specified club
     */
    public Map<Integer, ClubRoundCounters> getOpponentCounts(int clubId) {
        ClubRoundStats clubRoundStats = getOrCreateClubRoundStats(clubId);
        return clubRoundStats.getOpponentCounts();
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
