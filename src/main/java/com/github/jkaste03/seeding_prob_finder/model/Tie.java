package com.github.jkaste03.seeding_prob_finder.model;

import java.io.Serializable;
import java.util.Random;

import com.github.jkaste03.seeding_prob_finder.enums.CompetitionData.Tournament;
import com.github.jkaste03.seeding_prob_finder.service.ClubEloDataLoader;

/**
 * Represents a tie between two clubs at a specific competition level.
 * <p>
 * A {@code Tie} consists of two {@link Club} instances and an integer
 * indicating
 * the competition level at which the tie occurs. The class provides methods to
 * access the clubs and competition level, as well as to calculate a ranking
 * value for the tie relative to a given competition level.
 * <p>
 * The ranking calculation considers whether the tie's competition level is
 * higher
 * or lower than the caller's level, returning either the best or worst ranking
 * among the two clubs accordingly.
 *
 * @author jkaste03
 */
public abstract class Tie implements Serializable {
    protected ClubSlot clubSlot1;
    protected ClubSlot clubSlot2;
    protected Integer club1Goals;
    protected Integer club2Goals;
    protected Boolean club1Winner;

    protected static final int SIMS = 10_000; // kun én simulering
    protected static final double HFA = 50; // hjemmebanefordel
    protected static final double AVG_GOALS = 2.7;
    protected static final Random rnd = new Random();

    public Tie(ClubSlot clubSlot1, ClubSlot clubSlot2) {
        this.clubSlot1 = clubSlot1;
        this.clubSlot2 = clubSlot2;
    }

    public Tie(ClubSlot clubSlot1, ClubSlot clubSlot2, Integer club1Goals, Integer club2Goals) {
        this.clubSlot1 = clubSlot1;
        this.clubSlot2 = clubSlot2;
        this.club1Goals = club1Goals;
        this.club2Goals = club2Goals;
    }

    public ClubSlot getClubSlot1() {
        return clubSlot1;
    }

    public ClubSlot getClubSlot2() {
        return clubSlot2;
    }

    public Integer getClub1Goals() {
        return club1Goals;
    }

    public Integer getClub2Goals() {
        return club2Goals;
    }

    /** Getter for å sjekke hvem som vant etter playOutcome() */
    public Boolean isClub1Winner() {
        return club1Winner;
    }

    public void incrementSeedingCounter(boolean isSeeded) {
        clubSlot1.incrementSeedingCounter(isSeeded);
        clubSlot2.incrementSeedingCounter(isSeeded);
    }

    /**
     * Get the ranking of the tie given the caller's tournament context.
     */
    public abstract float getRankingAndResolveSlots(Tournament callerTournament);

    /**
     * Simulates the match.
     * <p>
     * Implementing methods should perform the match, update the results,
     * and set the winner based on the match outcome.
     */
    public abstract void play(ClubEloDataLoader clubEloDataLoader);

    @Override
    public String toString() {
        return "Tie{" +
                ", clubSlot1=" + clubSlot1 +
                ", clubSlot2=" + clubSlot2 +
                ", club1Goals=" + club1Goals +
                ", club2Goals=" + club2Goals +
                '}';
    }

    public String toCompactString() {
        return clubSlot1.toCompactString() + " vs " +
                clubSlot2.toCompactString();
    }
}
