package com.github.jkaste03.uefaccsim.model.competition;

import com.github.jkaste03.uefaccsim.enums.Leg;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.repository.ClubSimStateRepository;

/**
 * Represents a knockout tie, supporting both single-legged and two-legged
 * formats. Extends {@link Tie}, provides ranking retrieval for draw/seeding
 * logic, and provides simulation logic for match outcomes, including extra
 * time and penalties.
 *
 * <p>
 * Key Features:
 * <ul>
 * <li>Supports single-legged and two-legged knockout ties.</li>
 * <li>Simulates matches using Elo ratings for participating clubs.</li>
 * <li>Handles aggregate scoring, extra time, and penalty shootouts.</li>
 * </ul>
 * </p>
 * 
 * @author jkaste03
 */
public class KnockoutTie extends Tie {
    private static final int SIMS = 1000; // antall simuleringer for å estimere sannsynligheter

    protected Boolean clubAWinner;
    private boolean singleLegged = false;

    /**
     * The tournament this tie is part of. This is needed for qualifying rounds,
     * where the tournament affects seeding in the next round.
     */
    protected final Tournament tournament;

    /**
     * Constructs a knockout tie for the given slots in the specified
     * tournament. Order matters.
     * 
     * @param clubSlotA    home participant first leg
     * @param clubSlotB    away participant first leg
     * @param tournament   tournament
     * @param singleLegged indicates if the tie is single-legged.
     */
    public KnockoutTie(ClubSlot clubSlotA, ClubSlot clubSlotB, Tournament tournament, boolean singleLegged) {
        super(clubSlotA, clubSlotB);
        this.tournament = tournament;
        this.singleLegged = singleLegged;
    }

    /**
     * Constructs a two-legged knockout tie with preset goals (first leg). Order
     * matters.
     * 
     * @param clubSlotA        home participant first leg
     * @param clubSlotB        away participant first leg
     * @param clubAGoals1stLeg goals for club A
     * @param clubBGoals1stLeg goals for club B
     * @param tournament       tournament
     */
    public KnockoutTie(ClubSlot clubSlotA, ClubSlot clubSlotB, Integer clubAGoals1stLeg, Integer clubBGoals1stLeg,
            Tournament tournament) {
        super(clubSlotA, clubSlotB, clubAGoals1stLeg, clubBGoals1stLeg);
        this.tournament = tournament;
    }

    /**
     * Constructs a completed two-legged knockout tie with goals (both legs).
     * Order matters.
     * 
     * @param clubSlotA        home participant first leg
     * @param clubSlotB        away participant first leg
     * @param clubAGoals1stLeg goals for club A in first leg
     * @param clubBGoals1stLeg goals for club B in first leg
     * @param clubAGoals2ndLeg goals for club A in second leg
     * @param clubBGoals2ndLeg goals for club B in second leg
     * @param clubAWinner      indicates if club A is the winner (true if A wins,
     *                         false if B wins)
     * @param tournament       tournament
     */
    public KnockoutTie(ClubSlot clubSlotA, ClubSlot clubSlotB, Integer clubAGoals1stLeg, Integer clubBGoals1stLeg,
            Integer clubAGoals2ndLeg, Integer clubBGoals2ndLeg, Tournament tournament, Boolean clubAWinner) {
        super(clubSlotA, clubSlotB, clubAGoals1stLeg, clubBGoals1stLeg, clubAGoals2ndLeg, clubBGoals2ndLeg);
        this.clubAWinner = clubAWinner;
        this.tournament = tournament;
    }

    public Boolean isClubAWinner() {
        return clubAWinner;
    }

    public Tournament getTournament() {
        return tournament;
    }

    /**
     * Computes this tie's effective ranking relative to a caller tournament by
     * selecting one of the two underlying club rankings. If the caller tournament
     * is at a worse level than this tie's tournament, the worst ranking is
     * returned; otherwise, the best ranking is returned. Caller tournament may be
     * null, because non-knockout ties may not need a tournament.
     *
     * @param callerTournament the tournament context requesting the ranking
     * @return the ranking
     */
    public float getRanking(Tournament callerTournament) {
        float rA = clubSlotA.getRanking(tournament), rB = clubSlotB.getRanking(tournament);
        return ((tournament.compareTo(callerTournament == null ? tournament : callerTournament) > 0) ^ (rA < rB)) ? rA
                : rB;
    }

    /**
     * Plays the requested leg of this knockout tie and updates Elo ratings.
     *
     * <p>
     * <strong>Behavior</strong>
     * </p>
     * <ul>
     * <li>If {@code leg} is {@link Leg#FIRST}, the method simulates the first leg
     * with Club A at home. For two-legged ties, it returns immediately afterward
     * so the second leg can be played later.</li>
     * <li>If {@code leg} is {@link Leg#SECOND}, the method simulates the second leg
     * with Club A away.</li>
     * <li>If the aggregate score is decisive after the selected leg, the winner is
     * set and the method returns.</li>
     * <li>If the tie is still level on aggregate, the method simulates extra time
     * at
     * the venue of the decisive leg, then updates Elo with a reduced K-factor for
     * extra-time goals.</li>
     * <li>If the tie is still level after extra time, the winner is decided by a
     * penalty shootout at the same venue and Elo is updated with a reduced
     * shootout-weighted result.</li>
     * </ul>
     *
     * <p>
     * <strong>Elo update policy</strong>
     * </p>
     * <ul>
     * <li>Regulation-time goals of the played leg use the full K-factor.</li>
     * <li>Extra-time goals use {@code PARAM_ET_FACTOR * K}.</li>
     * <li>Penalty shootout outcome uses {@code PARAM_PSO_FACTOR * K}.</li>
     * </ul>
     *
     * <p>
     * <strong>Side effects</strong>
     * </p>
     * <ul>
     * <li>Mutates internal match state (leg scores, extra-time adjustments) and
     * sets the winner flag.</li>
     * <li>Updates clubs' Elo ratings via the provided repository.</li>
     * </ul>
     *
     * <p>
     * <strong>Usage</strong>
     * </p>
     * <ul>
     * <li><em>Two-legged ties:</em> Call once with {@code Leg.FIRST}, then again
     * with {@code Leg.SECOND} to resolve the tie.</li>
     * <li><em>Single-legged ties:</em> Call with {@code Leg.FIRST} to resolve the
     * tie in one invocation, including extra time and penalties if needed.</li>
     * </ul>
     *
     * @param clubSimStateRepo repository used to read and persist club simulation
     *                         state (e.g., Elo ratings) during updates
     * @param leg              which leg of the tie to play
     */
    public void play(ClubSimStateRepository clubSimStateRepo, Leg leg) {
        if (leg == Leg.FIRST) {
            if (clubAGoals1stLeg != null) {
                return;
            }
            // Simulate first leg (club A at home)
            simulateMatch(true, false);
            // Update Elo after first leg (only for first leg goals)
            updateEloForResult(clubAGoals1stLeg, clubBGoals1stLeg, true, PARAM_ELO_UPDATE_K, clubSimStateRepo);
            // If two-legged, return now and wait for second leg to be played later.
            if (!singleLegged)
                return;
        } else {
            if (clubAGoals2ndLeg != null) {
                return;
            }
            // Two-legged tie: simulate second leg
            simulateMatch(false, false);
            // Update Elo after second leg (only for second leg goals)
            updateEloForResult(clubAGoals2ndLeg, clubBGoals2ndLeg, false, PARAM_ELO_UPDATE_K, clubSimStateRepo);
        }
        // Check aggregate before potential ET/penalties
        if (getClubAGoals() != getClubBGoals()) {
            clubAWinner = getClubAGoals() > getClubBGoals();
            return;
        }

        // Temporary variables to track goals scored in ET
        int clubAGoalsPre90 = clubAGoals2ndLeg;
        int clubBGoalsPre90 = clubBGoals2ndLeg;

        // Simulate ET at the venue of the decisive leg
        simulateMatch(singleLegged, true);

        // Goals scored in ET (for Elo update with reduced K)
        int clubAGoalsET = clubAGoals2ndLeg - clubAGoalsPre90;
        int clubBGoalsET = clubBGoals2ndLeg - clubBGoalsPre90;

        // Update Elo after ET (only for ET goals)
        updateEloForResult(clubAGoalsET, clubBGoalsET, singleLegged, PARAM_ET_FACTOR * PARAM_ELO_UPDATE_K,
                clubSimStateRepo);

        // Check aggregate after ET
        if (getClubAGoals() != getClubBGoals()) {
            clubAWinner = getClubAGoals() > getClubBGoals();
            return;
        }

        // Penalties at the venue of the decisive leg
        clubAWinner = simulatePenaltyWinner(singleLegged);

        // Update Elo for penalty shootout outcome. We treat the winner as having scored
        // 1-0 (not actual match goals) for Elo purposes only, using a reduced K-factor.
        updateEloForResult(clubAWinner ? 1 : 0, clubAWinner ? 0 : 1, singleLegged,
                PARAM_PSO_FACTOR * PARAM_ELO_UPDATE_K, clubSimStateRepo);
    }

    @SuppressWarnings("unused")
    private void simulateOutCome() {
        int clubAWinnerCount = 0;
        for (int i = 0; i < SIMS; i++) {
            clubAGoals1stLeg = null;
            clubBGoals1stLeg = null;
            clubAGoals2ndLeg = null;
            clubBGoals2ndLeg = null;
            clubAWinner = null;
            simulateMatch(true, false);
            simulateMatch(false, false);

            // System.out.println(clubAGoals + "-" + clubBGoals);

            if (getClubAGoals() != getClubBGoals()) {
                clubAWinner = getClubAGoals() > getClubBGoals();
                clubAWinnerCount += clubAWinner ? 1 : 0;
                continue;
            }

            simulateMatch(false, true);

            if (getClubAGoals() != getClubBGoals()) {
                clubAWinner = getClubAGoals() > getClubBGoals();
                clubAWinnerCount += clubAWinner ? 1 : 0;
                continue;
            }

            // Penalties in second leg
            // simulatePenaltyWinner expects (eloHome, eloAway) where home is venue of
            // penalties (clubB)
            boolean clubAWinner = simulatePenaltyWinner(false);

            clubAWinnerCount += clubAWinner ? 1 : 0;
        }
        clubAGoals1stLeg = null;
        clubBGoals1stLeg = null;
        clubAGoals2ndLeg = null;
        clubBGoals2ndLeg = null;
        clubAWinner = null;
        System.out.println("Probabilities after " + SIMS + " sims: " + clubSlotA.toCompactString() + " win "
                + (clubAWinnerCount * 100.0 / SIMS) + "%, " + clubSlotB.toCompactString() + " win "
                + ((SIMS - clubAWinnerCount) * 100.0 / SIMS) + "%");
    }

    @Override
    public String toString() {
        return "KnockoutTie [" + fieldsToString() + ", clubAWinner=" + clubAWinner + ", singleLegged=" + singleLegged
                + ", tournament=" + tournament + "]";
    }
}
