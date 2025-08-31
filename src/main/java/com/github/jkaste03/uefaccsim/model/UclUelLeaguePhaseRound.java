package com.github.jkaste03.uefaccsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.github.jkaste03.uefaccsim.enums.Country;
import com.github.jkaste03.uefaccsim.enums.Tournament;

/**
 * Class representing the league phase in the Champions League and Europa
 * League. This class handles the league phase rounds where clubs compete in a
 * league format specific to those competitions.
 */
public class UclUelLeaguePhaseRound extends LeaguePhaseRound {
    // Constant for clubs skipping a round (e.g., UCL Q3 LP to UEL LP)
    // private final static String ROUND_CLUBS_SKIP_TO = Tournament.EUROPA_LEAGUE +
    // " "
    // + RoundType.LEAGUE_PHASE;

    /**
     * The number of pots used for seeding clubs in the league phase.
     */
    private final static int POT_COUNT = 4;

    /**
     * Constructs a Champions/Europa LeaguePhaseRound.
     *
     * @param tournament the tournament for which this league phase round is
     *                   initialized.
     */
    public UclUelLeaguePhaseRound(Tournament tournament) {
        super(tournament);
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

        sortClubSlots();

        int potSize = clubSlots.size() / POT_COUNT;

        // Divide the club slots into pots for the league phase.
        for (int i = 0; i < POT_COUNT; i++) {
            addPot(i, new ArrayList<>(clubSlots.subList(i * potSize, (i + 1) * potSize)));
            // System.out.print(pots.get(i));
            // printClubSlotList(pots.get(i).clubs());
        }
    }

    /**
     * Sorts the club slots for the league phase round.
     * <p>
     * If the tournament is the Champions League, this method checks if the last UCL
     * winner is present in the club slots.
     * If the UCL winner is found, it is moved to the top of the list.
     * <p>
     * After handling the UCL winner, the remaining club slots are sorted based on
     * their ranking.
     * The UCL winner, if present, remains at the top of the list.
     */
    private void sortClubSlots() {
        final boolean[] isUclWinnerHere = { false }; // Array to hold the state of UCL winner presence. This is an array
                                                     // to allow modification inside the lambda below.
        // Check if the UCL winner is present in the club slots and move them to the top
        if (tournament == Tournament.CHAMPIONS_LEAGUE) {
            clubSlots.stream()
                    .filter(c -> c.toCompactString().equals(ClubRepository.getLastUclWinnerId()))
                    .findFirst()
                    .ifPresent(c -> {
                        Collections.swap(clubSlots, 0, clubSlots.indexOf(c));
                        isUclWinnerHere[0] = true;
                    });
        }

        // Sort the club slots based on their ranking. Leave the UCL winner at the top
        // if present.
        clubSlots.subList(isUclWinnerHere[0] ? 1 : 0, clubSlots.size())
                .sort((c1, c2) -> Float.compare(c1.getRanking(tournament), c2.getRanking(tournament)));
    }

    @Override
    protected void draw() {

    }
}