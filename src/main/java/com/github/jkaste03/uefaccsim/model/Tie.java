package com.github.jkaste03.uefaccsim.model;

import java.io.Serializable;
import java.util.Random;

import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.service.ClubEloDataLoader;

/**
 * Abstract base for a tie between two club slots. May hold goals.
 */
public abstract class Tie implements Serializable {
    protected final ClubSlot clubSlot1;
    protected final ClubSlot clubSlot2;
    protected Integer club1Goals;
    protected Integer club2Goals;
    protected Boolean club1Winner;
    /**
     * The tournament this tie is part of. This is needed
     */
    protected final Tournament tournament;

    protected static final int SIMS = 10_000; // kun én simulering
    protected static final double HFA = 50; // hjemmebanefordel
    protected static final double AVG_GOALS = 2.7;
    protected static final Random rnd = new Random();

    /**
     * Constructs a new Tie representing a pairing between two club slots for a
     * specific tournament. Order matters
     *
     * @param clubSlot1  home participant first leg
     * @param clubSlot2  away participant first leg
     * @param tournament tournament
     */
    public Tie(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament) {
        this.clubSlot1 = clubSlot1;
        this.clubSlot2 = clubSlot2;
        this.tournament = tournament;
    }

    /**
     * Constructs a new Tie representing a pairing between two club slots with
     * preset goals for a specific tournament. Order matters.
     *
     * @param clubSlot1  home participant first leg
     * @param clubSlot2  away participant first leg
     * @param club1Goals goals for club 1
     * @param club2Goals goals for club 2
     * @param tournament tournament
     */
    public Tie(ClubSlot clubSlot1, ClubSlot clubSlot2, Integer club1Goals, Integer club2Goals, Tournament tournament) {
        this.clubSlot1 = clubSlot1;
        this.clubSlot2 = clubSlot2;
        this.club1Goals = club1Goals;
        this.club2Goals = club2Goals;
        this.tournament = tournament;
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

    public Boolean isClub1Winner() {
        return club1Winner;
    }

    public Tournament getTournament() {
        return tournament;
    }

    /**
     * Computes this tie's effective ranking relative to a caller tournament by
     * selecting one of the two underlying club rankings. If the caller tournament
     * is at a worse level than this tie's tournament, the worst ranking is
     * returned; otherwise, the best ranking is returned.
     *
     * @param callerTournament the tournament context requesting the ranking
     * @return the ranking
     */
    public float getRanking(Tournament callerTournament) {
        float r1 = clubSlot1.getRanking(tournament), r2 = clubSlot2.getRanking(tournament);
        return ((tournament.compareTo(callerTournament) > 0) ^ (r1 < r2)) ? r1 : r2;
    }

    public void incrementSeedingCounter(boolean isSeeded) {
        clubSlot1.incrementSeedingCounter(isSeeded);
        clubSlot2.incrementSeedingCounter(isSeeded);
    }

    /**
     * Simulates the tie.
     * <p>
     * Implementing methods should play the tie, update the results,
     * and set the winner based on the tie outcome.
     */
    public abstract void play(ClubEloDataLoader clubEloDataLoader);

    /**
     * Returns a compact "Club1 vs Club2" string for the tie.
     * 
     * @return compact string representation of the tie
     */
    public String toCompactString() {
        return clubSlot1.toCompactString() + " vs " +
                clubSlot2.toCompactString();
    }

    @Override
    public String toString() {
        return "Tie[" + fieldsToString() + "]";
    }

    /**
     * Returns a concise, comma-separated textual representation of this tie's key
     * fields. Used by all Tie subclasses in their toString implementations.
     *
     * @return a human-readable summary of this tie's state
     */
    protected String fieldsToString() {
        return "clubSlot1=" + clubSlot1 +
                ", clubSlot2=" + clubSlot2 +
                ", club1Goals=" + club1Goals +
                ", club2Goals=" + club2Goals;
    }
}
