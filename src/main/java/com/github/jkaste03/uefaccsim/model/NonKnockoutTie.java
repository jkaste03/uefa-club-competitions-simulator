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
    public NonKnockoutTie(ClubSlot clubA, ClubSlot clubB) {
        super(clubA, clubB);
    }

    /**
     * Kjører én simulering av returkampen (eller simulerer begge ben hvis ingen
     * ben er spilt) og setter clubAWinner = true/false.
     *
     * Behaviour:
     * - If first leg (clubAGoals) is null: simulate FIRST LEG (clubA home), THEN
     * SECOND LEG (clubB home). Apply away-goals, then ET (in second leg), then
     * penalties.
     * - If first leg exists: treat that as first leg result (clubSlotA home),
     * simulate return leg (clubSlotB home), compute aggregate, apply away-goals,
     * then ET and penalties if needed.
     *
     * Note: Away-goals are applied before ET (common in many competitions). Adjust
     * logic if your tournament rules differ.
     */
    @Override
    public void play(ClubEloDataLoader clubEloDataLoader) {
        ClubIdWrapper clubA = clubSlotA.getClubIdWrapper();
        ClubIdWrapper clubB = clubSlotB.getClubIdWrapper();

        simulateMatch(true, clubEloDataLoader);

        System.out.println(
                "Result: " + clubA.getName() + " " + clubAGoals1stLeg + " - " + clubBGoals1stLeg + " "
                        + clubB.getName());
    }

    private void simulateMatch(boolean firstLeg, ClubEloDataLoader clubEloDataLoader) {
        simulateMatch(firstLeg, false, clubEloDataLoader);
    }

    @Override
    public String toString() {
        return "NonKnockoutTie [" + fieldsToString() + "]";
    }
}
