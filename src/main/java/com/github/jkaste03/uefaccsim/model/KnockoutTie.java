package com.github.jkaste03.uefaccsim.model;

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

    protected Boolean club1Winner;
    private boolean singleLegged = false;

    /**
     * Constructs a knockout tie for the given slots in the specified
     * tournament. Order matters.
     * 
     * @param clubSlot1    home participant first leg
     * @param clubSlot2    away participant first leg
     * @param tournament   tournament
     * @param singleLegged indicates if the tie is single-legged.
     */
    public KnockoutTie(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament, boolean singleLegged) {
        super(clubSlot1, clubSlot2, tournament);
        this.singleLegged = singleLegged;
    }

    /**
     * Constructs a two-legged knockout tie with preset goals (first leg). Order
     * matters.
     * 
     * @param clubSlot1  home participant first leg
     * @param clubSlot2  away participant first leg
     * @param club1Goals goals for club 1
     * @param club2Goals goals for club 2
     * @param tournament tournament
     */
    public KnockoutTie(ClubSlot clubSlot1, ClubSlot clubSlot2, Integer club1Goals,
            Integer club2Goals, Tournament tournament) {
        super(clubSlot1, clubSlot2, club1Goals, club2Goals, tournament);
    }

    public Boolean isClub1Winner() {
        return club1Winner;
    }

    /**
     * Simulates the outcome of a knockout tie between two clubs using their Elo
     * ratings.
     * The method handles both single-legged and double-legged ties, including extra
     * time and penalties if necessary.
     * <p>
     * Steps:
     * <ul>
     * <li>If no score is known, simulates the first leg (club1 at home).</li>
     * <li>If the tie is double-legged, simulates the second leg (club2 at
     * home).</li>
     * <li>Determines the winner based on aggregate goals.</li>
     * <li>If aggregate scores are level, simulates extra time at the second leg
     * venue.</li>
     * <li>If still level after extra time, simulates a penalty shootout at the
     * second leg venue.</li>
     * <li>Sets {@code club1Winner} to indicate if club1 won the tie.</li>
     * </ul>
     *
     * @param clubEloDataLoader the loader providing Elo ratings for clubs
     */
    @Override
    public void play(ClubEloDataLoader clubEloDataLoader) {
        ClubIdWrapper club1 = clubSlot1.getClubIdWrapper();
        ClubIdWrapper club2 = clubSlot2.getClubIdWrapper();

        double elo1 = club1.getEloRating(clubEloDataLoader);
        double elo2 = club2.getEloRating(clubEloDataLoader);

        // If no score known -> simulate first leg
        if (club1Goals1stLeg == null) {
            // Simulate first leg
            simulateMatch(elo1, elo2, true);
            // If double-legged, wait for second leg
            if (!singleLegged)
                return;
        }
        // If double-legged, simulate second leg: club2 at home
        if (!singleLegged) {
            simulateMatch(elo2, elo1, false);
        }

        // Check aggregate before potential ET/penalties
        if (getClub1Goals() != getClub2Goals()) {
            club1Winner = getClub1Goals() > getClub2Goals();
            return;
        }

        simulateExtraTime(elo2, elo1); // simulate ET with club2 at home

        // Check aggregate after ET
        if (getClub1Goals() != getClub2Goals()) {
            club1Winner = getClub1Goals() > getClub2Goals();
            return;
        }

        // Penalties in second leg
        boolean homePenaltyWinner = simulatePenaltyWinner(elo2, elo1);
        club1Winner = !homePenaltyWinner;
    }

    private void simulateOutCome(double elo1, double elo2) {
        int club1WinnerCount = 0;
        for (int i = 0; i < SIMS; i++) {
            club1Goals1stLeg = null;
            club2Goals1stLeg = null;
            club1Goals2ndLeg = null;
            club2Goals2ndLeg = null;
            club1Winner = null;
            simulateMatch(elo1, elo2, true);
            simulateMatch(elo2, elo1, false);

            // System.out.println(club1Goals + "-" + club2Goals);

            if (getClub1Goals() != getClub2Goals()) {
                club1Winner = getClub1Goals() > getClub2Goals();
                club1WinnerCount += club1Winner ? 1 : 0;
                continue;
            }

            simulateExtraTime(elo2, elo1);

            if (getClub1Goals() != getClub2Goals()) {
                club1Winner = getClub1Goals() > getClub2Goals();
                club1WinnerCount += club1Winner ? 1 : 0;
                continue;
            }

            // Penalties in second leg
            // simulatePenaltyWinner expects (eloHome, eloAway) where home is venue of
            // penalties (club2)
            boolean homePenaltyWinner = simulatePenaltyWinner(elo2, elo1);
            // if homePenaltyWinner == true => club2 wins penalties => club1 loses
            club1Winner = !homePenaltyWinner;
            club1WinnerCount += club1Winner ? 1 : 0;
        }
        club1Goals1stLeg = null;
        club2Goals1stLeg = null;
        club1Goals2ndLeg = null;
        club2Goals2ndLeg = null;
        club1Winner = null;
        System.out.println("Probabilities after " + SIMS + " sims: " + clubSlot1.toCompactString() + " win "
                + (club1WinnerCount * 100.0 / SIMS) + "%, " + clubSlot2.toCompactString() + " win "
                + ((SIMS - club1WinnerCount) * 100.0 / SIMS) + "%");
    }

    @Override
    public String toString() {
        return "KnockoutTie [" + fieldsToString() + ", club1Winner=" + club1Winner + ", singleLegged=" + singleLegged
                + "]";
    }
}
