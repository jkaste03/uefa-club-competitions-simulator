package com.github.jkaste03.seeding_prob_finder.model;

import java.util.ArrayList;
import java.util.List;
import com.github.jkaste03.seeding_prob_finder.enums.CompetitionData;
import com.github.jkaste03.seeding_prob_finder.service.ClubEloDataLoader;

/**
 * Class representing a league phase in the UEFA competitions.
 * This class handles the league phase rounds where clubs compete in a league
 * format.
 */
public abstract class LeaguePhaseRound extends Round {
    // Replaced raw nested list with typed Pot objects

    protected List<SingleLeggedTie> ties = new ArrayList<>();
    protected final List<Pot> pots;

    public static class Pot {
        private final int index;
        private final List<ClubSlot> clubs;

        public Pot(int index, List<ClubSlot> clubs) {
            this.index = index;
            this.clubs = clubs;
        }

        public int getIndex() {
            return index;
        }

        public List<ClubSlot> getClubs() {
            return clubs;
        }

        @Override
        public String toString() {
            return "Pot " + (index + 1) + " " + clubs;
        }
    }

    /**
     * Constructs a LeaguePhaseRound with the specified tournament.
     *
     * @param tournament the tournament for which this league phase round is
     *                   initialized.
     */
    public LeaguePhaseRound(CompetitionData.Tournament tournament) {
        super(tournament, CompetitionData.RoundType.LEAGUE_PHASE);
        pots = new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a string representation of the qualifying round, including the
     * tournament and round type.
     */
    @Override
    public String getName() {
        return super.getName() + " " + CompetitionData.RoundType.LEAGUE_PHASE;
    }

    public List<Pot> getPots() {
        return pots;
    }

    // Helper for subclasses when constructing pots
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