package com.github.jkaste03.uefaccsim.model;

import java.io.Serializable;

import com.github.jkaste03.uefaccsim.service.ClubEloDataLoader;

/**
 * Immutable wrapper around a club identifier that provides convenient,
 * on‑demand access to derived club properties without duplicating or eagerly
 * loading the underlying club data.
 */
public record ClubIdWrapper(int id) implements Serializable {

    // Getters and methods to access club properties

    public String getName() {
        return ClubRepository.getClub(id).getName();
    }

    public float getRanking() {
        return ClubRepository.getClub(id).getRanking();
    }

    /**
     * Retrieves the current Elo rating for this club using the provided data
     * loader.
     *
     * @param clubEloDataLoader the loader responsible for supplying Elo ratings
     * @return the Elo rating associated with this club's identifier
     */
    public double getEloRating(ClubEloDataLoader clubEloDataLoader) {
        return clubEloDataLoader.getEloRating(id);
    }

    // TODO: Needs changing
    public void incrementSeedingCounter(boolean isSeeded) {
        ClubRepository.getClub(id).incrementSeedingCounter(isSeeded);
    }

    @Override
    public String toString() {
        return ClubRepository.getClub(id).toString();
    }
}