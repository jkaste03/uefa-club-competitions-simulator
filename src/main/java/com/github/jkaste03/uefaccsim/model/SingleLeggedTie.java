package com.github.jkaste03.uefaccsim.model;

import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.service.ClubEloDataLoader;

/**
 * SingleLeggedTie is a specialized implementation of the Tie class that
 * represents a single-legged tie between two clubs.
 * <p>
 * This class extends the abstract Tie class and implements the specific
 * behavior for a single-legged tie, including playing the tie.
 */
public class SingleLeggedTie extends Tie {
    /**
     * Indicates whether this tie is a knockout match. This affects the play logic.
     */
    private final boolean knockout;

    /*
     * Constructs a new single-legged tie with the specified club slots. Order
     * matters. Tournament is important in QRounds.
     */
    public SingleLeggedTie(ClubSlot club1, ClubSlot club2, boolean knockout, Tournament tournament) {
        super(club1, club2, tournament);
        this.knockout = knockout;
    }

    /*
     * Constructs a new single-legged tie with the specified club slots. Order
     * matters.
     */
    public SingleLeggedTie(ClubSlot club1, ClubSlot club2, boolean knockout) {
        super(club1, club2);
        this.knockout = knockout;
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

        simulateMatch(elo1, elo2, true);

        // Tournament is always null in non-knockout rounds
        if (!knockout)
            return;

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
        simulateExtraTime(elo1, elo2); // simulate ET with club2 at home
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
        boolean homePenaltyWinner = simulatePenaltyWinner(elo1, elo2);
        // if homePenaltyWinner == true => club2 wins penalties => club1 loses
        club1Winner = homePenaltyWinner;
        // System.out.println("Aggregate result: " + club1.getName() + " " + club1Goals
        // + " - " + club2Goals + " "
        // + club2.getName());
        // System.out.println("Penalty shootout winner: " + (club1Winner ?
        // club1.getName() : club2.getName()));
    }
}
