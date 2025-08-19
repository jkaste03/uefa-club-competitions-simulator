package com.github.jkaste03.seeding_prob_finder.model;

import com.github.jkaste03.seeding_prob_finder.enums.Tournament;
import com.github.jkaste03.seeding_prob_finder.service.ClubEloDataLoader;

/**
 * SingleLeggedTie is a specialized implementation of the Tie class that
 * represents a single-legged tie between two clubs.
 * <p>
 * This class extends the abstract Tie class and implements the specific
 * behavior for a single-legged tie, including score calculation and determining
 * the winner.
 */
public class SingleLeggedTie extends Tie {

    /*
     * * Constructs a new single-legged tie with the specified club slots.
     */
    public SingleLeggedTie(ClubSlot club1, ClubSlot club2) {
        super(club1, club2);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void play(ClubEloDataLoader clubEloDataLoader) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'play'");
    }

    @Override
    public float getRanking(Tournament callerTournament) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRanking'");
    }
}
