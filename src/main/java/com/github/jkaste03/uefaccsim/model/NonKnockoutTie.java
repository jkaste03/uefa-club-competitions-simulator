package com.github.jkaste03.uefaccsim.model;

import com.github.jkaste03.uefaccsim.service.ClubEloDataLoader;

/**
 * NonKnockoutTie is a specialized implementation of the Tie class that
 * represents a normal tie that isn’t a knockout tie between two clubs.
 * <p>
 * This class extends the abstract Tie class and implements the specific
 * behavior for a non-knockout tie, including playing the tie.
 */
public class NonKnockoutTie extends Tie {
    /*
     * Constructs a new non-knockout tie with the specified club slots. Order
     * matters.
     */
    public NonKnockoutTie(ClubSlot club1, ClubSlot club2) {
        super(club1, club2);
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

        System.out.println(
                "Result: " + club1.getName() + " " + club1Goals1stLeg + " - " + club2Goals1stLeg + " "
                        + club2.getName());
    }

    @Override
    public String toString() {
        return "NonKnockoutTie [" + fieldsToString() + "]";
    }
}
