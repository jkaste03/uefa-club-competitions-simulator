package com.github.jkaste03.uefaccsim.reporting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

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
            return pathType == null ? tournament + " " + roundType : tournament + " " + roundType + " " + pathType;
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
     * Checks if the round has seeding statistics.
     *
     * @param roundKey round's key
     * @return {@code true} if the round has seeding data
     */
    private boolean roundHasSeedingStats(RoundKey roundKey) {
        return getRoundStats(roundKey).hasSeedingStats();
    }

    // TODO: Remove the below temporary methods

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
        System.out.print(buildRoundStatsReport(tournament, roundType, pathType, clubName));
    }

    /**
     * Builds statistics for a club in a specific round as text.
     * <p>
     * If the round contains seeding data, an advanced display with seeding split
     * returned, otherwise simple matchup display is returned.
     *
     * @param tournament tournament
     * @param roundType  round type
     * @param pathType   path (may be {@code null} for rounds without path)
     * @param clubName   club name
     * @return formatted statistics text
     */
    public String buildRoundStatsReport(Tournament tournament, RoundType roundType, PathType pathType,
            String clubName) {
        StringBuilder report = new StringBuilder();
        RoundKey roundKey = new RoundKey(tournament, roundType, pathType);
        int clubId = ClubRepository.getIdByName(clubName);

        if (roundHasSeedingStats(roundKey)) {
            appendQRoundMatchupCounts(report, roundKey, clubName, clubId);
        } else {
            appendLeaguePhaseMatchupCounts(report, roundKey, clubName, clubId);
        }

        return report.toString();
    }

    /**
     * Appends matchup percentages for rounds without seeding split (typically
     * league phase rounds) to the report.
     *
     * @param report   target report buffer
     * @param roundKey round's key
     * @param clubName club name
     * @param clubId   club ID
     */
    private void appendLeaguePhaseMatchupCounts(StringBuilder report, RoundKey roundKey, String clubName,
            int clubId) {
        RoundStats roundStats = getRoundStats(roundKey);
        Map<Integer, Integer> club2Map = roundStats.getMatchupCounts(clubId);
        if (club2Map.isEmpty()) {
            return;
        }

        appendRoundHeader(report, roundKey);

        report.append("=== Matchup probabilities").append(" ===\n");
        club2Map.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> {
                    int club2Id = entry.getKey();
                    int count = entry.getValue();
                    float percentage = (count / (float) roundStats.getParticipationCount(clubId)) * 100;
                    report.append(String.format(Locale.US,
                            "%-21s: %5.2f%%%n",
                            ClubRepository.getClub(club2Id).getName(),
                            percentage));
                });
    }

    /**
     * Appends round statistics with seeding perspective to the report.
     *
     * @param report   target report buffer
     * @param roundKey round's key
     * @param clubName club name
     * @param clubId   club ID
     */
    private void appendQRoundMatchupCounts(StringBuilder report, RoundKey roundKey, String clubName, int clubId) {
        int seededCount = getSeedingCount(roundKey, true, clubId);
        int unseededCount = getSeedingCount(roundKey, false, clubId);
        RoundStats roundStats = getRoundStats(roundKey);

        if (!appendSeedingProbability(report, roundKey, clubName, clubId)) {
            return;
        }
        appendOverallMatchupProbabilities(report, roundKey, clubName, clubId);

        if (seededCount == 0 || unseededCount == 0
                || (roundStats.getPerSeedingParticipationCount(true, clubId)
                        + roundStats.getPerSeedingParticipationCount(false, clubId)) == 0) {
            return;
        }

        appendPerSeedingMatchupProbabilities(report, roundKey, true, clubName, clubId);
        appendPerSeedingMatchupProbabilities(report, roundKey, false, clubName, clubId);
    }

    /**
     * Appends to the report the total matchup percentages regardless of seeding
     * status.
     *
     * @param report   target report buffer
     * @param roundKey round's key
     * @param clubName club name
     * @param clubId   club ID
     */
    private void appendOverallMatchupProbabilities(StringBuilder report, RoundKey roundKey, String clubName,
            int clubId) {
        RoundStats roundStats = getRoundStats(roundKey);
        Map<Integer, Integer> seededMatchups = roundStats.getPerSeedingMatchupCounts(true, clubId);
        Map<Integer, Integer> unseededMatchups = roundStats.getPerSeedingMatchupCounts(false, clubId);

        Map<Integer, Integer> totalMatchups = new HashMap<>();
        seededMatchups.forEach((opponent, count) -> totalMatchups.merge(opponent, count, Integer::sum));
        unseededMatchups.forEach((opponent, count) -> totalMatchups.merge(opponent, count, Integer::sum));

        if (totalMatchups.isEmpty()) {
            report.append("\nNo matchups recorded\n");
            return;
        }

        int totalMatches = totalMatchups.values().stream().mapToInt(Integer::intValue).sum();
        report.append("\n=== Overall matchup probabilities ===\n");
        totalMatchups.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> {
                    int opponentId = entry.getKey();
                    int count = entry.getValue();
                    float percentage = (count / (float) totalMatches) * 100;
                    report.append(
                            String.format(Locale.US, "%-21s: %5.2f%%%n", ClubRepository.getClub(opponentId).getName(),
                                    percentage));
                });
    }

    /**
     * Appends the probability that the club was seeded to the report.
     *
     * @param report   target report buffer
     * @param roundKey round's key
     * @param clubName club name
     * @param clubId   club ID
     */
    private boolean appendSeedingProbability(StringBuilder report, RoundKey roundKey, String clubName, int clubId) {
        int seededCount = getSeedingCount(roundKey, true, clubId);
        int unseededCount = getSeedingCount(roundKey, false, clubId);
        int totalCount = seededCount + unseededCount;

        if (totalCount == 0) {
            return false;
        }
        appendRoundHeader(report, roundKey);

        float seedingProbability = (seededCount / (float) totalCount) * 100;
        report.append("\n=== Seeding probability ===\n");
        report.append(String.format(Locale.US, "Seeded:   %5.2f%%%n", seedingProbability));
        return true;
    }

    /**
     * Appends a clear separator before each round block to the report.
     *
     * @param report   target report buffer
     * @param roundKey round's key
     */
    private void appendRoundHeader(StringBuilder report, RoundKey roundKey) {
        report.append("\n\n");
        report.append("========================================\n");
        report.append(roundKey).append('\n');
        report.append("========================================\n\n");
    }

    /**
     * Appends matchup percentages for a given seeding status to the report.
     *
     * @param report   target report buffer
     * @param roundKey round's key
     * @param isSeeded {@code true} for seeded, {@code false} for unseeded
     * @param clubName club name
     * @param clubId   club ID
     */
    private void appendPerSeedingMatchupProbabilities(StringBuilder report, RoundKey roundKey, boolean isSeeded,
            String clubName,
            int clubId) {
        RoundStats roundStats = getRoundStats(roundKey);
        Map<Integer, Integer> matchups = roundStats.getPerSeedingMatchupCounts(isSeeded, clubId);

        if (matchups.isEmpty()) {
            report.append("\nNo matchups recorded when ").append(isSeeded ? "seeded" : "unseeded").append("\n");
            return;
        }

        int totalMatches = roundStats.getPerSeedingParticipationCount(isSeeded, clubId);

        String status = isSeeded ? "seeded" : "unseeded";
        report.append("\n=== Matchup probabilities when ").append(status).append(" ===\n");
        matchups.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> {
                    int opponentId = entry.getKey();
                    int count = entry.getValue();
                    float percentage = (count / (float) totalMatches) * 100;
                    report.append(
                            String.format(Locale.US, "%-21s: %5.2f%%%n", ClubRepository.getClub(opponentId).getName(),
                                    percentage));
                });
    }

    // TODO: Remove the above temporary methods

    @Override
    public String toString() {
        return "StatsAggregator [roundStatsByRound=" + roundStatsByRound + "]";
    }
}
