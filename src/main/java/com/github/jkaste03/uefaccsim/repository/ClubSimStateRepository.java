package com.github.jkaste03.uefaccsim.repository;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.jkaste03.uefaccsim.model.competition.ClubSimState;

/**
 * Repository for runtime club state such as Elo and uncommitted Elo deltas.
 * Provides a single source of truth for ClubSimState instances keyed by club
 * id.
 */
public final class ClubSimStateRepository implements Serializable {

    /** Map of club IDs to their simulation states */
    private final Map<Integer, ClubSimState> wrappers = new HashMap<>();
    /** Tracks which club states currently have an uncommitted Elo delta */
    private final Set<ClubSimState> pendingStates = new HashSet<>();

    /**
     * Creates a new ClubSimState for the given id, stores it in the map,
     * and returns it.
     */
    public ClubSimState create(int id) {
        ClubSimState w = new ClubSimState(id);
        wrappers.put(id, w);
        return w;
    }

    public ClubSimState get(int id) {
        return wrappers.get(id);
    }

    public boolean contains(int id) {
        return wrappers.containsKey(id);
    }

    /**
     * Stages a non-persistent Elo rating adjustment for the specified club. Also
     * marks the club as having a pending delta so it can be committed or cleared in
     * a subsequent step.
     *
     * @param id    the id of the club whose uncommitted Elo delta will be updated
     * @param delta the amount to add to the club's current uncommitted Elo delta
     *              (positive or negative)
     */
    public void updateUncommittedEloDelta(int id, double delta) {
        ClubSimState w = get(id);
        w.updateUncommitedEloDelta(delta);
        pendingStates.add(w);
    }

    /**
     * Commits all accumulated Elo rating deltas for the currently pending club
     * simulation states.
     */
    public void applyAllUncommittedEloDeltas() {
        for (ClubSimState s : pendingStates) {
            s.commitEloDelta();
        }
        pendingStates.clear();
    }

    @Override
    public String toString() {
        return "ClubSimStateRepository [wrappers=" + wrappers + ", pendingStates=" + pendingStates + "]";
    }
}
