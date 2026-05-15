package com.github.jkaste03.uefaccsim.model;

import com.github.jkaste03.uefaccsim.enums.Country;
import com.github.jkaste03.uefaccsim.repository.ClubRepository;

/**
 * Represents a football club with a name, country, and UEFA ranking.
 */
public class Club {

    private static int id_counter = 0;
    private int id;
    private String name;
    private Country country;
    private float ranking;

    /**
     * No-arg constructor used by Gson. Initializes id, counters and registers the
     * club in the {@code ClubRepository}. Field values (name, country, ranking)
     * will be populated by Gson after construction.
     */
    public Club() {
        this.id = id_counter++;
        ClubRepository.addClub(this);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Country getCountry() {
        return country;
    }

    public float getRanking() {
        return ranking;
    }

    @Override
    public String toString() {
        return "Club{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", country=" + country +
                ", coefficientRanking=" + ranking +
                '}';
    }
}