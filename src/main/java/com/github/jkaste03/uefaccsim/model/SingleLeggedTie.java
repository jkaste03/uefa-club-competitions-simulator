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

    /*
     * Constructs a new single-legged tie with the specified club slots. Order
     * matters. Tournament is important in QRounds.
     */
    public SingleLeggedTie(ClubSlot club1, ClubSlot club2, Tournament tournament) {
        super(club1, club2, tournament);
    }

    /*
     * Constructs a new single-legged tie with the specified club slots. Order
     * matters.
     */
    public SingleLeggedTie(ClubSlot club1, ClubSlot club2) {
        super(club1, club2);
    }

    @Override
    public void play(ClubEloDataLoader clubEloDataLoader) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'play'");
    }
}
