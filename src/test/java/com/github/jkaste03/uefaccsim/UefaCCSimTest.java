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
import com.github.jkaste03.uefaccsim.model.ClubSlot;
import com.github.jkaste03.uefaccsim.model.LeaguePhaseRound;
import com.github.jkaste03.uefaccsim.model.QRound;
import com.github.jkaste03.uefaccsim.model.Round;
import com.github.jkaste03.uefaccsim.model.Rounds;
import com.github.jkaste03.uefaccsim.model.SingleLeggedTie;
import com.github.jkaste03.uefaccsim.model.Tie;

import java.util.List;
import java.util.Map;
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
                continue; // Skip per original logic
            }
            List<LeaguePhaseRound.Pot> pots = lp.getPots();
            List<SingleLeggedTie> ties = lp.getTies();
            List<ClubSlot> clubSlots = lp.getClubSlots();
            checkOpponentPotHomeAway(pots, clubSlots, ties);
            checkNoIllegalTies(lp, ties);
            checkNoClubMeetsCountryMoreThanTwice(clubSlots, ties);
        }
    }

    @Test
    void deepCopyIsolation() {
        // Snapshot a property from baseline (size of first round's clubSlots)
        int originalSize = baseline.getRounds().get(0).getClubSlots().size();
        Rounds copy = UefaCCSim.deepCopy(baseline);
        copy.run("isolation");
        // Baseline must remain unchanged in its first round clubSlots size
        assertEquals(originalSize, baseline.getRounds().get(0).getClubSlots().size(),
                "Deep copy simulation mutated baseline state");
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
        Map<ClubSlot, Integer> potIndex = pots.stream()
                .flatMap(p -> p.clubs().stream().map(cs -> Map.entry(cs, p.index() + 1)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (ClubSlot clubSlot : clubSlots) {
            // Count home/away vs pot in one pass
            int potCount = pots.size();
            int[] homeCounts = new int[potCount + 1]; // 1-based
            int[] awayCounts = new int[potCount + 1];
            for (SingleLeggedTie tie : ties) {
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
     * Helper method to get the pot number for a given club slot.
     *
     * @param clubSlot the club slot to find the pot for
     * @param pots     the list of lists with club slots
     * @return the pot number for the given club slot
     */
    // Removed obsolete linear pot lookup method after optimization.

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
}