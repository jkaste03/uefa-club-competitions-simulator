package com.github.jkaste03.uefaccsim.model.competition;

import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.repository.ClubSimStateRepository;

/**
 * Represents a knockout tie, supporting both single-legged and two-legged
 * formats. Extends {@link Tie} and provides simulation logic for match
 * outcomes, including extra time and penalties.
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
     * Constructs a knockout tie for the given slots in the specified
     * tournament. Order matters.
     * 
     * @param clubSlotA    home participant first leg
     * @param clubSlotB    away participant first leg
     * @param tournament   tournament
     * @param singleLegged indicates if the tie is single-legged.
     */
    public KnockoutTie(ClubSlot clubSlotA, ClubSlot clubSlotB, Tournament tournament, boolean singleLegged) {
        super(clubSlotA, clubSlotB, tournament);
        this.singleLegged = singleLegged;
    }

    /**
     * Constructs a two-legged knockout tie with preset goals (first leg). Order
     * matters.
     * 
     * @param clubSlotA  home participant first leg
     * @param clubSlotB  away participant first leg
     * @param clubAGoals goals for club A
     * @param clubBGoals goals for club B
     * @param tournament tournament
     */
    public KnockoutTie(ClubSlot clubSlotA, ClubSlot clubSlotB, Integer clubAGoals,
            Integer clubBGoals, Tournament tournament) {
        super(clubSlotA, clubSlotB, clubAGoals, clubBGoals, tournament);
    }

    public Boolean isClubAWinner() {
        return clubAWinner;
    }

    /**
     * Plays or advances this knockout tie by simulating the next required phase and
     * updating Elo ratings.
     *
     * <p>
     * <strong>Behavior</strong>
     * </p>
     * <ul>
     * <li>If no first-leg score exists, simulates the first leg with Club A at home
     * and updates Elo using only first-leg goals. For a two-legged tie, the method
     * returns immediately afterward to await the second leg.</li>
     * <li>Otherwise (for a two-legged tie), simulates the second leg with Club A
     * away
     * and updates Elo using only second-leg goals.</li>
     * <li>If the aggregate score is decisive at this point, sets the winner and
     * returns.</li>
     * <li>If still level on aggregate, simulates extra time (at the venue of the
     * decisive leg),
     * derives ET-only goals, and updates Elo with a reduced K-factor for ET.</li>
     * <li>If still tied after extra time, determines the winner via a penalty
     * shootout (same venue)
     * and updates Elo by treating the shootout outcome as a reduced-weight 1–0
     * result for Elo
     * purposes only (does not alter match goals).</li>
     * </ul>
     *
     * <p>
     * <strong>Elo update policy</strong>
     * </p>
     * <ul>
     * <li>Regulation-time goals of the leg being played use the full K-factor.</li>
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
     * <li>Updates clubs’ Elo ratings via the provided repository.</li>
     * </ul>
     *
     * <p>
     * <strong>Usage</strong>
     * </p>
     * <ul>
     * <li><em>Two-legged ties:</em> Call once to simulate the first leg (method
     * returns afterward),
     * then call again to simulate the second leg and resolve the tie.</li>
     * <li><em>Single-legged ties:</em> A single call fully resolves the tie
     * (including ET and penalties if needed).</li>
     * </ul>
     *
     * @param clubSimStateRepo repository used to read and persist club simulation
     *                         state (e.g., Elo ratings) during updates
     */
    @Override
    public void play(ClubSimStateRepository clubSimStateRepo) {
        // If no score known -> simulate first leg
        if (clubAGoals1stLeg == null) {
            // Simulate first leg (club A at home)
            simulateMatch(true, false);
            // Update Elo after first leg (only for first leg goals)
            updateEloForResult(clubAGoals1stLeg, clubBGoals1stLeg, true, PARAM_ELO_UPDATE_K, clubSimStateRepo);
            // If two-legged, return now and wait for second leg to be played later.
            if (!singleLegged)
                return;
        }
        // If double-legged, simulate second leg
        else if (!singleLegged) {
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
                + "]";
    }
}
