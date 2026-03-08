package com.github.jkaste03.uefaccsim.model.competition;

import java.util.List;
import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.repository.ClubSimStateRepository;

import java.util.ArrayList;

/**
 * Class representing a knockout round in the UEFA competitions.
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

    public boolean isSingleLegged() {
        return isSingleLegged;
    }

    /**
     * Seeds and draws the ties. Scheduling isn't relevant for knockout rounds.
     */
    @Override
    public void seedDrawSchedule() {
        seed();
        draw();
    }

    /**
     * Registers all current ties (matchups) into their appropriate slots for the
     * upcoming rounds in the competition structure.
     * <p>
     * Behavior:
     * <ul>
     * <li>Every tie is always added to the next primary round via
     * {@code nextPrimaryRnd.addClubSlot(...)}.</li>
     * <li>If a secondary round exists:
     * <ul>
     * <li>Every tie is added to the next secondary round via
     * {@code nextSecondaryRnd.addClubSlot(...)}.</li>
     * </ul>
     * </li>
     * </ul>
     * </p>
     */
    public void regTiesForNextRounds() {
        // Add ties to the next primary round and the next secondary round if applicable
        ties.forEach(tie -> {
            // Add tie to the next primary round
            this.nextPrimaryRnd.addClubSlot(new ClubSlot(tie));
            // Add tie to the next secondary round if applicable
            if (this.nextSecondaryRnd != null) {
                // Add tie to the secondary round
                this.nextSecondaryRnd.addClubSlot(new ClubSlot(tie));
            }
        });
    }

    /**
     * Plays the round. This method is responsible for playing the round.
     */
    public void play(ClubSimStateRepository clubSimStateRepo) {
        for (Tie tie : getTies()) {
            tie.play(clubSimStateRepo);
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
