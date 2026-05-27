package com.github.jkaste03.uefaccsim.model.competition;

import java.util.List;

import com.github.jkaste03.uefaccsim.enums.Leg;
import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.repository.ClubSimStateRepository;

import java.util.ArrayList;

/**
 * Represents a knockout round in the UEFA competitions.
 *
 * <p>
 * A knockout round owns the {@link KnockoutTie} pairings for the round,
 * controls whether the round is played as single-legged or two-legged ties,
 * and delegates leg-by-leg simulation to the contained ties.
 * </p>
 *
 * <p>
 * Subclasses are responsible for defining how ties are seeded, drawn, and
 * registered for later rounds in the competition structure.
 * </p>
 */
public abstract class KnockoutRound extends Round {
    /**
     * Current tie pairings for this knockout round.
     */
    protected List<KnockoutTie> ties = new ArrayList<>();
    /**
     * Indicates if the ties in this qualifying round should be made single-legged.
     */
    protected boolean isSingleLegged = false;

    /**
     * Constructs a knockout round for the specified tournament and round type
     *
     * @param tournament the tournament of this round.
     * @param roundType  the type of the round (Q1, Q2, etc.).
     */
    public KnockoutRound(Tournament tournament, RoundType roundType) {
        super(tournament, roundType);
    }

    /**
     * Constructs a knockout round for the specified tournament, round type and
     * path type, with an option to have ties single-legged.
     *
     * @param tournament     the tournament of this round.
     * @param roundType      the type of the round (Q1, Q2, etc.).
     * @param isSingleLegged indicates if the ties in this round should be
     *                       single-legged.
     */
    public KnockoutRound(Tournament tournament, RoundType roundType, boolean isSingleLegged) {
        super(tournament, roundType);
        this.isSingleLegged = isSingleLegged;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a string representation of the knockout round, including the
     * tournament and round type.
     */
    @Override
    public String getName() {
        return super.getName() + " " + roundType;
    }

    @Override
    public List<KnockoutTie> getTies() {
        return ties;
    }

    /**
     * Adds a tie to this knockout round. Only KnockoutTie instances are allowed.
     * Only call this method during the pre-simulation phase, as it performs slow
     * validation checks.
     * 
     * @param tie the tie to add
     * @throws IllegalArgumentException if the provided tie is not an instance of
     *                                  KnockoutTie
     */
    @Override
    public void addTiePreSim(Tie tie) {
        // Validate that the provided tie is a KnockoutTie
        if (!(tie instanceof KnockoutTie)) {
            throw new IllegalArgumentException("Only KnockoutTie instances can be added to a KnockoutRound.");
        }
        // Validate that both club slots of the tie are part of this round's club slots
        validateTieClubSlotsBelongToRound(tie);
        // If validation passes, add the tie to the list of ties
        ties.add((KnockoutTie) tie);
    }

    public boolean isSingleLegged() {
        return isSingleLegged;
    }

    /**
     * Seeds and draws the ties. Scheduling isn't relevant for knockout rounds.
     */
    @Override
    protected void seedDrawSchedule() {
        seed();
        draw();
    }

    /**
     * Registers all current ties (or clubs) into their appropriate slots for the
     * upcoming rounds in the competition structure.
     */
    protected abstract void regForNextRounds();

    /**
     * Plays a leg of the round.
     */
    public void play(ClubSimStateRepository clubSimStateRepo, Leg leg) {
        for (KnockoutTie tie : getTies()) {
            tie.play(clubSimStateRepo, leg);
        }
    }

    @Override
    public String toString() {
        return "KnockoutRound [name=" + getName() + fieldsToString() + "]";
    }

    /**
     * Returns a concise, comma-separated textual representation of this
     * KnockoutRound's key fields. Used by all KnockoutRound subclasses in their
     * toString implementations.
     *
     * @return a human-readable summary of this KnockoutRound's state
     */
    @Override
    protected String fieldsToString() {
        return super.fieldsToString() + ", ties=" + ties + ", isSingleLegged=" + isSingleLegged;
    }
}
