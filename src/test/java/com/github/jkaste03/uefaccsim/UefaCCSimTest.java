package com.github.jkaste03.uefaccsim;

// Removed per-request: new baseline per repetition. We now create baseline once.
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.github.jkaste03.uefaccsim.enums.Country;
import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.model.ClubIdWrapper;
import com.github.jkaste03.uefaccsim.model.ClubSlot;
import com.github.jkaste03.uefaccsim.model.LeaguePhaseRound;
import com.github.jkaste03.uefaccsim.model.QRound;
import com.github.jkaste03.uefaccsim.model.Round;
import com.github.jkaste03.uefaccsim.model.Rounds;
import com.github.jkaste03.uefaccsim.model.SingleLeggedTie;
import com.github.jkaste03.uefaccsim.model.Tie;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class contains unit tests for the UefaCCSim class.
 */
@Execution(ExecutionMode.CONCURRENT) // Allow concurrent execution of repeated tests (if enabled globally)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // So we can use non-static @BeforeAll and share baseline
public class UefaCCSimTest {

    private static final int REPETITIONS = 40; // Tune for speed vs. coverage
    private Rounds baseline; // Single baseline created once

    @BeforeAll
    void initBaseline() {
        // Construct baseline once; all repetitions deep-copy this immutable template
        baseline = new Rounds();
    }

    // Helper: run one simulation starting from a deep copy of the baseline
    private Rounds simulateOnce(String label) {
        Rounds copy = UefaCCSim.deepCopy(baseline);
        copy.run(label);
        return copy;
    }

    @RepeatedTest(REPETITIONS)
    void qualifyingRoundsShouldHaveNoIllegalTies(RepetitionInfo repInfo) {
        Rounds sim = simulateOnce("QR-" + repInfo.getCurrentRepetition());
        sim.getRounds().stream()
                .filter(r -> r instanceof QRound)
                .forEach(r -> checkNoIllegalTies(r, ((QRound) r).getTies()));
    }

    @RepeatedTest(REPETITIONS)
    void leaguePhaseConstraintsHold(RepetitionInfo repInfo) {
        Rounds sim = simulateOnce("LP-" + repInfo.getCurrentRepetition());
        for (Round r : sim.getRoundsOfType(RoundType.LEAGUE_PHASE)) {
            LeaguePhaseRound lp = (LeaguePhaseRound) r;
            if (lp.getTournament() == Tournament.CONFERENCE_LEAGUE) {
                continue; // Skip per original logic (you handle UECL separately later)
            }
            List<LeaguePhaseRound.Pot> pots = lp.getPots();
            List<SingleLeggedTie> ties = lp.getTies();
            List<ClubSlot> clubSlots = lp.getClubSlots();

            // NEW: verify pot.index() is 0-based and covers full range [0..pots.size()-1]
            int minIndex = pots.stream().mapToInt(LeaguePhaseRound.Pot::index).min().orElse(-1);
            int maxIndex = pots.stream().mapToInt(LeaguePhaseRound.Pot::index).max().orElse(-1);
            assertEquals(0, minIndex, "Pot index should be 0-based (min index is " + minIndex + ")");
            assertEquals(pots.size() - 1, maxIndex,

                    "Pot indices should span 0..pots.size()-1 (max is " + maxIndex + " for pots.size()=" + pots.size()
                            + ")");

            // existing checks
            checkOpponentPotHomeAway(pots, clubSlots, ties);
            checkNoIllegalTies(lp, ties);
            checkNoClubMeetsCountryMoreThanTwice(clubSlots, ties);

            // NEW: additional consistency checks (non-overlapping with above)
            // - unique opponents per club (ensures correct count of distinct opponents)
            // - no duplicate ties (A-B and B-A counted twice)
            checkEachClubHasExpectedUniqueOpponents(lp, ties);
            checkNoDuplicateTiesById(ties);
        }
    }

    @Test
    void deepCopyIsolation() {
        // Stronger deep copy isolation test:
        // take a deep copy, mutate the copy, ensure baseline remains unchanged.
        Rounds copy = UefaCCSim.deepCopy(baseline);

        // snapshot baseline sizes (defensive - could be empty)
        int baselineRounds = baseline.getRounds().size();
        Integer baselineFirstRoundClubSlotsSize = null;
        if (baselineRounds > 0 && !baseline.getRounds().get(0).getClubSlots().isEmpty()) {
            baselineFirstRoundClubSlotsSize = baseline.getRounds().get(0).getClubSlots().size();
        }

        //
        // Mutate copy if possible (best-effort; if underlying lists are unmodifiable
        // this will throw)
        if (copy.getRounds().size() > 0 && !copy.getRounds().get(0).getClubSlots().isEmpty()) {
            copy.getRounds().get(0).getClubSlots().remove(0);
        }

        // Baseline must remain unchanged
        assertEquals(baselineRounds, baseline.getRounds().size(), "Baseline rounds count mutated");
        if (baselineFirstRoundClubSlotsSize != null) {
            assertEquals(baselineFirstRoundClubSlotsSize.intValue(),
                    baseline.getRounds().get(0).getClubSlots().size(),
                    "Deep copy mutation affected baseline clubSlots");
        }
    }

    /**
     * Checks that each club meets exactly one club from each pot at home and one
     * away, with specific rules for 6 pots.
     *
     * @param clubSlots the list of club slots participating in the competition
     * @param ties      the list of ties between the clubs
     * @throws AssertionError if a club does not meet exactly one club from each pot
     *                        at home and one away
     */
    private void checkOpponentPotHomeAway(List<LeaguePhaseRound.Pot> pots, List<ClubSlot> clubSlots,
            List<SingleLeggedTie> ties) {
        // Precompute pot index for O(1) lookups
        //
        // Note: pot.index() is verified to be 0-based above; we map to 1-based array
        // indices below.
        Map<ClubSlot, Integer> potIndex = pots.stream()
                .flatMap(p -> p.clubs().stream().map(cs -> Map.entry(cs, p.index() + 1)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (ClubSlot clubSlot : clubSlots) {
            // Count home/away vs pot in one pass
            int potCount = pots.size();
            int[] homeCounts = new int[potCount + 1]; // 1-based
            int[] awayCounts = new int[potCount + 1];
            for (SingleLeggedTie tie : ties) {
                // You stated clubSlot1 is always home -> we rely on that invariant here
                if (tie.getClubSlot1().equals(clubSlot)) {
                    int oppPot = potIndex.get(tie.getClubSlot2());
                    homeCounts[oppPot]++;
                } else if (tie.getClubSlot2().equals(clubSlot)) {
                    int oppPot = potIndex.get(tie.getClubSlot1());
                    awayCounts[oppPot]++;
                }
            }

            if (potCount == 6) {
                // Convert arrays to maps-on-demand for existing helper reuse
                Map<Integer, Long> homePotCounts = intArrayToCountMap(homeCounts);
                Map<Integer, Long> awayPotCounts = intArrayToCountMap(awayCounts);
                checkPotSubpotHomeAway(homePotCounts, awayPotCounts, clubSlot, 1, 2);
                checkPotSubpotHomeAway(homePotCounts, awayPotCounts, clubSlot, 3, 4);
                checkPotSubpotHomeAway(homePotCounts, awayPotCounts, clubSlot, 5, 6);
            } else {
                for (int pot = 1; pot <= potCount; pot++) {
                    assertEquals(1, homeCounts[pot],
                            "Home pot count mismatch for " + clubSlot + " pot=" + pot + " ties=" + ties);
                    assertEquals(1, awayCounts[pot],
                            "Away pot count mismatch for " + clubSlot + " pot=" + pot + " ties=" + ties);
                }
            }
        }
    }

    private Map<Integer, Long> intArrayToCountMap(int[] counts) {
        return java.util.stream.IntStream.range(1, counts.length)
                .boxed()
                .collect(Collectors.toMap(i -> i, i -> (long) counts[i]));
    }

    /**
     * Ensures a club meets exactly one club from each sub pot at home and one
     * away, and does not meet clubs from the same pot.
     *
     * @param homePotCounts a map containing the counts of home matches against each
     *                      pot
     * @param awayPotCounts a map containing the counts of away matches against each
     *                      pot
     * @param clubSlot      the club slot being checked
     * @param potX          the first pot to check
     * @param potY          the second pot to check
     */
    private void checkPotSubpotHomeAway(Map<Integer, Long> homePotCounts, Map<Integer, Long> awayPotCounts,
            ClubSlot clubSlot,
            int potX, int potY) {
        // Retrieve the home and away match counts against pots 1 and 2
        long homeCountPot1 = homePotCounts.getOrDefault(potX, 0L);
        long homeCountPot2 = homePotCounts.getOrDefault(potY, 0L);
        long awayCountPot1 = awayPotCounts.getOrDefault(potX, 0L);
        long awayCountPot2 = awayPotCounts.getOrDefault(potY, 0L);

        // Check that the club meets exactly one club from pots 1 or 2 at home and one
        // away
        assertEquals(1, homeCountPot1 + homeCountPot2,
                "ClubSlot " + clubSlot + " must meet exactly one from pots " + potX + "/" + potY + " at home");
        assertEquals(1, awayCountPot1 + awayCountPot2,
                "ClubSlot " + clubSlot + " must meet exactly one from pots " + potX + "/" + potY + " away");

        // Check that the club does not meet clubs from same pot home and away
        assertTrue(homeCountPot1 == 0 || awayCountPot1 == 0,
                "ClubSlot " + clubSlot + " meets clubs from pot " + potX + " both home and away");
        assertTrue(homeCountPot2 == 0 || awayCountPot2 == 0,
                "ClubSlot " + clubSlot + " meets clubs from pot " + potY + " both home and away");
    }

    /**
     * Checks that no illegal ties are present.
     *
     * @param clubSlots the list of club slots participating in the competition
     * @param ties      the list of ties between the clubs
     * @throws AssertionError if a club meets a club from its own country or if
     *                        there are illegal ties
     */
    private void checkNoIllegalTies(Round round, List<? extends Tie> ties) {
        for (Tie tie : ties) {
            ClubSlot c1 = tie.getClubSlot1();
            ClubSlot c2 = tie.getClubSlot2();
            assertFalse(round.isIllegalTie(c1, c2),
                    () -> "Illegal tie: " + c1 + " vs " + c2 + " in round " + round.getName());
        }
    }

    /**
     * Checks that no club meets more than two clubs from any specific country.
     *
     * @param clubSlots the list of club slots participating in the competition
     * @param ties      the list of ties between the clubs
     * @throws AssertionError if a club meets more than two clubs from the same
     *                        country
     */
    private void checkNoClubMeetsCountryMoreThanTwice(List<ClubSlot> clubSlots, List<SingleLeggedTie> ties) {
        for (ClubSlot clubSlot : clubSlots) {
            // Create a map to count the number of opponents from each country
            Map<Country, Long> opponentCountryCounts = ties.stream()
                    // Filter ties to include only those involving the current club slot
                    .filter(tie -> tie.getClubSlot1().equals(clubSlot) || tie.getClubSlot2().equals(clubSlot))
                    // Map each tie to the opponent club slot
                    .map(tie -> tie.getClubSlot1().equals(clubSlot) ? tie.getClubSlot2() : tie.getClubSlot1())
                    // Group the opponents by their country and count the occurrences
                    .collect(Collectors.groupingBy(opp -> opp.getCountries().get(0), Collectors.counting()));

            // Check that no club meets more than two clubs from any specific country
            for (Map.Entry<Country, Long> entry : opponentCountryCounts.entrySet()) {
                assertTrue(entry.getValue() <= 2,
                        "ClubSlot " + clubSlot + " meets >2 clubs from " + entry.getKey() + " ties=" + ties);
            }
        }
    }

    // ---------------------- NEW helper checks ----------------------

    /**
     * Ensures each club has the expected number of unique opponents (8 for 4-pot,
     * 6 for 6-pot).
     */
    private void checkEachClubHasExpectedUniqueOpponents(LeaguePhaseRound lp, List<SingleLeggedTie> ties) {
        int potsCount = lp.getPots().size();
        int expectedOpponents = (potsCount == 6) ? 6 : 2 * potsCount; // 6-pot special case, otherwise 2 per pot
        for (ClubSlot club : lp.getClubSlots()) {
            Set<ClubSlot> opponents = ties.stream()
                    .filter(t -> t.getClubSlot1().equals(club) || t.getClubSlot2().equals(club))
                    .map(t -> t.getClubSlot1().equals(club) ? t.getClubSlot2() : t.getClubSlot1())
                    .collect(Collectors.toSet());
            assertEquals(expectedOpponents, opponents.size(),

                    "Club " + club + " should have " + expectedOpponents + " unique opponents, got "
                            + opponents.size());
        }
    }

    /**
     * Verifies there are no duplicate ties (A-B and B-A counted separately) using
     * club IDs (assumes ClubSlot.getId() exists and is unique per club).
     */
    private void checkNoDuplicateTiesById(List<SingleLeggedTie> ties) {
        Set<Set<Object>> seenPairs = new HashSet<>();
        for (SingleLeggedTie t : ties) {
            ClubIdWrapper a = t.getClubSlot1().getClubIdWrapper();
            ClubIdWrapper b = t.getClubSlot2().getClubIdWrapper();
            // Use club unique id for comparison (assumption: getId() exists and is unique)
            Object aId = a.id();
            Object bId = b.id();
            Set<Object> pair = new HashSet<>(Arrays.asList(aId, bId)); // unordered pair of ids
            assertFalse(seenPairs.contains(pair), "Duplicate tie found for pair: " + pair);
            seenPairs.add(pair);
        }
    }

    // -----------------------------------------------------------------
}
