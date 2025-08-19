package com.github.jkaste03.seeding_prob_finder.model;

import com.github.jkaste03.seeding_prob_finder.enums.Tournament;

/**
 * Class representing the league phase in the UEFA Conference League.
 * This class handles the league phase rounds where clubs compete in a league
 * format specific to the Conference League.
 */
public class UeclLeaguePhaseRound extends LeaguePhaseRound {
    private final static int POT_COUNT = 6;

    /**
     * Constructs a ConferenceLeaguePhaseRound with the specified tournament.
     *
     * @param tournament the tournament for which this league phase round is
     *                   initialized.
     */
    public UeclLeaguePhaseRound() {
        super(Tournament.CONFERENCE_LEAGUE);
    }

    /**
     * Seeds the club slots into pots for the league phase.
     * <p>
     * This method ensures that the number of club slots is divisible by the
     * constant POT_COUNT.
     * If the club slots are null or their size is not divisible by POT_COUNT, an
     * IllegalStateException is thrown.
     * The club slots are then sorted based on their ranking and divided into pots.
     * Each pot is printed to the console.
     * </p>
     *
     * @throws IllegalStateException if the number of club slots is null or not
     *                               divisible by POT_COUNT.
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
            printClubSlotList(pots.get(i).getClubs());
        }
    }

    @Override
    protected void draw() {

    }
}