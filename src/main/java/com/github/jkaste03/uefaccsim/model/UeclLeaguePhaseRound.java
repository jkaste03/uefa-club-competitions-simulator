package com.github.jkaste03.uefaccsim.model;

import com.github.jkaste03.uefaccsim.enums.Tournament;

/**
 * Class representing the league phase in the UEFA Conference League.
 * This class handles the league phase rounds where clubs compete in a league
 * format specific to the Conference League.
 */
public class UeclLeaguePhaseRound extends LeaguePhaseRound {

    /**
     * The number of pots used for seeding clubs in the league phase.
     */
    private final static int POT_COUNT = 6;

    /**
     * Constructs a ConferenceLeaguePhaseRound.
     */
    public UeclLeaguePhaseRound() {
        super(Tournament.CONFERENCE_LEAGUE);
    }

    /**
     * Seeds the club slots into pots for the league phase.
     * 
     * <p>
     * This method performs the following steps:
     * </p>
     * <ol>
     * <li>Ensures the number of club slots is divisible by {@code POT_COUNT}. If
     * not, throws an {@link IllegalStateException}.</li>
     * <li>Sorts the club slots.</li>
     * <li>Divides the club slots into pots for the league phase and prints each
     * pot.</li>
     * </ol>
     * 
     * @throws IllegalStateException if the number of club slots is not divisible
     *                               by {@code POT_COUNT}.
     */
    @Override
    protected void seed() {
        // Ensure the number of clubSlots is divisible by POT_COUNT.
        if (clubSlots == null || clubSlots.size() % POT_COUNT != 0) {
            throw new IllegalStateException("ClubSlot count must be divisible by " + POT_COUNT + " to seed properly.");
        }

        // Sort the club slots based on their ranking.
        clubSlots.sort((c1, c2) -> Float.compare(c1.getRanking(tournament), c2.getRanking(tournament)));

        int potSize = clubSlots.size() / POT_COUNT;

        // Divide the club slots into pots for the league phase.
        for (int i = 0; i < POT_COUNT; i++) {
            // Defensive copy to decouple from backing list
            addPot(i, new java.util.ArrayList<>(clubSlots.subList(i * potSize, (i + 1) * potSize)));
            // printClubSlotList(pots.get(i).clubs());
        }
    }

    @Override
    protected void draw() {

    }

}