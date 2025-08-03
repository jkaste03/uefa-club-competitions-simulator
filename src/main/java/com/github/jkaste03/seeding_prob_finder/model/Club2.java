package com.github.jkaste03.seeding_prob_finder.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Represents all football clubs with an automatic id (using enum ordinal),
 * display name,
 * ranking and seeding statistics. This enum replaces separate Club2 and
 * repository classes.
 */
public enum Club2 {
    REAL_MADRID("Real Madrid", 96.5f),
    BARCELONA("FC Barcelona", 95.2f),
    BAYERN_MUNICH("Bayern Munich", 94.7f),
    JUVENTUS("Juventus", 92.3f),
    LIVERPOOL("Liverpool", 93.1f);
    // … legg til flere klubber etter behov …

    private final String name;
    private final float ranking;

    // mutable counters per klubb
    private final AtomicInteger timesSeeded = new AtomicInteger(0);
    private final AtomicInteger timesUnseeded = new AtomicInteger(0);

    private static final Map<Integer, Club2> BY_ID = Arrays.stream(values())
            .collect(Collectors.toMap(Club2::getId, c -> c));

    Club2(String name, float ranking) {
        this.name = name;
        this.ranking = ranking;
    }

    /**
     * Den unike iden for hver enum-verdi, basert på ordinal.
     */
    public int getId() {
        return this.ordinal();
    }

    public String getName() {
        return name;
    }

    /**
     * Ranking uavhengig av konkurransenivå.
     */
    public float getRanking(int callerTournament) {
        return ranking;
    }

    public int getTimesSeeded() {
        return timesSeeded.get();
    }

    public int getTimesUnseeded() {
        return timesUnseeded.get();
    }

    /**
     * Øk seeding-teller.
     * 
     * @param isSeeded true om seeded, false om unseeded
     */
    public void incrementSeedingCounter(boolean isSeeded) {
        if (isSeeded) {
            timesSeeded.incrementAndGet();
        } else {
            timesUnseeded.incrementAndGet();
        }
    }

    // ----- statiske hjelpe-metoder -----

    /**
     * Hent klubb etter id (ordinal).
     */
    public static Club2 fromId(int id) {
        return BY_ID.get(id);
    }

    /**
     * Alle klubber som liste.
     */
    public static List<Club2> getAllClubs() {
        return Arrays.asList(values());
    }

    @Override
    public String toString() {
        return String.format("Club2{id=%d, name='%s', ranking=%.1f, seeded=%d, unseeded=%d}",
                getId(), name, ranking, getTimesSeeded(), getTimesUnseeded());
    }
}
