package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class serves as a repository for managing club instances in the UEFA
 * competition simulations. It provides methods to store and retrieve clubs
 * based on a unique identifier. All clubs are stored in a static map, ensuring
 * consistent access throughout the application.
 * <p>
 * The ClubRepository class functions as the storage location where all
 * simulation data produced through the simulations is stored for all clubs.
 */
public class ClubRepository {
    /*
     * The static map of clubs, where the key is the club's id and the value is the
     * Club instance.
     */
    private static Map<Integer, Club> clubs = new HashMap<>();

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

    public static ArrayList<Club> getAllClubs() {
        return new ArrayList<>(clubs.values());
    }

    /*
     * Adds a club to the static map of clubs. This method is used to populate the
     * repository with club data, which can be used for simulations and other
     */
    public static void addClub(Club club) {
        clubs.put(club.getId(), club);
    }

    // Add this method to your ClubRepository class
    public static Club getClubByName(String name) {
        return getAllClubs().stream()
                .filter(club -> club.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}