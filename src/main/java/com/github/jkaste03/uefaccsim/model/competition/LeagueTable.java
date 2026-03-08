package com.github.jkaste03.uefaccsim.model.competition;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Tracks match results and computes league phase standings for a UEFA
 * competition, applying the tiebreaker criteria (a–h) followed
 * by a coin flip when all other criteria (a–h) are equal.
 */
public class LeagueTable {
    /**
     * The number of clubs in the league phase, which determines the size of all
     * stat arrays and the standings order.
     */
    private final int n;
    // Maps club simulation-state ID -> internal club index.
    private final int[] idToIdx;
    // Sorted club indices representing current standings.
    private final Integer[] standingsOrder;

    // Opponent indices per club, used for opponent-based tiebreakers (f, g, h).
    private int[][] opponents;

    // Per-club aggregate stats used when ranking clubs.
    private final int[] points;
    private final int[] goalsScored;
    private final int[] goalsConceded;
    private final int[] awayGoalsScored;
    private final int[] wins;
    private final int[] awayWins;

    /**
     * Constructs a league table for the given clubs, initializing all stat arrays
     * and building a mapping from club simulation state IDs to internal indices.
     *
     * @param clubSlots the clubs participating in the league phase
     */
    public LeagueTable(List<ClubSlot> clubSlots) {
        this.n = clubSlots.size();
        standingsOrder = IntStream.range(0, n).boxed().toArray(Integer[]::new);
        // Build idToIdx mapping for quick lookups of club indices by their simulation
        // state IDs
        int maxId = 0;
        for (ClubSlot cs : clubSlots)
            maxId = Math.max(maxId, cs.getClubSimState().getId());
        this.idToIdx = new int[maxId + 1];
        Arrays.fill(idToIdx, -1);
        for (int i = 0; i < n; i++)
            idToIdx[clubSlots.get(i).getClubSimState().getId()] = i;

        // Allocate arrays for tracking points, goals, wins, etc. for each club in the
        // league phase.
        this.points = new int[n];
        this.goalsScored = new int[n];
        this.goalsConceded = new int[n];
        this.awayGoalsScored = new int[n];
        this.wins = new int[n];
        this.awayWins = new int[n];
    }

    /**
     * Records which opponents each club faces, enabling lazy computation of
     * opponent-based tiebreakers (f, g, h) during standings calculation.
     *
     * @param ties           the drawn ties defining all matchups
     * @param matchesPerClub the number of matches each club plays
     */
    public void recordOpponents(List<NonKnockoutTie> ties, int matchesPerClub) {
        this.opponents = new int[n][matchesPerClub];
        int[] matchCount = new int[n];
        for (NonKnockoutTie tie : ties) {
            int idxA = idToIdx[tie.getClubSlotA().getClubSimState().getId()];
            int idxB = idToIdx[tie.getClubSlotB().getClubSimState().getId()];
            opponents[idxA][matchCount[idxA]++] = idxB;
            opponents[idxB][matchCount[idxB]++] = idxA;
        }
    }

    /**
     * Registers a played match by updating points, goals and wins for both clubs.
     *
     * @param tie the played non-knockout tie to record
     */
    public void registerMatch(NonKnockoutTie tie) {
        int idxA = idToIdx[tie.getClubSlotA().getClubSimState().getId()];
        int idxB = idToIdx[tie.getClubSlotB().getClubSimState().getId()];
        int goalsA = tie.getClubAGoals1stLeg();
        int goalsB = tie.getClubBGoals1stLeg();

        updatePoints(idxA, goalsA, idxB, goalsB);
        updateGoals(idxA, goalsA, idxB, goalsB);
        updateWins(idxA, goalsA, idxB, goalsB);
    }

    /**
     * Awards 3 points for a win, 1 for a draw, and 0 for a loss to each club.
     *
     * @param idxA   internal index of the home club
     * @param goalsA goals scored by the home club
     * @param idxB   internal index of the away club
     * @param goalsB goals scored by the away club
     */
    private void updatePoints(int idxA, int goalsA, int idxB, int goalsB) {
        points[idxA] += goalsA > goalsB ? 3 : goalsA == goalsB ? 1 : 0;
        points[idxB] += goalsB > goalsA ? 3 : goalsB == goalsA ? 1 : 0;
    }

    /**
     * Updates goals scored, goals conceded, and away goals scored for both clubs.
     *
     * @param idxA   internal index of the home club
     * @param goalsA goals scored by the home club
     * @param idxB   internal index of the away club
     * @param goalsB goals scored by the away club
     */
    private void updateGoals(int idxA, int goalsA, int idxB, int goalsB) {
        goalsScored[idxA] += goalsA;
        goalsScored[idxB] += goalsB;
        goalsConceded[idxA] += goalsB;
        goalsConceded[idxB] += goalsA;
        awayGoalsScored[idxB] += goalsB;
    }

    /**
     * Increments the win counter for the winning club. If the away club wins,
     * also increments their away win counter.
     *
     * @param idxA   internal index of the home club
     * @param goalsA goals scored by the home club
     * @param idxB   internal index of the away club
     * @param goalsB goals scored by the away club
     */
    private void updateWins(int idxA, int goalsA, int idxB, int goalsB) {
        if (goalsA > goalsB) {
            wins[idxA]++;
        } else if (goalsB > goalsA) {
            wins[idxB]++;
            awayWins[idxB]++;
        }
    }

    /**
     * Sorts the standings by applying UEFA tiebreakers in order: points,
     * goal difference (a), goals scored (b), away goals scored (c), wins (d),
     * away wins (e), opponent points (f), opponent goal difference (g),
     * opponent goals scored (h), and finally a coin flip as last resort.
     * <p>
     * UEFA officially also includes disciplinary points (yellow/red cards) (i) and
     * club coefficient (j) as tiebreakers, but since the simulation does not track
     * cards, these are omitted and the coin flip is used immediately after (h).
     */
    public void calcStandings() {
        Arrays.sort(standingsOrder, (i1, i2) -> {
            if (points[i2] != points[i1])
                return Integer.compare(points[i2], points[i1]);
            // Tiebreakers:
            int gd1 = goalsScored[i1] - goalsConceded[i1];
            int gd2 = goalsScored[i2] - goalsConceded[i2];
            if (gd2 != gd1) // a
                return Integer.compare(gd2, gd1);
            if (goalsScored[i2] != goalsScored[i1]) // b
                return Integer.compare(goalsScored[i2], goalsScored[i1]);
            if (awayGoalsScored[i2] != awayGoalsScored[i1]) // c
                return Integer.compare(awayGoalsScored[i2], awayGoalsScored[i1]);
            if (wins[i2] != wins[i1]) // d
                return Integer.compare(wins[i2], wins[i1]);
            if (awayWins[i2] != awayWins[i1]) // e
                return Integer.compare(awayWins[i2], awayWins[i1]);
            // f: Higher number of points obtained collectively by league phase opponents
            int oppPts1 = calcOpponentPoints(i1), oppPts2 = calcOpponentPoints(i2);
            if (oppPts2 != oppPts1)
                return Integer.compare(oppPts2, oppPts1);
            // g: Superior collective goal difference of league phase opponents
            int oppGD1 = calcOpponentGoalDifference(i1), oppGD2 = calcOpponentGoalDifference(i2);
            if (oppGD2 != oppGD1)
                return Integer.compare(oppGD2, oppGD1);
            // h: Higher number of goals scored collectively by league phase opponents
            int oppGS1 = calcOpponentGoalsScored(i1), oppGS2 = calcOpponentGoalsScored(i2);
            if (oppGS2 != oppGS1)
                return Integer.compare(oppGS2, oppGS1);
            // i and j is not implemented, as yellow cars are not tracked in the simulation.
            // Last resort: coin flip
            return ThreadLocalRandom.current().nextInt(2) * 2 - 1;
        });
    }

    /**
     * Returns the sum of points of all opponents of the club at the given index.
     */
    private int calcOpponentPoints(int idx) {
        int sum = 0;
        for (int opp : opponents[idx])
            sum += points[opp];
        return sum;
    }

    /**
     * Returns the collective goal difference of all opponents of the club at the
     * given index.
     */
    private int calcOpponentGoalDifference(int idx) {
        int sum = 0;
        for (int opp : opponents[idx])
            sum += goalsScored[opp] - goalsConceded[opp];
        return sum;
    }

    /**
     * Returns the total goals scored by all opponents of the club at the given
     * index.
     */
    private int calcOpponentGoalsScored(int idx) {
        int sum = 0;
        for (int opp : opponents[idx])
            sum += goalsScored[opp];
        return sum;
    }

    @Override
    public String toString() {
        return "LeagueTable [n=" + n + ", standingsOrder=" + Arrays.toString(standingsOrder) + ", idToIdx="
                + Arrays.toString(idToIdx) + ", points=" + Arrays.toString(points) + ", goalsScored="
                + Arrays.toString(goalsScored) + ", goalsConceded=" + Arrays.toString(goalsConceded)
                + ", awayGoalsScored=" + Arrays.toString(awayGoalsScored) + ", wins=" + Arrays.toString(wins)
                + ", awayWins=" + Arrays.toString(awayWins) + "]";
    }
}
