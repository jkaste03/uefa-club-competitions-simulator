package com.github.jkaste03.uefaccsim.model.competition;

import java.util.List;
import java.util.Set;

import com.github.jkaste03.uefaccsim.enums.Leg;
import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.repository.ClubSimStateRepository;

import java.util.ArrayList;
import java.util.HashSet;

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

    @Override
    public List<KnockoutTie> getTies() {
        return ties;
    }

    /**
     * Adds a tie to this knockout round. Only KnockoutTie instances are allowed.
     * <p>
     * This method is only intended to be called during the pre-simulation phase.
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
        ties.add((KnockoutTie) tie);
    }

    /**
     * Runs structural pre-simulation validation for ties in this round.
     * <p>
     * This method should only be called in the pre-simulation phase after ties have
     * been added to the round, as it performs slow validation checks. It ensures
     * the integrity of the round's tie configuration and prevents issues during
     * simulation caused by invalid tie setups.
     * <p>
     * The validation checks only include necessary checks to avoid completely
     * unsensible tie configurations, like having the same club slot in both
     * positions of a tie. <b>UEFA restrictions are not checked here</b> (such as
     * "avoiding political ties"), as that is not the scope of this validation.
     * <p>
     * The method performs three checks in order:
     * <ol>
     * <li>For each tie, verifies that both club slots belong to this round's slot
     * pool ({@link #validateTieClubSlotsBelongToRound(Tie)}).</li>
     * <li>Verifies across the entire round that each club slot appears in at most
     * one tie ({@link #validateNoDuplicateClubSlotsInTies()}).</li>
     * <li>If this is a {@link PostLeagueKnockoutRound}, verifies that a partial
     * draw has not been completed, which is not allowed.</li>
     * </ol>
     * <p>
     */
    @Override
    public void validateTiesPreSim() {
        // For all ties...
        for (Tie tie : ties) {
            // ...validate that both club slots are part of this round's club slots
            validateTieClubSlotsBelongToRound(tie);
        }
        // Validate that no club slot appears in more than one tie in this round
        validateNoDuplicateClubSlotsInTies();

        // If this is a post-league knockout round, also validate that a partial draw
        // has not already been completed.
        if (this instanceof PostLeagueKnockoutRound) {
            ((PostLeagueKnockoutRound) this).validateTieCount(); // This will throw if a partial draw has been
                                                                 // completed, which is not allowed.
        }
    }

    /**
     * Validates that the ties doesn't contain duplicate club slots. Only call this
     * method during the pre-simulation phase, as it is a slow validation check.
     */
    private void validateNoDuplicateClubSlotsInTies() {
        Set<ClubSlot> seenClubSlots = new HashSet<>();
        for (KnockoutTie tie : ties) {
            // If we have already seen either club slot in this tie, that means it's a
            // duplicate and we should throw an exception
            if (!seenClubSlots.add(tie.getClubSlotA())) {
                throw new IllegalStateException(
                        "Duplicate club slot found in " + getName() + ". Club slot: "
                                + tie.getClubSlotA().toCompactString());
            }
            if (!seenClubSlots.add(tie.getClubSlotB())) {
                throw new IllegalStateException(
                        "Duplicate club slot found in " + getName() + ". Club slot: "
                                + tie.getClubSlotB().toCompactString());
            }
        }
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
