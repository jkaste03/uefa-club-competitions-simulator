package com.github.jkaste03.uefaccsim.reporting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jkaste03.uefaccsim.enums.PathType;
import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.model.competition.ClubSlot;
import com.github.jkaste03.uefaccsim.model.competition.KnockoutTie;
import com.github.jkaste03.uefaccsim.model.competition.Tie;

/**
 * Aggregator for simulation statistics across rounds.
 * <p>
 * Statistics are stored per {@link RoundKey}, with each key pointing to a
 * {@link RoundStats} instance. The class is used for both ongoing recording
 * and final reporting after multiple simulations have been run.
 * </p>
 */
public class StatsAggregator {

    /**
     * Unique key for a round in the statistics.
     *
     * @param tournament tournament
     * @param roundType  round type
     * @param pathType   qualifying/tournament path (may be {@code null})
     */
    public record RoundKey(Tournament tournament, RoundType roundType, PathType pathType) {
        /**
         * Returns a readable representation of the round key.
         *
         * @return text representation of the key
         */
        @Override
        public String toString() {
            return pathType == null ? tournament + " " + roundType : tournament + " " + roundType + " " + pathType;
        }
    }

    private final Map<RoundKey, RoundStats> roundStatsByRound = new HashMap<>();

    /**
     * Returns round statistics for a given key or throws an exception if not found.
     *
     * @param roundKey round's key
     * @return round statistics for the given key
     * @throws IllegalArgumentException if the round key is not found
     */
    RoundStats getRoundStatsOrThrow(RoundKey roundKey) {
        RoundStats roundStats = roundStatsByRound.get(roundKey);
        if (roundStats == null) {
            throw new IllegalArgumentException("Round key " + roundKey + " not found in stats.");
        }
        return roundStats;
    }

    /**
     * Records matchup data for a round.
     *
     * @param roundKey round's key
     * @param ties     ties to be recorded
     */
    public void recordMatchups(RoundKey roundKey, List<? extends Tie> ties) {
        RoundStats roundStats = getOrCreateRoundStats(roundKey);
        roundStats.recordMatchups(ties);
    }

    /**
     * Records matchup data for a league phase round and registers matches-per-club
     * so participation counts can be found based on matchup counts.
     *
     * @param roundKey       round's key
     * @param ties           ties to be recorded
     * @param matchesPerClub number of league-phase matches each club plays
     */
    public void recordLeaguePhaseMatchup(RoundKey roundKey, List<? extends Tie> ties, int matchesPerClub) {
        RoundStats roundStats = getOrCreateRoundStats(roundKey);
        roundStats.setMatchesPerClubIfAbsent(matchesPerClub);
        roundStats.recordMatchups(ties);
    }

    /**
     * Records seeded/unseeded data for a round.
     *
     * @param roundKey round's key
     * @param seeded   seeded slots
     * @param unseeded unseeded slots
     */
    public void recordSeeding(RoundKey roundKey, Set<ClubSlot> seeded, Set<ClubSlot> unseeded) {
        RoundStats roundStats = getOrCreateRoundStats(roundKey);
        roundStats.recordSeeding(seeded, unseeded);
    }

    /**
     * Records matchup data for a round with seeding context.
     *
     * @param roundKey round's key
     * @param ties     ties to be recorded
     * @param seeded   set of slots that were seeded
     */
    public void recordPerSeedingMatchups(RoundKey roundKey, List<? extends Tie> ties, Set<ClubSlot> seeded) {
        RoundStats roundStats = getOrCreateRoundStats(roundKey);
        roundStats.recordPerSeedingMatchups(ties, seeded);
    }

    /**
     * Records "would have been" matchup data for a round with seeding context.
     * <p>
     * "Would have been" matchups refer to matchups that would have happened had not
     * clubs been eliminated in the previous round. This is relevant for certain
     * statistics that consider the potential matchups, even if those matchups did
     * not actually occur due to eliminations.
     *
     * @param roundKey
     * @param ties
     * @param seeded
     */
    public void recordPerSeedingWouldHaveBeenMatchups(RoundKey roundKey, List<KnockoutTie> ties, Set<ClubSlot> seeded) {
        RoundStats roundStats = getOrCreateRoundStats(roundKey);
        roundStats.recordPerSeedingWouldHaveBeenMatchups(ties, seeded);
    }

    /**
     * Returns existing round statistics or creates a new one if needed.
     *
     * @param roundKey round's key
     * @return existing or newly created round statistics
     */
    private RoundStats getOrCreateRoundStats(RoundKey roundKey) {
        return roundStatsByRound.computeIfAbsent(roundKey, key -> new RoundStats());
    }

    /**
     * Merges statistics from another aggregator into this one.
     *
     * @param other other aggregator with data to be merged
     */
    public void mergeFrom(StatsAggregator other) {
        other.roundStatsByRound
                .forEach((roundKey, otherRoundStats) -> getOrCreateRoundStats(roundKey).mergeFrom(otherRoundStats));
    }

    @Override
    public String toString() {
        return "StatsAggregator [roundStatsByRound=" + roundStatsByRound + "]";
    }
}
