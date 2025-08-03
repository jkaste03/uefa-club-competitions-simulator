package com.github.jkaste03.seeding_prob_finder.model;

import java.io.Serializable;

import com.github.jkaste03.seeding_prob_finder.service.ClubEloDataLoader;

/**
 * ClubIdWrapper is a specialized implementation of ClubSlot that encapsulates a
 * club's unique id.
 * <p>
 * This wrapper delegates the retrieval of club details (such as name, ranking,
 * and associated countries)
 * to the Clubs repository using the stored identifier. It provides a convenient
 * abstraction to access a
 * club's properties without holding a direct reference to the Club object.
 * <p>
 * Example usage:
 * 
 * <pre>
 * ClubIdWrapper wrapper = new ClubIdWrapper(5);
 * String clubName = wrapper.getName();
 * float clubRanking = wrapper.getRanking();
 * </pre>
 */
public class ClubIdWrapper implements Serializable {
    private int id;

    /**
     * Constructs a ClubIdWrapper with the specified club id.
     *
     * @param id the unique identifier of the club
     */
    public ClubIdWrapper(int id) {
        this.id = id;
    }

    public ClubIdWrapper(Club club) {
        this.id = club.getId();
    }

    public int getId() {
        return id;
    }

    public float getRanking() {
        return getClub(id).getRanking();
    }

    public double getEloRating(ClubEloDataLoader clubEloDataLoader) {
        return clubEloDataLoader.getEloRating(id);
    }

    public String getName() {
        return getClub(id).getName();
    }

    private Club getClub(int id) {
        return ClubRepository.getClub(id);
    }

    public void incrementSeedingCounter(boolean isSeeded) {
        getClub(id).incrementSeedingCounter(isSeeded);
    }

    @Override
    public String toString() {
        return getClub(id).toString();
    }
}