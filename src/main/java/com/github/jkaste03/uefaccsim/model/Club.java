package com.github.jkaste03.uefaccsim.model;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.jkaste03.uefaccsim.enums.Country;

/**
 * Represents a football club with a name, country, and UEFA ranking.
 */
public class Club {

    private static int id_counter = 0;
    private int id;
    private String name;
    private Country country;
    private float ranking;
    private AtomicInteger timesSeeded;
    private AtomicInteger timesUnseeded;

    /**
     * No-arg constructor used by Gson. Initializes id, counters and registers the
     * club in the {@code ClubRepository}. Field values (name, country, ranking)
     * will be populated by Gson after construction.
     */
    public Club() {
        this.id = id_counter++;
        this.timesSeeded = new AtomicInteger(0);
        this.timesUnseeded = new AtomicInteger(0);
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

    // TODO: Needs changing
    public int getTimesSeeded() {
        return timesSeeded.get();
    }

    // TODO: Needs changing
    public int getTimesUnseeded() {
        return timesUnseeded.get();
    }

    // TODO: Needs changing
    public void incrementSeedingCounter(boolean isSeeded) {
        if (isSeeded) {
            timesSeeded.incrementAndGet();
        } else {
            timesUnseeded.incrementAndGet();
        }
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