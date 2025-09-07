package com.github.jkaste03.uefaccsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import com.github.jkaste03.uefaccsim.enums.Country;
import com.github.jkaste03.uefaccsim.enums.Tournament;

/**
 * Class representing the league phase in the Champions League and Europa
 * League. This class handles the league phase rounds where clubs compete in a
 * league format specific to those competitions.
 */
public class UclUelLeaguePhaseRound extends LeaguePhaseRound {
    /**
     * The number of pots used for seeding clubs in the league phase.
     */
    private final static int POT_COUNT = 4;
    private static final int MAX_RECURSIVE_CALLS = 300;
    private static final int MAX_RESTARTS = 20;

    // Statistikk for siste draw() TODO: Clean mess
    public static final class DrawStats {
        public long recursiveCalls;
        public long assignmentAttempts;
        public long backtracks;
        public long forwardPrunes;

        @Override
        public String toString() {
            return "DrawStats{recursiveCalls=" + recursiveCalls +
                    ", assignmentAttempts=" + assignmentAttempts +
                    ", backtracks=" + backtracks +
                    ", forwardPrunes=" + forwardPrunes + "}";
        }
    }

    private DrawStats lastDrawStats;

    public DrawStats getLastDrawStats() {
        return lastDrawStats;
    }

    /**
     * Constructs a Champions/Europa LeaguePhaseRound.
     *
     * @param tournament the tournament for which this league phase round is
     *                   initialized.
     */
    public UclUelLeaguePhaseRound(Tournament tournament) {
        super(tournament);
    }

    /**
     * Returns the number of pots used in the league phase round.
     *
     * @return the pot count as an integer
     */
    @Override
    protected int getPotCount() {
        return POT_COUNT;
    }

    /**
     * Improved draw method:
     * - Uses backtracking with MRV (choose slot with fewest candidates) and
     * forward-checking.
     * - Keeps randomness (shuffle) to avoid deterministic bias.
     * - Enforces the same constraints as original implementation (isIllegalTie,
     * country limits, etc.).
     */
    @Override
    protected void draw() {
        lastDrawStats = new DrawStats();
        // Build list of all clubs and mapping from club to its pot index.
        final List<ClubSlot> allClubs = new ArrayList<>();
        Map<ClubSlot, Integer> clubToPot = new HashMap<>();
        for (int i = 0; i < pots.size(); i++) {
            for (ClubSlot club : pots.get(i).clubs()) {
                clubToPot.put(club, i);
                allClubs.add(club);
            }
        }

        final Random random = new Random();

        // Helper for country counters (max two opponents from same foreign country)
        class CountryHelper {
            private final Map<ClubSlot, Map<Country, Integer>> countryCounters = new HashMap<>();

            CountryHelper() {
                for (ClubSlot c : allClubs) {
                    countryCounters.put(c, new HashMap<>());
                }
            }

            boolean canAddOpponent(ClubSlot club, ClubSlot opponent) {
                for (Country oppCountry : opponent.getCountries()) {
                    if (!club.getCountries().contains(oppCountry)) {
                        int count = countryCounters.get(club).getOrDefault(oppCountry, 0);
                        if (count >= 2)
                            return false;
                    }
                }
                return true;
            }

            void addOpponent(ClubSlot club, ClubSlot opponent) {
                for (Country oppCountry : opponent.getCountries()) {
                    if (!club.getCountries().contains(oppCountry)) {
                        Map<Country, Integer> map = countryCounters.get(club);
                        map.put(oppCountry, map.getOrDefault(oppCountry, 0) + 1);
                    }
                }
            }

            void removeOpponent(ClubSlot club, ClubSlot opponent) {
                for (Country oppCountry : opponent.getCountries()) {
                    if (!club.getCountries().contains(oppCountry)) {
                        Map<Country, Integer> map = countryCounters.get(club);
                        int prev = map.getOrDefault(oppCountry, 0);
                        if (prev <= 1)
                            map.remove(oppCountry);
                        else
                            map.put(oppCountry, prev - 1);
                    }
                }
            }

            void clear() {
                for (ClubSlot c : allClubs)
                    countryCounters.get(c).clear();
            }
        }

        // Slot representation: for each club, for each target pot, two slots: home
        // (true) and away (false).
        class Slot {
            final ClubSlot owner;
            final int targetPot;
            final boolean ownerHome;
            final int ownersPot; // cached pot index of owner to avoid map lookups
            boolean filled = false;
            ClubSlot opponent = null;

            Slot(ClubSlot owner, int targetPot, boolean ownerHome, int ownersPot) {
                this.owner = owner;
                this.targetPot = targetPot;
                this.ownerHome = ownerHome;
                this.ownersPot = ownersPot;
            }

            int complementIndex() { // replaces SlotHelpers.compIndexOf
                return ownerHome ? 1 : 0;
            }
        }

        // Prepare slots and quick access structures
        Map<ClubSlot, Slot[][]> slotsByClub = new HashMap<>(); // club -> [pot][home(0)/away(1)]
        List<Slot> allSlots = new ArrayList<>(allClubs.size() * POT_COUNT * 2);

        for (ClubSlot club : allClubs) {
            int ownersPot = clubToPot.get(club);
            Slot[][] arr = new Slot[POT_COUNT][2];
            for (int p = 0; p < POT_COUNT; p++) {
                arr[p][0] = new Slot(club, p, true, ownersPot); // owner plays home vs pot p
                arr[p][1] = new Slot(club, p, false, ownersPot); // owner plays away vs pot p
                allSlots.add(arr[p][0]);
                allSlots.add(arr[p][1]);
            }
            slotsByClub.put(club, arr);
        }

        // Assigned opponents (to quickly check duplicates). Using sets for fast lookup.
        Map<ClubSlot, Set<ClubSlot>> assignedOpp = new HashMap<>();
        for (ClubSlot c : allClubs)
            assignedOpp.put(c, new HashSet<>());

        // For speedy access, prepare pot membership lists (so we don't create temp
        // lists repeatedly).
        List<List<ClubSlot>> potLists = new ArrayList<>();
        for (int p = 0; p < POT_COUNT; p++) {
            // create a shuffled copy to simulate balls drawing order
            List<ClubSlot> potCopy = new ArrayList<>(pots.get(p).clubs());
            // shuffle to avoid deterministic bias
            Collections.shuffle(potCopy, random);
            potLists.add(potCopy);
        }

        // Removed SlotHelpers indirection; Slot now exposes complementIndex().

        // Now implement backtracking solver with MRV and forward checking.
        CountryHelper countryHelper = new CountryHelper();

        // Utility to check whether a slot can accept a candidate considering country
        // constraints and already assigned opponents.
        java.util.function.BiPredicate<Slot, ClubSlot> slotCandidateLegal = (slot, candidate) -> {
            if (assignedOpp.get(slot.owner).contains(candidate))
                return false;
            // candidate complementary slot must be free
            int ownersPot = slot.ownersPot;
            Slot candidateComp = slotsByClub.get(candidate)[ownersPot][slot.complementIndex()];
            if (candidateComp.filled)
                return false;
            // illegal tie checks
            if (isIllegalTie(slot.owner, candidate))
                return false;
            if (isIllegalTie(candidate, slot.owner))
                return false;
            // country constraints for both sides
            if (!countryHelper.canAddOpponent(slot.owner, candidate))
                return false;
            if (!countryHelper.canAddOpponent(candidate, slot.owner))
                return false;
            return true;
        };

        // ownersPot cached inside Slot; ownersPotCache Map removed.

        // Helper: count unfilled slots
        final int totalSlots = allSlots.size();

        // Prepare list of slots for iterative scanning
        List<Slot> slotList = new ArrayList<>(allSlots);

        // MRV selection: pick an unfilled slot with the fewest legal candidates
        Supplier<Slot> selectNextSlot = () -> {
            Slot best = null;
            int bestCount = Integer.MAX_VALUE;
            for (Slot s : slotList) {
                if (s.filled)
                    continue;
                // quick heuristic: if the owner already has many assigned opponents, it's more
                // constrained
                // But we compute exact candidate count:
                int ownersPot = s.ownersPot;
                List<ClubSlot> pool = potLists.get(s.targetPot);
                int cnt = 0;
                for (ClubSlot cand : pool) {
                    if (cand.equals(s.owner))
                        continue;
                    if (assignedOpp.get(s.owner).contains(cand))
                        continue;
                    Slot comp = slotsByClub.get(cand)[ownersPot][s.complementIndex()];
                    if (comp.filled)
                        continue;
                    if (isIllegalTie(s.owner, cand))
                        continue;
                    if (isIllegalTie(cand, s.owner))
                        continue;
                    if (!countryHelper.canAddOpponent(s.owner, cand))
                        continue;
                    if (!countryHelper.canAddOpponent(cand, s.owner))
                        continue;
                    cnt++;
                    if (cnt >= bestCount)
                        break; // no need to count more than current best
                }
                if (cnt < bestCount) {
                    bestCount = cnt;
                    best = s;
                    if (bestCount <= 1)
                        break; // can't get better than 0 or 1
                }
            }
            return best;
        };

        // Build initial assignedOpp empty sets (already done)
        assignedOpp.clear();
        for (ClubSlot c : allClubs)
            assignedOpp.put(c, new HashSet<>());
        countryHelper.clear();

        // Main recursive solver
        final boolean[] solved = { false };
        final boolean[] restartRequested = { false };

        // To speed up, we can cache candidate lists per call — but since constraints
        // change, we must recompute.
        class Solver {
            void solve(int filledCount) {
                lastDrawStats.recursiveCalls++;
                if (lastDrawStats.recursiveCalls > MAX_RECURSIVE_CALLS) {
                    // be om en restart — vi _kaller ikke_ draw() her
                    restartRequested[0] = true;
                    return;
                }
                if (solved[0] || restartRequested[0])
                    return; // early exit if found
                if (filledCount == totalSlots) {
                    solved[0] = true;
                    return;
                }
                // select slot using MRV
                Slot slot = selectNextSlot.get();
                if (slot == null)
                    return; // no slot found — dead end

                // generate candidates in randomized order (but deterministic per Random)
                List<ClubSlot> pool = potLists.get(slot.targetPot);
                List<ClubSlot> candidates = new ArrayList<>(pool.size()); // preallocate
                for (ClubSlot cand : pool) {
                    if (!slotCandidateLegal.test(slot, cand))
                        continue;
                    candidates.add(cand);
                }
                // if no candidates, dead end
                if (candidates.isEmpty())
                    return;

                Collections.shuffle(candidates, random); // random tie-break

                int ownersPot = slot.ownersPot;

                for (ClubSlot cand : candidates) {
                    if (restartRequested[0])
                        return; // stopp tidlig hvis restart bedt om
                    lastDrawStats.assignmentAttempts++;
                    // check complementary slot again and assign
                    Slot comp = slotsByClub.get(cand)[ownersPot][slot.complementIndex()];
                    if (comp.filled)
                        continue; // double-check race condition

                    // assign
                    slot.filled = true;
                    slot.opponent = cand;
                    comp.filled = true;
                    comp.opponent = slot.owner;
                    assignedOpp.get(slot.owner).add(cand);
                    assignedOpp.get(cand).add(slot.owner);
                    countryHelper.addOpponent(slot.owner, cand);
                    countryHelper.addOpponent(cand, slot.owner);

                    // Forward-check: early prune if any unfilled slot has zero candidates
                    boolean forwardFail = false;
                    for (Slot s2 : slotList) {
                        if (s2.filled)
                            continue;
                        boolean has = false;
                        List<ClubSlot> pool2 = potLists.get(s2.targetPot);
                        int ownersPot2 = s2.ownersPot;
                        for (ClubSlot cand2 : pool2) {
                            if (cand2.equals(s2.owner))
                                continue;
                            if (assignedOpp.get(s2.owner).contains(cand2))
                                continue;
                            Slot comp2 = slotsByClub.get(cand2)[ownersPot2][s2.complementIndex()];
                            if (comp2.filled)
                                continue;
                            if (isIllegalTie(s2.owner, cand2))
                                continue;
                            if (isIllegalTie(cand2, s2.owner))
                                continue;
                            if (!countryHelper.canAddOpponent(s2.owner, cand2))
                                continue;
                            if (!countryHelper.canAddOpponent(cand2, s2.owner))
                                continue;
                            has = true;
                            break;
                        }
                        if (!has) {
                            forwardFail = true;
                            break;
                        }
                    }

                    if (!forwardFail) {
                        solve(filledCount + 2); // two slots filled together
                        if (solved[0] || restartRequested[0])
                            return;
                    } else {
                        lastDrawStats.forwardPrunes++;
                    }

                    // undo assignment (backtrack)
                    slot.filled = false;
                    slot.opponent = null;
                    comp.filled = false;
                    comp.opponent = null;
                    assignedOpp.get(slot.owner).remove(cand);
                    assignedOpp.get(cand).remove(slot.owner);
                    countryHelper.removeOpponent(slot.owner, cand);
                    countryHelper.removeOpponent(cand, slot.owner);
                    lastDrawStats.backtracks++;
                }
            }
        }

        // Nå: iterativ restart-løkke i stedet for å rekursivt kalle draw()
        boolean overallSolved = false;
        int attempt;
        for (attempt = 0; attempt < MAX_RESTARTS && !overallSolved; attempt++) {
            // Nullstill nødvendige tilstander for nytt forsøk
            lastDrawStats.recursiveCalls = 0;
            lastDrawStats.assignmentAttempts = 0;
            lastDrawStats.backtracks = 0;
            lastDrawStats.forwardPrunes = 0;
            restartRequested[0] = false;
            solved[0] = false;

            // Reset slots
            for (Slot s : slotList) {
                s.filled = false;
                s.opponent = null;
            }
            // Reset assignedOpp
            assignedOpp.clear();
            for (ClubSlot c : allClubs)
                assignedOpp.put(c, new HashSet<>());
            // Reset country counts
            countryHelper.clear();
            // reshuffle pots to vary randomness
            for (int p = 0; p < POT_COUNT; p++) {
                Collections.shuffle(potLists.get(p), random);
            }

            // Utility for Slot to compute complementary index (we add method outside of
            // local class by using this utility)
            // But earlier we used slotHelpers.compIndexOf(slot) when needed.

            // Fill: count currently filled = 0
            Solver solver = new Solver();

            // Because official draw procedure draws pot-by-pot, we want to avoid bias:
            // shuffle initial pot order and potLists contents already shuffled above.
            // However solver will fill slots in MRV order (which is correct and unbiased
            // with randomized tie-breaks).

            solver.solve(0);

            if (solved[0]) {
                overallSolved = true;
                break;
            }
            // if restartRequested was set, loop will try again
        }

        if (!overallSolved) {
            throw new RuntimeException("Kunne ikke fullføre trekningen: backtracking mislyktes.");
        }

        // System.out.println("[LeaguePhase draw stats][" +
        // Thread.currentThread().getName() + "] " + lastDrawStats);

        // Build ties list from filled slots (each pair will appear twice as two
        // complementary slots; we only add one per pair)
        List<Tie> results = new ArrayList<>();
        for (Slot s : slotList) {
            if (!s.filled || !s.ownerHome)
                continue; // legg kun til én gang pr oppgjør
            results.add(new NonKnockoutTie(s.owner, s.opponent));
        }

        // Assign to ties field
        ties = results;
    }
}
