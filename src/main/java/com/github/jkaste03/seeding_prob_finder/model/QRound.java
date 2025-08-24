package com.github.jkaste03.seeding_prob_finder.model;

import java.util.List;

import com.github.jkaste03.seeding_prob_finder.service.ClubEloDataLoader;
import com.github.jkaste03.seeding_prob_finder.enums.Tournament;
import com.github.jkaste03.seeding_prob_finder.enums.PathType;
import com.github.jkaste03.seeding_prob_finder.enums.RoundType;

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
    private final static int UCL_Q1_CP_TIES_WITHOUT_REBALANCING = 16;
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
     * Recursively builds a legal matching between remaining seeded and unseeded.
     * Uses MRV heuristic: pick seeded club with fewest legal opponents to reduce
     * branching and bias. Candidate opponent order is randomized each recursion.
     *
     * @param remainingSeeded   remaining seeded clubs to match
     * @param remainingUnseeded remaining unseeded clubs to match
     * @param result            accumulator list of ties (modified in-place)
     * @param rng               randomness source
     * @return true if a full legal matching was found
     */
    private boolean buildMatching(List<ClubSlot> remainingSeeded, List<ClubSlot> remainingUnseeded,
            List<DoubleLeggedTie> result, java.util.Random rng) {
        if (remainingSeeded.isEmpty()) {
            return true; // all paired
        }

        // Pick seeded with minimum number of legal opponents (MRV)
        ClubSlot nextSeeded = null;
        List<ClubSlot> legalOpponentsForNext = null;
        int min = Integer.MAX_VALUE;
        for (ClubSlot s : remainingSeeded) {
            List<ClubSlot> legal = new ArrayList<>();
            for (ClubSlot u : remainingUnseeded) {
                if (!isIllegalTie(s, u)) {
                    legal.add(u);
                }
            }
            if (legal.isEmpty()) {
                return false; // dead end
            }
            if (legal.size() < min) {
                min = legal.size();
                nextSeeded = s;
                legalOpponentsForNext = legal;
                if (min == 1) { // can't do better
                    break;
                }
            }
        }

        // Randomize opponent order to avoid bias
        java.util.Collections.shuffle(legalOpponentsForNext, rng);

        // Try each opponent (backtracking)
        for (ClubSlot opp : legalOpponentsForNext) {
            remainingSeeded.remove(nextSeeded);
            remainingUnseeded.remove(opp);
            DoubleLeggedTie tie = rng.nextBoolean() ? new DoubleLeggedTie(nextSeeded, opp, tournament)
                    : new DoubleLeggedTie(opp, nextSeeded, tournament);
            result.add(tie);

            if (buildMatching(remainingSeeded, remainingUnseeded, result, rng)) {
                // Restore lists not needed because we return success
                return true;
            }

            // Backtrack
            result.remove(result.size() - 1);
            remainingUnseeded.add(opp);
            remainingSeeded.add(nextSeeded);
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
