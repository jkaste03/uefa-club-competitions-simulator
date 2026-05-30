package com.github.jkaste03.uefaccsim.reporting;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.github.jkaste03.uefaccsim.repository.ClubRepository;

/**
 * Formats {@link RoundStats} into human-readable report text for one club and
 * one round.
 * <p>
 * The formatter supports two presentation modes:
 * </p>
 * <ul>
 * <li>Normal style output (no seeding split), and</li>
 * <li>Qualifying style output with per seeding sections.</li>
 * </ul>
 * <p>
 * The class is stateless and can be safely reused across multiple calls.
 * </p>
 */
public final class RoundStatsReportFormatter {

    /**
     * Creates a formatted report for one club in one round.
     *
     * @param roundKey   identifies tournament, round type and optional path
     * @param roundStats aggregated statistics for the selected round
     * @param clubName   club name used to resolve club id and headings
     * @return formatted report text (possibly empty if no relevant data exists)
     */
    public String format(StatsAggregator.RoundKey roundKey, RoundStats roundStats, String clubName) {
        int clubId = ClubRepository.getIdByName(clubName);
        StringBuilder report = new StringBuilder();

        // Branch output style based on whether this round has seeding dimensions.
        if (roundStats.hasSeedingStats()) {
            appendQRoundMatchupCounts(report, roundKey, roundStats, clubName, clubId);
        } else {
            appendLeaguePhaseMatchupCounts(report, roundKey, roundStats, clubName, clubId);
        }

        return report.toString();
    }

    /**
     * Appends matchup probabilities for rounds that do not use seeding split
     * (typically league phase).
     *
     * @param report     target report buffer
     * @param roundKey   round identifier used in the section header
     * @param roundStats source statistics for the selected round
     * @param clubName   club name (kept for API consistency with other append
     *                   methods)
     * @param clubId     resolved club id for {@code clubName}
     */
    private void appendLeaguePhaseMatchupCounts(StringBuilder report, StatsAggregator.RoundKey roundKey,
            RoundStats roundStats, String clubName, int clubId) {
        Map<Integer, ClubRoundCounters> club2Map = new HashMap<>(roundStats.getOpponentCounts(clubId));
        club2Map.entrySet().removeIf(entry -> entry.getValue().getMatchups() == 0);
        if (club2Map.isEmpty()) {
            return;
        }

        appendRoundHeader(report, roundKey);

        report.append("=== Matchup probabilities ===\n");
        club2Map.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getMatchups(), e1.getValue().getMatchups()))
                .forEach(entry -> {
                    int club2Id = entry.getKey();
                    int count = entry.getValue().getMatchups();
                    float percentage = (count / (float) roundStats.getParticipationCount(clubId)) * 100;
                    report.append(String.format(Locale.US, "%-21s: %5.2f%%%n",
                            ClubRepository.getClub(club2Id).getName(), percentage));
                });
    }

    /**
     * Appends the full qualifying-style view, including seeding probability,
     * overall matchup distribution, and per-seeding matchup distributions.
     *
     * @param report     target report buffer
     * @param roundKey   round identifier used in section headers
     * @param roundStats source statistics for the selected round
     * @param clubName   club name (kept for symmetry with other helper signatures)
     * @param clubId     resolved club id for {@code clubName}
     */
    private void appendQRoundMatchupCounts(StringBuilder report, StatsAggregator.RoundKey roundKey,
            RoundStats roundStats, String clubName, int clubId) {
        int seededCount = roundStats.getSeedingCount(true, clubId);
        int unseededCount = roundStats.getSeedingCount(false, clubId);

        if (!appendSeedingProbability(report, roundKey, seededCount, unseededCount)) {
            return;
        }

        appendQRoundOverallMatchupProbabilities(report, roundKey, roundStats, clubId);

        if (seededCount == 0 || unseededCount == 0) {
            return;
        }

        appendPerSeedingMatchupProbabilities(report, roundKey, roundStats, true, clubName, clubId);
        appendPerSeedingMatchupProbabilities(report, roundKey, roundStats, false, clubName, clubId);
    }

    /**
     * Appends overall matchup probabilities in a QRound independent of seeding
     * state.
     *
     * @param report     target report buffer
     * @param roundKey   round identifier (not used directly, retained for signature
     *                   consistency)
     * @param roundStats source statistics for the selected round
     * @param clubId     resolved club id
     */
    private void appendQRoundOverallMatchupProbabilities(StringBuilder report, StatsAggregator.RoundKey roundKey,
            RoundStats roundStats, int clubId) {
        Map<Integer, ClubRoundCounters> opponentCounts = new HashMap<>(roundStats.getOpponentCounts(clubId));

        Map<Integer, Integer> totalMatchups = new HashMap<>();
        opponentCounts.forEach(
                (opponent, counters) -> totalMatchups.merge(opponent, counters.getSeededMatchups(), Integer::sum));
        opponentCounts.forEach(
                (opponent, counters) -> totalMatchups.merge(opponent, counters.getUnseededMatchups(), Integer::sum));

        totalMatchups.entrySet().removeIf(entry -> entry.getValue() == 0);
        if (totalMatchups.isEmpty()) {
            report.append("\nNo matchups recorded\n");
            return;
        }

        // Normalize each opponent's count by total matchup occurrences for the club.
        int totalMatches = totalMatchups.values().stream().mapToInt(Integer::intValue).sum();
        report.append("\n=== Overall matchup probabilities ===\n");
        totalMatchups.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> {
                    int opponentId = entry.getKey();
                    int count = entry.getValue();
                    float percentage = (count / (float) totalMatches) * 100;
                    report.append(String.format(Locale.US, "%-21s: %5.2f%%%n",
                            ClubRepository.getClub(opponentId).getName(), percentage));
                });
    }

    /**
     * Appends the probability section for the club being seeded in the selected
     * round.
     *
     * @param report        target report buffer
     * @param roundKey      round identifier used in the section header
     * @param seededCount   number of simulations where the club was seeded
     * @param unseededCount number of simulations where the club was unseeded
     * @return {@code true} if the section was appended, {@code false} when no
     *         seeding samples exist
     */
    private boolean appendSeedingProbability(StringBuilder report, StatsAggregator.RoundKey roundKey,
            int seededCount, int unseededCount) {
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
     * Appends a common visual header for one round section.
     *
     * @param report   target report buffer
     * @param roundKey round identifier to print
     */
    private void appendRoundHeader(StringBuilder report, StatsAggregator.RoundKey roundKey) {
        report.append("\n\n");
        report.append("========================================\n");
        report.append(roundKey).append('\n');
        report.append("========================================\n\n");
    }

    /**
     * Appends matchup probabilities for one seeding perspective (seeded or
     * unseeded).
     *
     * @param report     target report buffer
     * @param roundKey   round identifier (not used directly, retained for signature
     *                   consistency)
     * @param roundStats source statistics for the selected round
     * @param isSeeded   {@code true} for seeded perspective, {@code false} for
     *                   unseeded perspective
     * @param clubName   club name (kept for symmetry with sibling helpers)
     * @param clubId     resolved club id
     */
    private void appendPerSeedingMatchupProbabilities(StringBuilder report, StatsAggregator.RoundKey roundKey,
            RoundStats roundStats, boolean isSeeded, String clubName, int clubId) {
        Map<Integer, ClubRoundCounters> opponentCounts = new HashMap<>(roundStats.getOpponentCounts(clubId));

        String status = isSeeded ? "seeded" : "unseeded";
        opponentCounts.entrySet().removeIf(entry -> {
            ClubRoundCounters counters = entry.getValue();
            return isSeeded ? counters.getSeededMatchups() == 0 : counters.getUnseededMatchups() == 0;
        });
        if (opponentCounts.isEmpty()) {
            report.append("\nNo matchups recorded when ").append(status).append("\n");
            return;
        }

        // Use the seeding-specific participation count as denominator for percentages.
        int totalMatches = roundStats.getPerSeedingParticipationCount(isSeeded, clubId);

        report.append("\n=== Matchup probabilities when ").append(status).append(" ===\n");
        opponentCounts.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(
                        isSeeded ? e2.getValue().getSeededMatchups() : e2.getValue().getUnseededMatchups(),
                        isSeeded ? e1.getValue().getSeededMatchups() : e1.getValue().getUnseededMatchups()))
                .forEach(entry -> {
                    int opponentId = entry.getKey();
                    int count = isSeeded ? entry.getValue().getSeededMatchups()
                            : entry.getValue().getUnseededMatchups();
                    float percentage = (count / (float) totalMatches) * 100;
                    report.append(String.format(Locale.US, "%-21s: %5.2f%%%n",
                            ClubRepository.getClub(opponentId).getName(), percentage));
                });
    }
}