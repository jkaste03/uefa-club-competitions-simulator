package com.github.jkaste03.uefaccsim.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class serves as a repository for managing club instances in the UEFA
 * competition simulations. It provides methods to store and retrieve clubs
 * based on a unique identifier. All clubs are stored in a static map, ensuring
 * consistent access throughout the application.
 */
public class ClubRepository {
    /*
     * The static map of clubs, where the key is the club's id and the value is the
     * Club instance.
     */
    private static final Map<Integer, Club> clubs = new HashMap<>();

    /*
     * The id of the club that won the last UEFA Champions League. This is needed
     * decide the UCL seeding pots.
     */
    public static int lastUclWinnerId;

    public static int getLastUclWinnerId() {
        return lastUclWinnerId;
    }

    public static void setLastUclWinnerId(int id) {
        lastUclWinnerId = id;
    }

    /*
     * Retrieves the club with the specified id from the static map of clubs.
     */
    public static Club getClub(int id) {
        return clubs.get(id);
    }

    /*
     ** Retrieves a club's id by its name from the static map of clubs.
     */
    public static int getIdByName(String name) {
        return clubs.entrySet().stream()
                .filter(entry -> entry.getValue().getName().equals(name))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1); // Return -1 if the club is not found
    }

    /**
     * Returns a new mutable list containing a snapshot of every Club currently
     * registered in this repository.
     */
    public static ArrayList<Club> getAllClubs() {
        return new ArrayList<>(clubs.values());
    }

    /*
     * Adds a club to the static map of clubs. This method is used to populate the
     * repository with clubs.
     */
    public static void addClub(Club club) {
        clubs.put(club.getId(), club);
    }
}