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
     * Kjører én simulering av returkampen (eller simulerer begge ben hvis ingen
     * ben er spilt) og setter club1Winner = true/false.
     *
     * Behaviour:
     * - If first leg (club1Goals) is null: simulate FIRST LEG (club1 home), THEN
     * SECOND LEG (club2 home). Apply away-goals, then ET (in second leg), then
     * penalties.
     * - If first leg exists: treat that as first leg result (clubSlot1 home),
     * simulate return leg (clubSlot2 home), compute aggregate, apply away-goals,
     * then ET and penalties if needed.
     *
     * Note: Away-goals are applied before ET (common in many competitions). Adjust
     * logic if your tournament rules differ.
     */
    @Override
    public void play(ClubEloDataLoader clubEloDataLoader) {
        ClubIdWrapper club1 = clubSlot1.getClubIdWrapper();
        ClubIdWrapper club2 = clubSlot2.getClubIdWrapper();

        double elo1 = club1.getEloRating(clubEloDataLoader);
        double elo2 = club2.getEloRating(clubEloDataLoader);

        // If no score known -> simulate first leg
        if (club1Goals == null) {
            // simulateOutCome(elo1, elo2);

            // Simulate first leg
            simulateMatch(elo1, elo2, true);
            // System.out.println("First leg result: " + club1.getName() + " " + club1Goals
            // + " - " + club2Goals + " "
            // + club2.getName());
            // If double-legged, wait for second leg
            if (!singleLegged)
                return;
        }
        // If double-legged, simulate second leg: club2 at home
        if (!singleLegged) {
            simulateMatch(elo2, elo1, false);
        }

        if (club1Goals != club2Goals) {
            club1Winner = club1Goals > club2Goals;
            // System.out.println("Aggregate result: " + club1.getName() + " " + club1Goals
            // + " - " + club2Goals + " "
            // + club2.getName());
            return;
        }

        // is club2,
        // but we want to add ET goals to aggregates for club1/club2 correctly.
        // We simulated as (elo1, elo2) above for convenience; adjust to simulate ET at
        // second leg venue:
        // Re-simulate ET correctly at second leg venue:
        simulateExtraTime(elo2, elo1); // simulate ET with club2 at home
        // et.home == extra-time goals for club2; et.away == extra-time goals for club1

        if (club1Goals != club2Goals) {
            club1Winner = club1Goals > club2Goals;
            // System.out.println("Aggregate result (AET): " + club1.getName() + " " +
            // club1Goals + " - " + club2Goals
            // + " " + club2.getName());
            return;
        }

        // Penalties in second leg
        // simulatePenaltyWinner expects (eloHome, eloAway) where home is venue of
        // penalties (club2)
        boolean homePenaltyWinner = simulatePenaltyWinner(elo2, elo1);
        // if homePenaltyWinner == true => club2 wins penalties => club1 loses
        club1Winner = !homePenaltyWinner;
        // System.out.println("Aggregate result: " + club1.getName() + " " + club1Goals
        // + " - " + club2Goals + " "
        // + club2.getName());
        // System.out.println("Penalty shootout winner: " + (club1Winner ?
        // club1.getName() : club2.getName()));
    }

    private void simulateOutCome(double elo1, double elo2) {
        int club1WinnerCount = 0;
        for (int i = 0; i < SIMS; i++) {
            club1Goals = null;
            club2Goals = null;
            club1Winner = null;
            simulateMatch(elo1, elo2, true);
            simulateMatch(elo2, elo1, false);

            // System.out.println(club1Goals + "-" + club2Goals);

            if (club1Goals != club2Goals) {
                club1Winner = club1Goals > club2Goals;
                club1WinnerCount += club1Winner ? 1 : 0;
                continue;
            }

            simulateExtraTime(elo2, elo1);

            if (club1Goals != club2Goals) {
                club1Winner = club1Goals > club2Goals;
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
        club1Goals = null;
        club2Goals = null;
        club1Winner = null;
        System.out.println("Probabilities after " + SIMS + " sims: " + clubSlot1.toCompactString() + " win "
                + (club1WinnerCount * 100.0 / SIMS) + "%, " + clubSlot2.toCompactString() + " win "
                + ((SIMS - club1WinnerCount) * 100.0 / SIMS) + "%");
    }
}
