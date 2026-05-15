package com.github.jkaste03.uefaccsim.reporting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jkaste03.uefaccsim.enums.PathType;
import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.model.competition.ClubSlot;
import com.github.jkaste03.uefaccsim.model.competition.Tie;
import com.github.jkaste03.uefaccsim.repository.ClubRepository;

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
            return tournament + " " + roundType + " " + pathType;
        }
    }

    private final Map<RoundKey, RoundStats> roundStatsByRound = new HashMap<>();

    /**
     * Returns the number of times a club was seeded/unseeded in a given round.
     *
     * @param roundKey round's key
     * @param isSeeded {@code true} for seeded, {@code false} for unseeded
     * @param clubId   club ID
     * @return number of occurrences for the selected seeding status
     */
    private int getSeedingCount(RoundKey roundKey, boolean isSeeded, int clubId) {
        RoundStats roundStats = getRoundStats(roundKey);
        return roundStats.getSeedingCount(isSeeded, clubId);
    }

    /**
     * Retrieves round statistics for a given key.
     *
     * @param roundKey round's key
     * @return recorded round statistics
     * @throws IllegalArgumentException if the key is not found
     */
    private RoundStats getRoundStats(RoundKey roundKey) {
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
    public void recordMatchup(RoundKey roundKey, List<? extends Tie> ties) {
        RoundStats roundStats = getOrCreateRoundStats(roundKey);
        roundStats.recordMatchup(ties);
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
    public void recordRoundStats(RoundKey roundKey, List<? extends Tie> ties, Set<ClubSlot> seeded) {
        RoundStats roundStats = getOrCreateRoundStats(roundKey);
        roundStats.recordRoundStats(ties, seeded);
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

    /**
     * Prints statistics for a club in a specific round.
     * <p>
     * If the round contains seeding data, an advanced display with seeding split is
     * printed, otherwise simple matchup display is printed.
     * </p>
     *
     * @param tournament tournament
     * @param roundType  round type
     * @param pathType   path (may be {@code null} for rounds without path)
     * @param clubName   club name
     */
    public void printRoundStats(Tournament tournament, RoundType roundType, PathType pathType,
            String clubName) {
        RoundKey roundKey = new RoundKey(tournament, roundType, pathType);
        int clubId = ClubRepository.getIdByName(clubName);

        if (roundHasSeedingStats(roundKey)) {
            printQRoundMatchupCounts(roundKey, clubName, clubId);
        } else {
            printLeaguePhaseMatchupCounts(roundKey, clubName, clubId);
        }
    }

    /**
     * Checks if the round has seeding statistics.
     *
     * @param roundKey round's key
     * @return {@code true} if the round has seeding data
     */
    private boolean roundHasSeedingStats(RoundKey roundKey) {
        return getRoundStats(roundKey).hasSeedingStats();
    }

    /**
     * Prints matchup percentages for rounds without seeding split (typically league
     * phase rounds).
     *
     * @param roundKey round's key
     * @param clubName club name
     * @param clubId   club ID
     */
    private void printLeaguePhaseMatchupCounts(RoundKey roundKey, String clubName, int clubId) {
        RoundStats roundStats = getRoundStats(roundKey);
        Map<Integer, Integer> club2Map = roundStats.getMatchupCounts(clubId);
        if (club2Map.isEmpty()) {
            System.out.println("No matchups recorded for " + roundKey);
            return;
        }

        System.out.println("Matchup counts for " + clubName + " in " + roundKey + ":");
        club2Map.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> {
                    int club2Id = entry.getKey();
                    int count = entry.getValue();
                    float percentage = (count / (float) roundStats.getParticipationCount(clubId)) * 100;
                    System.out.printf(
                            "%-21s: %5.2f%%%n",
                            ClubRepository.getClub(club2Id).getName(),
                            percentage);
                });
    }

    /**
     * Prints round statistics with seeding perspective.
     *
     * @param roundKey round's key
     * @param clubName club name
     * @param clubId   club ID
     */
    private void printQRoundMatchupCounts(RoundKey roundKey, String clubName, int clubId) {
        int seededCount = getSeedingCount(roundKey, true, clubId);
        int unseededCount = getSeedingCount(roundKey, false, clubId);

        printSeedingProbability(roundKey, clubName, clubId);
        printOverallMatchupProbabilities(roundKey, clubName, clubId);

        if (seededCount == 0 || unseededCount == 0) {
            return;
        }

        printPerSeedingMatchupProbabilities(roundKey, true, clubName, clubId);
        printPerSeedingMatchupProbabilities(roundKey, false, clubName, clubId);
    }

    /**
     * Prints total matchup percentages regardless of seeding status.
     *
     * @param roundKey round's key
     * @param clubName club name
     * @param clubId   club ID
     */
    private void printOverallMatchupProbabilities(RoundKey roundKey, String clubName, int clubId) {
        RoundStats roundStats = getRoundStats(roundKey);
        Map<Integer, Integer> seededMatchups = roundStats.getPerSeedingMatchupCounts(true, clubId);
        Map<Integer, Integer> unseededMatchups = roundStats.getPerSeedingMatchupCounts(false, clubId);

        Map<Integer, Integer> totalMatchups = new HashMap<>();
        seededMatchups.forEach((opponent, count) -> totalMatchups.merge(opponent, count, Integer::sum));
        unseededMatchups.forEach((opponent, count) -> totalMatchups.merge(opponent, count, Integer::sum));

        if (totalMatchups.isEmpty()) {
            System.out.println("No matchups recorded for " + clubName + " in " + roundKey);
            return;
        }

        int totalMatches = totalMatchups.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println("\n=== Overall Matchup Probabilities for " + clubName + " in " + roundKey + " ===");
        totalMatchups.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> {
                    int opponentId = entry.getKey();
                    int count = entry.getValue();
                    float percentage = (count / (float) totalMatches) * 100;
                    System.out.printf("%-21s: %5.2f%%%n", ClubRepository.getClub(opponentId).getName(),
                            percentage);
                });
    }

    /**
     * Prints the probability that the club was seeded.
     *
     * @param roundKey round's key
     * @param clubName club name
     * @param clubId   club ID
     */
    private void printSeedingProbability(RoundKey roundKey, String clubName, int clubId) {
        int seededCount = getSeedingCount(roundKey, true, clubId);
        int unseededCount = getSeedingCount(roundKey, false, clubId);
        int totalCount = seededCount + unseededCount;

        if (totalCount == 0) {
            System.out.println("No seeding data recorded for " + clubName + " in " + roundKey);
            return;
        }

        float seedingProbability = (seededCount / (float) totalCount) * 100;
        System.out.println("\n=== Seeding Probability for " + clubName + " in " + roundKey + " ===");
        System.out.printf("Seeded:   %5.2f%%%n", seedingProbability);
    }

    /**
     * Prints matchup percentages for a given seeding status.
     *
     * @param roundKey round's key
     * @param isSeeded {@code true} for seeded, {@code false} for unseeded
     * @param clubName club name
     * @param clubId   club ID
     */
    private void printPerSeedingMatchupProbabilities(RoundKey roundKey, boolean isSeeded, String clubName,
            int clubId) {
        RoundStats roundStats = getRoundStats(roundKey);
        Map<Integer, Integer> matchups = roundStats.getPerSeedingMatchupCounts(isSeeded, clubId);

        if (matchups.isEmpty()) {
            return;
        }

        int totalMatches = roundStats.getPerSeedingParticipationCount(isSeeded, clubId);

        String status = isSeeded ? "Seeded" : "Unseeded";
        System.out.println(
                "\n=== Matchup Probabilities when " + status + ": " + clubName + " in " + roundKey + " ===");
        matchups.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> {
                    int opponentId = entry.getKey();
                    int count = entry.getValue();
                    float percentage = (count / (float) totalMatches) * 100;
                    System.out.printf("%-21s: %5.2f%%%n", ClubRepository.getClub(opponentId).getName(),
                            percentage);
                });
    }
}
