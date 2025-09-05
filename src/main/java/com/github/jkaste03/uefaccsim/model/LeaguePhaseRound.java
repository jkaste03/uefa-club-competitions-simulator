package com.github.jkaste03.uefaccsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.service.ClubEloDataLoader;

/**
 * Class representing a league phase in the UEFA competitions.
 * This class handles the league phase rounds where clubs compete in a league
 * format.
 */
public abstract class LeaguePhaseRound extends Round {
    protected final List<Pot> pots = new ArrayList<>();

    /**
     * Immutable container (Java record) representing a seeding pot in a league
     * phase round. A pot groups several ClubSlot entries that will later be drawn.
     * The pot index is the tier of the pot.
     * 
     * @param index zero-based pot index (must be >= 0)
     * @param clubs list of ClubSlot instances in this pot
     */
    public static record Pot(int index, List<ClubSlot> clubs) {
        @Override
        public String toString() {
            return "Pot " + (index + 1) + " " + clubs;
        }

        public String toCompactString() {
            return "Pot " + (index + 1) + " "
                    + clubs.stream().map(ClubSlot::toCompactString).collect(java.util.stream.Collectors.joining(", "));
        }
    }

    /**
     * Constructs a LeaguePhaseRound for the specified tournament.
     *
     * @param tournament the tournament for which this league phase round is
     *                   initialized.
     */
    public LeaguePhaseRound(Tournament tournament) {
        super(tournament, RoundType.LEAGUE_PHASE);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a string representation of the qualifying round, including the
     * tournament and round type.
     */
    @Override
    public String getName() {
        return super.getName() + " " + RoundType.LEAGUE_PHASE;
    }

    public List<Pot> getPots() {
        return pots;
    }

    /**
     * Returns the number of pots used in the league phase round.
     * <p>
     * This method should be implemented by subclasses.
     *
     * @return the number of pots
     */
    protected abstract int getPotCount();

    /**
     * Adds a new pot to the list of pots at the specified index with the given list
     * of club slots.
     *
     * @param index the index (tier, 0-based) of the pot to be added
     * @param clubs the list of {@link ClubSlot} objects to include in the pot
     */
    protected void addPot(int index, List<ClubSlot> clubs) {
        pots.add(new Pot(index, clubs));
    }

    /**
     * Seeds the club slots into pots for the league phase.
     * 
     * <p>
     * This method performs the following steps:
     * </p>
     * <ol>
     * <li>Ensures the number of club slots is divisible by {@code getPotCount()}.
     * If not, throws an {@link IllegalStateException}.</li>
     * <li>Sorts the club slots.</li>
     * <li>Divides the club slots into pots for the league phase and prints each
     * pot.</li>
     * </ol>
     * 
     * @throws IllegalStateException if the number of club slots is not divisible
     *                               by {@code getPotCount()}.
     */
    @Override
    protected void seed() {
        // Ensure the number of clubSlots is divisible by getPotCount().
        if (clubSlots == null || clubSlots.size() % getPotCount() != 0) {
            throw new IllegalStateException(
                    "ClubSlot count must be divisible by " + getPotCount() + " to seed properly.");
        }

        sortClubSlots();

        int potSize = clubSlots.size() / getPotCount();

        // Divide the club slots into pots for the league phase.
        for (int i = 0; i < getPotCount(); i++) {
            addPot(i, new ArrayList<>(clubSlots.subList(i * potSize, (i + 1) * potSize)));
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
                    .filter(c -> c.getClubIdWrapper().id() == ClubRepository.getLastUclWinnerId())
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

    /**
     * Draws the league phase round.
     */
    @Override
    protected abstract void draw();

    /**
     * Plays the league phase round.
     * This method is currently not implemented and will throw an
     * UnsupportedOperationException.
     *
     * @param clubEloDataLoader the data loader for club Elo ratings.
     */
    // TODO: Consider moving all play methods to Round
    public void play(ClubEloDataLoader clubEloDataLoader) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'play'");
        // Todo: Update the clubEloDataLoader with the new Elo ratings after the matches
    }

    @Override
    public String toString() {
        return "LeaguePhaseRound [name=" + getName() + ", pots=" + pots + "]";
    }
}