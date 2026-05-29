package com.github.jkaste03.uefaccsim.model.competition;

import com.github.jkaste03.uefaccsim.repository.ClubSimStateRepository;

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
     * Constructs a new non-knockout tie with the specified club slots and goals.
     * Order matters.
     */
    public NonKnockoutTie(ClubSlot clubA, ClubSlot clubB, int clubAGoals1stLeg, int clubBGoals1stLeg) {
        super(clubA, clubB, clubAGoals1stLeg, clubBGoals1stLeg);
    }

    /**
     * Plays a single non-knockout fixture.
     *
     * <p>
     * <strong>Behavior</strong>
     * </p>
     * <ul>
     * <li>Simulates one regular-season style match with Club A as the home
     * side.</li>
     * <li>Applies an Elo rating update for the result using a full K-factor.</li>
     * </ul>
     *
     * <p>
     * <strong>Elo update</strong>
     * </p>
     * <ul>
     * <li>Accounts for home advantage (home = Club A).</li>
     * <li>Uses the configured K-factor ({@code PARAM_ELO_UPDATE_K}).</li>
     * <li>Persists rating changes via the provided repository.</li>
     * </ul>
     *
     * <p>
     * <strong>Side effects</strong>
     * </p>
     * <ul>
     * <li>Mutates this tie's simulated match state (e.g., goals and outcome).</li>
     * <li>Updates the participating clubs’ Elo ratings through the repository.</li>
     * </ul>
     *
     * @param clubSimStateRepo repository used to read and update club state and
     *                         Elo ratings
     * @see #simulateMatch(boolean)
     * @see #updateEloForResult(int, int, boolean, double, ClubSimStateRepository)
     */
    public void play(ClubSimStateRepository clubSimStateRepo) {
        // Single regular-season style match: clubSlotA is home
        simulateMatch(true);
        // Apply Elo update for the result (full K)
        updateEloForResult(clubAGoals1stLeg, clubBGoals1stLeg, true, PARAM_ELO_UPDATE_K, clubSimStateRepo);
    }

    private void simulateMatch(boolean firstLeg) {
        simulateMatch(firstLeg, false);
    }

    @Override
    public String toString() {
        return "NonKnockoutTie [" + fieldsToString() + "]";
    }
}
