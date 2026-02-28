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
import com.github.jkaste03.uefaccsim.model.competition.ClubSimState;
import com.github.jkaste03.uefaccsim.model.competition.ClubSlot;
import com.github.jkaste03.uefaccsim.model.competition.LeaguePhaseRound;
import com.github.jkaste03.uefaccsim.model.competition.NonKnockoutTie;
import com.github.jkaste03.uefaccsim.model.competition.QRound;
import com.github.jkaste03.uefaccsim.model.competition.Round;
import com.github.jkaste03.uefaccsim.model.competition.Rounds;
import com.github.jkaste03.uefaccsim.model.competition.Tie;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Probabilistic and structural tests for {@link UefaCCSim} focusing on:
 * <ul>
 * <li>Qualifying rounds: no illegal (country / political) pairings.</li>
 * <li>League phase: pot indexing integrity, opponent distribution home/away,
 * country limits, uniqueness of opponents and absence of duplicate ties.</li>
 * <li>Deep copy behavior: simulation copies must not mutate the shared
 * baseline.</li>
 * </ul>
 * The tests are deliberately repeated ({@value #REPETITIONS} times) to increase
 * the
 * chance of detecting stochastic assignment bugs in draw logic.
 */
@Execution(ExecutionMode.CONCURRENT) // Allow concurrent execution of repeated tests (if enabled globally)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // So we can use non-static @BeforeAll and share baseline
public class UefaCCSimTest {

    /**
     * Number of repetitions for stochastic validation (tune for speed vs.
     * coverage).
     */
    private static final int REPETITIONS = 50;

    /**
     * Immutable (conceptually) baseline used only as a template for deep copies.
     */
    private Rounds baseline;

    /**
     * Builds a single baseline graph of rounds once. Each test iteration then
     * works on a deep copy to ensure isolation and determinism of starting state.
     */
    @BeforeAll
    void initBaseline() {
        // Construct baseline once; all repetitions deep-copy this template.
        baseline = new Rounds();
    }

    /**
     * Performs a complete simulation run on a freshly deep-copied baseline.
     *
     * @param label tag forwarded to the simulation for traceability/logging
     * @return the mutated {@link Rounds} instance that resulted from the run
     */
    private Rounds simulateOnce(String label) {
        Rounds copy = UefaCCSim.deepCopy(baseline);
        copy.run(label);
        return copy;
    }

    /**
     * Ensures every qualifying round instance contains only legal ties across
     * repeated probabilistic draws.
     *
     * @param repInfo repetition metadata injected by JUnit
     */
    @RepeatedTest(REPETITIONS)
    void qualifyingRoundsShouldHaveNoIllegalTies(RepetitionInfo repInfo) {
        Rounds sim = simulateOnce("QR-" + repInfo.getCurrentRepetition());
        sim.getRounds().stream()
                .filter(r -> r instanceof QRound)
                .forEach(r -> checkNoIllegalTies(r, ((QRound) r).getTies()));
    }

    /**
     * Validates several invariants for every league phase simulation run:
     * <ol>
     * <li>Pots are consecutively 0-indexed.</li>
     * <li>Opponent distribution per pot (home/away) matches competition
     * format.</li>
     * <li>No illegal ties.</li>
     * <li>No club faces more than two opponents from the same country.</li>
     * <li>Unique opponents is satisfied.</li>
     * <li>No duplicate unordered pairs of clubs.</li>
     * </ol>
     */
    @RepeatedTest(REPETITIONS)
    void leaguePhaseConstraintsHold(RepetitionInfo repInfo) {
        Rounds sim = simulateOnce("LP-" + repInfo.getCurrentRepetition());
        for (Round r : sim.getRoundsOfType(RoundType.LEAGUE_PHASE)) {
            LeaguePhaseRound lp = (LeaguePhaseRound) r;
            // if (lp.getTournament() == Tournament.CONFERENCE_LEAGUE) {
            // continue; // Skip per original logic (you handle UECL separately later)
            // }
            List<LeaguePhaseRound.Pot> pots = lp.getPots();
            List<NonKnockoutTie> ties = lp.getTies();
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
            checkExactlyOneMatchPerClubPerMatchDay(lp, ties);

            // NEW: additional consistency checks (non-overlapping with above)
            // - unique opponents per club (ensures correct count of distinct opponents)
            // - no duplicate ties (A-B and B-A counted twice)
            checkEachClubHasExpectedUniqueOpponents(lp, ties);
            checkNoDuplicateTiesById(ties);
        }
    }

    /**
     * Verifies that {@link UefaCCSim#deepCopy(Object)} produces a structure whose
     * mutation does not propagate back to the original baseline object graph.
     */
    @Test
    void deepCopyIsolation() {
        Rounds copy = UefaCCSim.deepCopy(baseline); // create isolated copy

        // Snapshot original sizes for later comparison (defensive against empties)
        int baselineRounds = baseline.getRounds().size();
        Integer baselineFirstRoundClubSlotsSize = null;
        if (baselineRounds > 0 && !baseline.getRounds().get(0).getClubSlots().isEmpty()) {
            baselineFirstRoundClubSlotsSize = baseline.getRounds().get(0).getClubSlots().size();
        }

        // Try mutating copy (if lists are unmodifiable this will raise and fail the
        // test — acceptable)
        if (copy.getRounds().size() > 0 && !copy.getRounds().get(0).getClubSlots().isEmpty()) {
            copy.getRounds().get(0).getClubSlots().remove(0);
        }

        // Assert isolation
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
     * @param pots      ordered list of league phase pots
     * @param clubSlots participating club slots
     * @param ties      scheduled ties
     * @throws AssertionError if a club does not meet exactly one club from each pot
     *                        at home and one away
     */
    private void checkOpponentPotHomeAway(List<LeaguePhaseRound.Pot> pots, List<ClubSlot> clubSlots,
            List<NonKnockoutTie> ties) {
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
            for (Tie tie : ties) {
                // ClubSlotA is always home
                if (tie.getClubSlotA() == clubSlot) {
                    int oppPot = potIndex.get(tie.getClubSlotB());
                    homeCounts[oppPot]++;
                } else if (tie.getClubSlotB() == clubSlot) {
                    int oppPot = potIndex.get(tie.getClubSlotA());
                    awayCounts[oppPot]++;
                }
            }
            // If Conference League
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

    /**
     * Converts a 1-based counting array to a map view (index -> count).
     * Index 0 is ignored intentionally.
     *
     * @param counts counting array where position i stores occurrences for i
     * @return map of index to count
     */
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
     * Asserts every tie in the specified round satisfies the round's legality
     * predicate.
     *
     * @param round round providing legality rules
     * @param ties  ties to validate
     * @throws AssertionError if a club meets a club from its own country or if
     *                        there are illegal ties
     */
    private void checkNoIllegalTies(Round round, List<? extends Tie> ties) {
        for (Tie tie : ties) {
            ClubSlot cA = tie.getClubSlotA();
            ClubSlot cB = tie.getClubSlotB();
            assertFalse(round.isIllegalTie(cA, cB),
                    () -> "Illegal tie: " + cA + " vs " + cB + " in round " + round.getName());
        }
    }

    /**
     * Ensures that no club exceeds facing more than two opponents from the same
     * country within the supplied tie set.
     *
     * @param clubSlots participants
     * @param ties      ties to inspect
     * @throws AssertionError if a club meets more than two clubs from the same
     *                        country
     */
    private void checkNoClubMeetsCountryMoreThanTwice(List<ClubSlot> clubSlots, List<NonKnockoutTie> ties) {
        for (ClubSlot clubSlot : clubSlots) {
            // Create a map to count the number of opponents from each country
            Map<Country, Long> opponentCountryCounts = ties.stream()
                    // Filter ties to include only those involving the current club slot
                    .filter(tie -> tie.getClubSlotA() == clubSlot || tie.getClubSlotB() == clubSlot)
                    // Map each tie to the opponent club slot
                    .map(tie -> tie.getClubSlotA() == clubSlot ? tie.getClubSlotB() : tie.getClubSlotA())
                    // Group the opponents by their country and count the occurrences
                    .collect(Collectors.groupingBy(opp -> opp.getCountries().get(0), Collectors.counting()));

            // Check that no club meets more than two clubs from any specific country
            for (Map.Entry<Country, Long> entry : opponentCountryCounts.entrySet()) {
                assertTrue(entry.getValue() <= 2,
                        "ClubSlot " + clubSlot + " meets >2 clubs from " + entry.getKey() + " ties=" + ties);
            }
        }
    }

    /**
     * Verifies that each match day in the scheduled league phase has exactly one
     * appearance per club.
     *
     * @param lp   league phase round under test
     * @param ties scheduled ties (ordered by match day groups)
     */
    private void checkExactlyOneMatchPerClubPerMatchDay(LeaguePhaseRound lp, List<NonKnockoutTie> ties) {
        int clubsCount = lp.getClubSlots().size();
        int tiesPerMatchDay = clubsCount / 2;

        assertEquals(0, clubsCount % 2, "League phase club count must be even");
        assertTrue(tiesPerMatchDay > 0, "tiesPerMatchDay must be > 0");
        assertEquals(0, ties.size() % tiesPerMatchDay,
                "Scheduled ties must split evenly into match days. ties=" + ties.size() + " tiesPerMatchDay="
                        + tiesPerMatchDay);

        int matchDayCount = ties.size() / tiesPerMatchDay;
        for (int md = 0; md < matchDayCount; md++) {
            int start = md * tiesPerMatchDay;
            int end = start + tiesPerMatchDay;
            Map<Integer, Long> appearanceCounts = ties.subList(start, end).stream()
                    .flatMap(t -> java.util.stream.Stream.of(
                            t.getClubSlotA().getClubSimState().getId(),
                            t.getClubSlotB().getClubSimState().getId()))
                    .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

            assertEquals(clubsCount, appearanceCounts.size(),
                    "Match day " + (md + 1) + " should include all clubs exactly once");
            for (Map.Entry<Integer, Long> entry : appearanceCounts.entrySet()) {
                assertEquals(1L, entry.getValue(),
                        "Club id " + entry.getKey() + " appears " + entry.getValue() +
                                " times on match day " + (md + 1));
            }
        }
    }

    /**
     * Ensures each club faces the expected number of distinct opponents dictated
     * by format: 6 for 6-pot (paired) format, else exactly two per pot.
     *
     * @param lp   the league phase round under test
     * @param ties all ties in that round
     */
    private void checkEachClubHasExpectedUniqueOpponents(LeaguePhaseRound lp, List<NonKnockoutTie> ties) {
        int potsCount = lp.getPots().size();
        int expectedOpponents = (potsCount == 6) ? 6 : 2 * potsCount; // 6-pot special case, otherwise 2 per pot
        for (ClubSlot club : lp.getClubSlots()) {
            Set<ClubSlot> opponents = ties.stream()
                    .filter(t -> t.getClubSlotA() == club || t.getClubSlotB() == club)
                    .map(t -> t.getClubSlotA() == club ? t.getClubSlotB() : t.getClubSlotA())
                    .collect(Collectors.toSet());
            assertEquals(expectedOpponents, opponents.size(),

                    "Club " + club + " should have " + expectedOpponents + " unique opponents, got "
                            + opponents.size());
        }
    }

    /**
     * Verifies that each unordered club pairing appears at most once.
     *
     * @param ties ties to scan for duplicate unordered pairs
     */
    private void checkNoDuplicateTiesById(List<NonKnockoutTie> ties) {
        // Unordered pairs based on ClubSimState equality (identity = club id)
        Set<Set<ClubSimState>> seenPairs = new HashSet<>();
        for (NonKnockoutTie t : ties) {
            ClubSimState a = t.getClubSlotA().getClubSimState();
            ClubSimState b = t.getClubSlotB().getClubSimState();
            Set<ClubSimState> pair = Set.of(a, b); // record equality ensures id-based comparison
            assertTrue(seenPairs.add(pair), "Duplicate tie found for pair: " + pair);
        }
    }
}
