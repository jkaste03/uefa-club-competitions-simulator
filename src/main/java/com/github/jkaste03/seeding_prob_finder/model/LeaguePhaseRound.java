package com.github.jkaste03.seeding_prob_finder.model;

import java.util.ArrayList;
import java.util.List;
import com.github.jkaste03.seeding_prob_finder.enums.Tournament;
import com.github.jkaste03.seeding_prob_finder.enums.RoundType;
import com.github.jkaste03.seeding_prob_finder.service.ClubEloDataLoader;

/**
 * Class representing a league phase in the UEFA competitions.
 * This class handles the league phase rounds where clubs compete in a league
 * format.
 */
public abstract class LeaguePhaseRound extends Round {
    protected List<SingleLeggedTie> ties;
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
     * Helper for subclasses when constructing pots
     */
    protected void addPot(int index, List<ClubSlot> clubs) {
        pots.add(new Pot(index, clubs));
    }

    protected void clearPots() {
        pots.clear();
    }

    /**
     * Seeds the league phase round.
     */
    @Override
    protected abstract void seed();

    @Override
    protected abstract void draw();

    /**
     * Plays the league phase round.
     * This method is currently not implemented and will throw an
     * UnsupportedOperationException.
     *
     * @param clubEloDataLoader the data loader for club Elo ratings.
     */
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