package com.github.jkaste03.seeding_prob_finder.model;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.jkaste03.seeding_prob_finder.enums.Country;

/**
 * Represents a football club with a name and ranking.
 *
 * <p>
 * Instances of this class store the club's name and its ranking coefficient.
 * </p>
 *
 * @author jkaste03
 */
public class Club {

    private static int id_counter = 0;
    private int id;
    private String name;
    private Country country;
    private float ranking;
    private AtomicInteger timesSeeded;
    private AtomicInteger timesUnseeded;

    public Club(String name, Country country, float ranking) {
        this.id = id_counter++;
        this.name = name;
        this.country = country;
        this.ranking = ranking;
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

    public int getTimesSeeded() {
        return timesSeeded.get();
    }

    public int getTimesUnseeded() {
        return timesUnseeded.get();
    }

    public void init() {
        this.id = id_counter++;
        timesSeeded = new AtomicInteger(0);
        timesUnseeded = new AtomicInteger(0);
    }

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