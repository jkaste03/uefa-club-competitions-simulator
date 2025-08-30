package com.github.jkaste03.uefaccsim.model;

import java.util.List;

import com.github.jkaste03.uefaccsim.enums.PathType;
import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.service.ClubEloDataLoader;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Class representing a qualifying round in the UEFA competitions.
 */
public class QRound extends Round {
    /**
     * Excluding rebalancing, the number of ties in the UCL Q1 CP round is 16. If
     * the number of ties is less than 16, losers of ties need to jump to the UECL
     * Q3 CP round.
     */
    private static final int UCL_Q1_CP_TIES_WITHOUT_REBALANCING = 16;
    /**
     * Max number of full draw construction attempts before giving up (should be
     * plenty).
     */
    private static final int MAX_DRAW_ATTEMPTS = 500;
    // private final static String ROUND_CLUBS_SKIP_TO =
    // Tournament.CONFERENCE_LEAGUE + " " + CompetitionData.RoundType.Q3
    // + " " + CompetitionData.PathType.CHAMPIONS_PATH;

    private final List<DoubleLeggedTie> ties = new ArrayList<>();
    private final PathType pathType;
    private final List<ClubSlot> seeded = new ArrayList<>();
    private final List<ClubSlot> unseeded = new ArrayList<>();

    /**
     * Constructs a qualifying round for the specified tournament, round type and
     * path type.
     *
     * @param tournament the tournament of this round.
     * @param roundType  the type of the round (Q1, Q2, etc.).
     * @param pathType   the path type representing the qualifying route.
     */
    public QRound(Tournament tournament, RoundType roundType, PathType pathType) {
        super(tournament, roundType);
        this.pathType = pathType;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a string representation of the qualifying round, including the
     * tournament, round type, and path type.
     */
    @Override
    public String getName() {
        return super.getName() + " " + roundType + " " + pathType;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Seeds the clubSlots in the qualifying round.
     * Throws an IllegalArgumentException if the number of clubSlots is odd.
     */
    @Override
    public void seed() {
        if (clubSlots == null || clubSlots.size() % 2 != 0) {
            throw new IllegalArgumentException("The number of clubSlots must be even to seed them properly.");
        }

        // Sort clubSlots by ranking descending
        clubSlots.sort((a, b) -> Float.compare(a.getRanking(tournament), b.getRanking(tournament)));
        int half = clubSlots.size() / 2;
        seeded.addAll(clubSlots.subList(0, half));
        unseeded.addAll(clubSlots.subList(half, clubSlots.size()));

        incrementSeedingCounters();
    }

    private void incrementSeedingCounters() {
        seeded.forEach(clubSlot -> clubSlot.incrementSeedingCounter(true));
        unseeded.forEach(clubSlot -> clubSlot.incrementSeedingCounter(false));
    }

    /**
     * Constructs a legal randomized draw pairing each seeded club with an unseeded
     * club.
     * <p>
     * Algorithm:
     * <ul>
     * <li>Uses randomized backtracking with an MRV (Minimum Remaining Values)
     * heuristic to reduce bias and avoid dead ends while building a perfect
     * matching.</li>
     * <li>Works on fresh copies of the original {@code seeded} and {@code unseeded}
     * lists so their order and contents remain unchanged.</li>
     * <li>Clears and repopulates {@code ties} with the successful set of
     * pairings.</li>
     * <li>May retry up to {@code MAX_DRAW_ATTEMPTS} times (normally succeeds on the
     * first attempt if any legal matching exists).</li>
     * <li>Randomness is sourced from a {@link java.util.Random} initialized with
     * {@code System.nanoTime()}.</li>
     * </ul>
     * Failure:
     * <ul>
     * <li>If no valid matching can be produced within the allowed attempts, throws
     * an {@link IllegalStateException}.</li>
     * </ul>
     *
     * @throws IllegalStateException if a legal draw cannot be built after
     *                               {@code MAX_DRAW_ATTEMPTS} attempts.
     */
    @Override
    public void draw() {
        List<ClubSlot> seededCopy = new ArrayList<>(seeded);
        List<ClubSlot> unseededCopy = new ArrayList<>(unseeded);

        java.util.Random rng = new java.util.Random(System.nanoTime());

        // Attempt multiple times (should almost always succeed first try if a valid
        // matching exists). If no valid matching is found after attempts, throw.
        boolean success = false;
        for (int attempt = 0; attempt < MAX_DRAW_ATTEMPTS && !success; attempt++) {
            ties.clear();
            if (attempt > 0) { // fresh copies each attempt
                seededCopy = new ArrayList<>(seeded);
                unseededCopy = new ArrayList<>(unseeded);
            }
            success = buildMatching(seededCopy, unseededCopy, ties, rng);
        }

        if (!success) {
            throw new IllegalStateException(
                    "Could not construct a legal draw after " + MAX_DRAW_ATTEMPTS + " attempts.");
        }
    }

    /**
     * Attempts to construct a complete one-to-one matching (set of DoubleLeggedTie
     * instances) between the supplied seeded and unseeded club slots subject to
     * pairing constraints (as enforced by {@code isIllegalTie}). This method uses a
     * recursive backtracking search enhanced with:
     * <ul>
     * <li>MRV (Minimum Remaining Values) heuristic: always expand a seeded club
     * with the fewest legal opponents to reduce branching and detect dead-ends
     * early.</li>
     * <li>Randomization: shuffles candidate seeded clubs having the MRV, randomizes
     * opponent order, and randomizes the home/away (or leg order) assignment when
     * creating each tie, producing varied valid draws across invocations.</li>
     * </ul>
     * 
     * @param remainingSeeded   the list of seeded ClubSlot instances
     * @param remainingUnseeded the list of unseeded ClubSlot instances
     * @param result            accumulator list to which confirmed DoubleLeggedTie
     *                          pairings are appended in order
     * @param rng               source of randomness for shuffling selection and tie
     *                          orientation
     * @return true if a complete legal matching was found; false if no completion
     *         is possible
     */
    private boolean buildMatching(List<ClubSlot> remainingSeeded, List<ClubSlot> remainingUnseeded,
            List<DoubleLeggedTie> result, java.util.Random rng) {
        if (remainingSeeded.isEmpty()) {
            return true; // all paired
        }

        // (1) Optional: shuffle seeded list to remove positional bias before computing
        // MRV
        Collections.shuffle(remainingSeeded, rng);

        // (2) Build map of legal opponents and find MRV value
        java.util.Map<ClubSlot, List<ClubSlot>> legalMap = new java.util.HashMap<>();
        int min = Integer.MAX_VALUE;
        for (ClubSlot s : remainingSeeded) {
            List<ClubSlot> legal = new ArrayList<>();
            for (ClubSlot u : remainingUnseeded) {
                if (!isIllegalTie(s, u)) {
                    legal.add(u);
                }
            }
            if (legal.isEmpty()) {
                return false; // dead end: impossible to match this seeded club
            }
            legalMap.put(s, legal);
            if (legal.size() < min) {
                min = legal.size();
            }
        }

        // (3) Collect all seeded clubs that have the MRV (min) and pick one at random
        List<ClubSlot> mrvCandidates = new ArrayList<>();
        for (ClubSlot s : remainingSeeded) {
            if (legalMap.get(s).size() == min) {
                mrvCandidates.add(s);
            }
        }
        ClubSlot nextSeeded = mrvCandidates.get(rng.nextInt(mrvCandidates.size()));

        // (4) Randomize opponents for the chosen seeded
        List<ClubSlot> legalOpponentsForNext = new ArrayList<>(legalMap.get(nextSeeded));
        Collections.shuffle(legalOpponentsForNext, rng);

        // (5) Try opponents, but use copies for recursion to avoid mutating & changing
        // order
        for (ClubSlot opp : legalOpponentsForNext) {
            List<ClubSlot> newRemainingSeeded = new ArrayList<>(remainingSeeded);
            newRemainingSeeded.remove(nextSeeded);
            List<ClubSlot> newRemainingUnseeded = new ArrayList<>(remainingUnseeded);
            newRemainingUnseeded.remove(opp);

            DoubleLeggedTie tie = rng.nextBoolean()
                    ? new DoubleLeggedTie(nextSeeded, opp, tournament)
                    : new DoubleLeggedTie(opp, nextSeeded, tournament);
            result.add(tie);

            if (buildMatching(newRemainingSeeded, newRemainingUnseeded, result, rng)) {
                return true;
            }

            // backtrack
            result.remove(result.size() - 1);
        }

        return false;
    }

    /**
     * Registers all current ties (matchups) into their appropriate slots for the
     * upcoming rounds in the competition structure.
     * <p>
     * Behavior:
     * <ul>
     * <li>If some clubs are eligible to skip the secondary round (determined by
     * {@code noOfClubsCanSkipSecondary()}), the list of ties is shuffled to
     * randomize which ties receive that advantage.</li>
     * <li>Every tie is always added to the next primary round via
     * {@code nextPrimaryRnd.addClubSlot(...)}.</li>
     * <li>If a secondary round exists:
     * <ul>
     * <li>The first {@code noOfClubsCanSkipSecondary()} ties (post-shuffle, if any)
     * bypass the secondary round and are placed directly into the
     * primary round following the secondary round
     * ({@code nextSecondaryRnd.nextPrimaryRnd}).</li>
     * <li>All remaining ties are entered into the secondary round
     * ({@code nextSecondaryRnd}).</li>
     * </ul>
     * </li>
     * </ul>
     * </p>
     */
    public void regTiesForNextRounds() {
        // If ties must skip the secondary round, shuffle the ties to randomize which
        // ties get to skip
        int noOfClubsToSkipSecondary = noOfClubsCanSkipSecondary();
        if (noOfClubsToSkipSecondary > 0) {
            Collections.shuffle(ties);
        }
        // Add ties to the next primary round and the next secondary round if applicable
        ties.forEach(tie -> {
            // Add tie to the next primary round
            this.nextPrimaryRnd.addClubSlot(new ClubSlot(tie));
            // Add tie to the next secondary round if applicable
            if (this.nextSecondaryRnd != null) {
                // Add tie to the next primary round of the secondary round if it can skip,
                // otherwise add to the secondary round
                if (ties.indexOf(tie) < noOfClubsToSkipSecondary) {
                    this.nextSecondaryRnd.nextPrimaryRnd.addClubSlot(new ClubSlot(tie));
                } else {
                    this.nextSecondaryRnd.addClubSlot(new ClubSlot(tie));
                }
            }
        });
    }

    /**
     * Determines the number of clubs that can skip the secondary round.
     * 
     * @return the number of clubs that can skip the secondary round.
     */
    private int noOfClubsCanSkipSecondary() {
        int noOfClubsToSkip = (tournament == Tournament.CHAMPIONS_LEAGUE
                && roundType == RoundType.Q1
                && pathType == PathType.CHAMPIONS_PATH) ? UCL_Q1_CP_TIES_WITHOUT_REBALANCING - ties.size() : 0;
        return noOfClubsToSkip;
    }

    /**
     * Plays the ties in the qualifying round.
     */
    public void play(ClubEloDataLoader clubEloDataLoader) {
        for (DoubleLeggedTie tie : ties) {
            tie.play(clubEloDataLoader);
        }
        // Todo: Update the clubEloDataLoader with the new Elo ratings after the matches
    }

    @Override
    public String toString() {
        return "QRound [name=" + getName() + fieldsToString() + ", seeded=" + seeded + ", unseeded=" + unseeded + "]";
    }
}
