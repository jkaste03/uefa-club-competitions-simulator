package com.github.jkaste03.uefaccsim.model;

import com.github.jkaste03.uefaccsim.enums.PathType;
import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;

/**
 * Represents a qualifying round in a tournament where each tie is played as a
 * single-legged match.
 * 
 * @see QRound
 * @see SingleLeggedTie
 */
public class QRoundSingleLegged extends QRound {

    public QRoundSingleLegged(Tournament tournament, RoundType roundType, PathType pathType) {
        super(tournament, roundType, pathType);
    }

    /**
     * Creates a new single-legged tie between two club slots for the tournament.
     * Order of clubs matters.
     *
     * @param club1 the first club slot participating in the tie
     * @param club2 the second club slot participating in the tie
     * @return a {@link SingleLeggedTie} instance representing the match between the
     *         two clubs
     */
    @Override
    protected Tie newTie(ClubSlot club1, ClubSlot club2) {
        return new SingleLeggedTie(club1, club2, tournament);
    }
}
