package com.github.jkaste03.uefaccsim.model.competition;

import java.io.Serializable;
import java.util.Objects;

import com.github.jkaste03.uefaccsim.model.Club;
import com.github.jkaste03.uefaccsim.repository.ClubRepository;

/**
 * Simulation state for a single club within the UEFA competitions simulator.
 * <p>
 * This class tracks:
 * <ul>
 * <li>The immutable club identifier ({@code id}) used to resolve the club from
 * {@code ClubRepository}.</li>
 * <li>The club's current Elo rating in the simulation ({@code elo}).</li>
 * <li>An "uncommitted" Elo delta ({@code uncommitedEloDelta}) that accumulates
 * rating changes during
 * intermediate steps (e.g., within a matchday or round) and is applied
 * atomically via {@link #commitEloDelta()}.</li>
 * </ul>
 * It also provides convenience accessors to the underlying {@code Club}.
 * <p>
 *
 * @see ClubRepository
 * @see Club
 */
public final class ClubSimState implements Serializable {

    private final int id;
    /** The club's current Elo rating; -1 if not yet initialized. */
    private double elo = -1;
    /** This represents the change in Elo that has not yet been applied. */
    private double uncommitedEloDelta;

    /**
     * Constructs a new ClubSimState initialized with the provided identifier.
     *
     * @param id the unique identifier for this simulation state
     */
    public ClubSimState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public double getElo() {
        return elo;
    }

    public void setElo(double elo) {
        this.elo = elo;
    }

    public double getUncommitedEloDelta() {
        return uncommitedEloDelta;
    }

    public void setUncommitedEloDelta(double uncommitedEloDelta) {
        this.uncommitedEloDelta = uncommitedEloDelta;
    }

    public void updateUncommitedEloDelta(double delta) {
        this.uncommitedEloDelta += delta;
    }

    /**
     * Commits the pending Elo rating change to the current Elo and clears the
     * pending value.
     */
    public void commitEloDelta() {
        this.elo += this.uncommitedEloDelta;
        this.uncommitedEloDelta = 0.0;
    }

    /**
     * Returns the underlying Club instance associated with this simulation state.
     * 
     * @return the Club instance
     */
    public Club getClub() {
        return ClubRepository.getClub(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ClubSimState other))
            return false;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ClubSimState [id=" + id + ", elo=" + elo + ", uncommitedEloDelta=" + uncommitedEloDelta + "]";
        // return ClubRepository.getClub(id).toString();
    }
}