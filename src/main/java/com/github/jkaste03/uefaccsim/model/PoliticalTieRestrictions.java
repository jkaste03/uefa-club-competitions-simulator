package com.github.jkaste03.uefaccsim.model;

import java.util.*;

import com.github.jkaste03.uefaccsim.enums.Country;

/**
 * Class containing all the illegal matchups based on political restrictions
 * decided by the UEFA Executive Committee.
 */
public final class PoliticalTieRestrictions {
    /**
     * Adjacency mapping: for each country, which other countries it is forbidden to
     * face. Uses EnumMap/EnumSet for minimal overhead and O(1) lookup.
     */
    private static final Map<Country, Set<Country>> ILLEGAL_MAP;

    /**
     * Static initializer block to populate the illegal matchups map.
     */
    static {
        EnumMap<Country, Set<Country>> m = new EnumMap<>(Country.class);
        addPair(m, Country.ARM, Country.AZE);
        addPair(m, Country.GIB, Country.ESP);
        addPair(m, Country.KOS, Country.BHZ);
        addPair(m, Country.KOS, Country.SRB);
        addPair(m, Country.UKR, Country.BLR);
        addPair(m, Country.UKR, Country.RUS);
        // Lock inner sets
        m.replaceAll((k, v) -> Collections.unmodifiableSet(v));
        ILLEGAL_MAP = Collections.unmodifiableMap(m);
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private PoliticalTieRestrictions() {
        throw new AssertionError("No instances");
    }

    /**
     * Adds a symmetric prohibited pairing (a,b) and (b,a) to the adjacency map.
     * Assumes single‑threaded static init and idempotent (re‑adding is harmless).
     * 
     * @param m adjacency map (Country -> forbidden opponents set)
     * @param a first country
     * @param b second country
     */
    private static void addPair(Map<Country, Set<Country>> m, Country a, Country b) {
        m.computeIfAbsent(a, k -> EnumSet.noneOf(Country.class)).add(b);
        m.computeIfAbsent(b, k -> EnumSet.noneOf(Country.class)).add(a);
    }

    /**
     * Determines whether a pairing between two ClubSlot instances is prohibited. A
     * pairing is considered prohibited if any Country from one slot is mapped (via
     * ILLEGAL_MAP) as incompatible with any Country in the other slot.
     *
     * @param clubSlot1 the first club slot
     * @param clubSlot2 the second club slot
     * @return true if the pairing violates political restrictions, false otherwise
     */
    public static boolean isProhibited(ClubSlot clubSlot1, ClubSlot clubSlot2) {
        // Choose the smaller collection as outer loop for fewer lookups
        Collection<Country> c1s = clubSlot1.getCountries();
        Collection<Country> c2s = clubSlot2.getCountries();
        Collection<Country> outer = c1s.size() <= c2s.size() ? c1s : c2s;
        Collection<Country> inner = outer == c1s ? c2s : c1s;
        for (Country cOuter : outer) {
            Set<Country> banned = ILLEGAL_MAP.get(cOuter);
            if (banned == null)
                continue;
            for (Country cInner : inner) {
                if (banned.contains(cInner))
                    return true;
            }
        }
        return false;
    }
}
