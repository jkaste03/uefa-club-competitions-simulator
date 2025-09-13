package com.github.jkaste03.uefaccsim.model.competition;

import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.service.ClubEloDataLoader;

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

    @Override
    public void play(ClubEloDataLoader clubEloDataLoader) {

        // If no score known -> simulate first leg
        if (clubAGoals1stLeg == null) {
            // Simulate first leg
            simulateMatch(true, false, clubEloDataLoader);
            // Update Elo after first leg (only for first leg goals)
            updateEloForResult(clubAGoals1stLeg, clubBGoals1stLeg, true, PARAM_ELO_UPDATE_K, clubEloDataLoader);
            // If double-legged, wait for second leg
            if (!singleLegged)
                return;
        }
        // If double-legged, simulate second leg
        else if (!singleLegged) {
            simulateMatch(false, false, clubEloDataLoader);

            // Update Elo after second leg (only for second leg goals)
            updateEloForResult(clubAGoals2ndLeg, clubBGoals2ndLeg, false, PARAM_ELO_UPDATE_K, clubEloDataLoader);
        }

        // Check aggregate before potential ET/penalties
        if (getClubAGoals() != getClubBGoals()) {
            clubAWinner = getClubAGoals() > getClubBGoals();
            return;
        }

        // Temporary variables to track goals scored in ET
        int clubAGoalsPre90 = clubAGoals2ndLeg;
        int clubBGoalsPre90 = clubBGoals2ndLeg;

        // Simulate ET
        simulateMatch(singleLegged, true, clubEloDataLoader);

        // Needed to know how many goals were scored in ET for Elo update
        int clubAGoalsPost90 = clubAGoals2ndLeg - clubAGoalsPre90;
        int clubBGoalsPost90 = clubBGoals2ndLeg - clubBGoalsPre90;

        // Update Elo after ET (only for ET goals)
        updateEloForResult(clubAGoalsPost90, clubBGoalsPost90, singleLegged, PARAM_ET_FACTOR * PARAM_ELO_UPDATE_K,
                clubEloDataLoader);

        // Check aggregate after ET
        if (getClubAGoals() != getClubBGoals()) {
            clubAWinner = getClubAGoals() > getClubBGoals();
            return;
        }

        // Simulate penalties
        clubAWinner = simulatePenaltyWinner(singleLegged, clubEloDataLoader);

        // Update Elo for penalty shootout outcome. We treat the winner as having scored
        // 1-0 (not actual match goals) for Elo purposes only, using a reduced K-factor.
        updateEloForResult(clubAWinner ? 1 : 0, clubAWinner ? 0 : 1, singleLegged,
                PARAM_PSO_FACTOR * PARAM_ELO_UPDATE_K, clubEloDataLoader);
    }

    private void simulateOutCome(ClubEloDataLoader clubEloDataLoader) {
        int clubAWinnerCount = 0;
        for (int i = 0; i < SIMS; i++) {
            clubAGoals1stLeg = null;
            clubBGoals1stLeg = null;
            clubAGoals2ndLeg = null;
            clubBGoals2ndLeg = null;
            clubAWinner = null;
            simulateMatch(true, false, clubEloDataLoader);
            simulateMatch(false, false, clubEloDataLoader);

            // System.out.println(clubAGoals + "-" + clubBGoals);

            if (getClubAGoals() != getClubBGoals()) {
                clubAWinner = getClubAGoals() > getClubBGoals();
                clubAWinnerCount += clubAWinner ? 1 : 0;
                continue;
            }

            simulateMatch(false, true, clubEloDataLoader);

            if (getClubAGoals() != getClubBGoals()) {
                clubAWinner = getClubAGoals() > getClubBGoals();
                clubAWinnerCount += clubAWinner ? 1 : 0;
                continue;
            }

            // Penalties in second leg
            // simulatePenaltyWinner expects (eloHome, eloAway) where home is venue of
            // penalties (clubB)
            boolean clubAWinner = simulatePenaltyWinner(false, clubEloDataLoader);

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
