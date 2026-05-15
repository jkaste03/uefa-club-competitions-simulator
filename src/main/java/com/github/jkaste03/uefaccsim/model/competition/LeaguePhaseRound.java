package com.github.jkaste03.uefaccsim.model.competition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.repository.ClubRepository;
import com.github.jkaste03.uefaccsim.repository.ClubSimStateRepository;

/**
 * Class representing a league phase in the UEFA competitions.
 * This class handles the league phase rounds where clubs compete in a league
 * format.
 */
public abstract class LeaguePhaseRound extends Round {

    protected List<NonKnockoutTie> ties = new ArrayList<>();
    protected final List<Pot> pots = new ArrayList<>();
    private int playedMatchDays = 0;
    private int remainingUnplayedTies = 0;
    private LeagueTable leagueTable;

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
    protected LeaguePhaseRound(Tournament tournament) {
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

    @Override
    public List<NonKnockoutTie> getTies() {
        return ties;
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
     * Returns how many league-phase matches each club plays.
     *
     * @return matches per club
     */
    protected abstract int getMatchesPerClub();

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
     * Seeds, draws and schedules the ties. The method also initializes the league
     * table.
     */
    public void seedDrawSchedule() {
        seed();
        draw();
        schedule();
        // Also, initialize the league table.
        initTable();
    }

    /**
     * Initializes the league table and records the opponent mapping from the drawn
     * ties.
     */
    private void initTable() {
        this.leagueTable = new LeagueTable(clubSlots);
        this.leagueTable.recordOpponents(ties, getMatchesPerClub());
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
                    .filter(c -> c.getClubSimState().getId() == ClubRepository.getLastUclWinnerId())
                    .findFirst()
                    .ifPresent(c -> {
                        Collections.swap(clubSlots, 0, clubSlots.indexOf(c));
                        isUclWinnerHere[0] = true;
                    });
        }

        // Sort the club slots based on their ranking. Leave the UCL winner at the top
        // if present.
        clubSlots.subList(isUclWinnerHere[0] ? 1 : 0, clubSlots.size())
                .sort((cA, cB) -> Float.compare(cA.getRanking(tournament), cB.getRanking(tournament)));
    }

    /**
     * Builds a valid matchday schedule for all drawn ties and applies it to
     * {@code ties}.
     * <p>
     * The schedule guarantees that each tie is assigned once and that clubs do not
     * play more than one tie per matchday.
     *
     * @throws IllegalStateException if no valid schedule can be generated
     */

    private void schedule() {
        List<NonKnockoutTie> original = new ArrayList<>(ties);
        List<List<NonKnockoutTie>> mdGroups = buildSchedule(original);

        applySchedule(mdGroups);
    }

    /**
     * Builds a valid schedule by attempting multiple times with restarts.
     * 
     * @param original the original list of ties
     * @return a list of match day groups
     * @throws IllegalStateException if a valid schedule cannot be generated
     */
    private List<List<NonKnockoutTie>> buildSchedule(List<NonKnockoutTie> original) {
        final int MAX_RESTARTS = 200;
        int restarts = 0;

        while (restarts < MAX_RESTARTS) {
            List<List<NonKnockoutTie>> mdGroups = attemptScheduleGeneration(original);
            if (mdGroups != null && mdGroups.size() == getMatchesPerClub() &&
                    mdGroups.stream().mapToInt(List::size).sum() == ties.size()) {
                return mdGroups;
            }
            restarts++;
        }

        throw new IllegalStateException("Could not generate schedule after " + restarts + " restarts.");
    }

    /**
     * Attempts to generate a schedule for all match days.
     * 
     * @param original the original list of ties
     * @return a list of match day groups, or null if generation failed
     */
    private List<List<NonKnockoutTie>> attemptScheduleGeneration(List<NonKnockoutTie> original) {
        List<NonKnockoutTie> remaining = new ArrayList<>(original);
        List<List<NonKnockoutTie>> mdGroups = new ArrayList<>(getMatchesPerClub());
        final int MAX_ATTEMPTS_PER_MD = 500;

        for (int md = 0; md < getMatchesPerClub(); md++) {
            List<NonKnockoutTie> group = tryFillMatchDay(remaining, ties.size() / getMatchesPerClub(),
                    MAX_ATTEMPTS_PER_MD);
            if (group == null) {
                return null;
            }
            remaining.removeAll(group);
            mdGroups.add(group);
        }

        return mdGroups;
    }

    /**
     * Attempts to fill a single match day with ties.
     * 
     * @param remaining   the remaining unscheduled ties
     * @param tiesPerMd   the number of ties needed for this match day
     * @param maxAttempts the maximum number of attempts
     * @return a filled match day group, or null if unsuccessful
     */
    private List<NonKnockoutTie> tryFillMatchDay(List<NonKnockoutTie> remaining, int tiesPerMd, int maxAttempts) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Collections.shuffle(remaining, ThreadLocalRandom.current());
            List<NonKnockoutTie> group = selectTiesForMatchDay(remaining, tiesPerMd);
            if (group.size() == tiesPerMd) {
                return group;
            }
        }
        return null;
    }

    /**
     * Selects ties for a match day such that no club plays twice.
     * 
     * @param remaining the remaining unscheduled ties
     * @param tiesPerMd the number of ties to select
     * @return a list of selected ties
     */
    private List<NonKnockoutTie> selectTiesForMatchDay(List<NonKnockoutTie> remaining, int tiesPerMd) {
        Set<Integer> used = new HashSet<>(tiesPerMd * 2);
        List<NonKnockoutTie> group = new ArrayList<>(tiesPerMd);

        for (NonKnockoutTie t : remaining) {
            if (group.size() >= tiesPerMd) {
                break;
            }
            int a = t.getClubSlotA().getClubSimState().getId();
            int b = t.getClubSlotB().getClubSimState().getId();
            if (!used.contains(a) && !used.contains(b)) {
                group.add(t);
                used.add(a);
                used.add(b);
            }
        }

        return group;
    }

    /**
     * Applies the generated schedule to the ties list.
     * 
     * @param mdGroups the list of match day groups
     */
    private void applySchedule(List<List<NonKnockoutTie>> mdGroups) {
        List<NonKnockoutTie> scheduled = new ArrayList<>();
        for (List<NonKnockoutTie> g : mdGroups) {
            Collections.shuffle(g, ThreadLocalRandom.current());
            scheduled.addAll(g);
        }
        this.ties.clear();
        this.ties.addAll(scheduled);
    }

    /**
     * Plays the next league-phase matchday for this round.
     * <p>
     * On each invocation, this method simulates only the ties belonging to the
     * next unplayed matchday (not the full league phase at once). It keeps internal
     * progress using {@code playedMatchDays} and {@code remainingUnplayedTies} so
     * repeated calls advance the round one matchday at a time.
     * <p>
     * The number of ties played in the current call is chosen to keep the remaining
     * ties distributable across the remaining matchdays.
     * <p>
     * After simulating each tie for the current matchday, the league table is
     * updated with the results.
     *
     * @param clubSimStateRepo repository used to persist and update club simulation
     *                         state while each tie is played
     * @throws IllegalStateException if there are no matchdays left to play, no
     *                               unplayed ties remain, or the expected number of
     *                               ties for the current matchday could not be
     *                               played
     */
    public void play(ClubSimStateRepository clubSimStateRepo) {
        if (playedMatchDays == 0) {
            this.remainingUnplayedTies = countUnplayedTies();
        }

        int remainingMatchDays = getMatchesPerClub() - playedMatchDays;
        if (remainingMatchDays <= 0) {
            throw new IllegalStateException(
                    "No remaining match days, but play() was called again for " + getName() + ".");
        }

        if (remainingUnplayedTies <= 0) {
            throw new IllegalStateException(
                    "No unplayed ties remain, but play() was called again for " + getName() + ".");
        }

        int tiesToPlayNow = determineTiesToPlayThisMatchDay(
                remainingMatchDays,
                ties.size() / getMatchesPerClub());

        int playedNow = 0;
        for (NonKnockoutTie tie : ties) {
            if (playedNow >= tiesToPlayNow) {
                break;
            }

            if (isTiePlayed(tie)) {
                continue;
            }

            tie.play(clubSimStateRepo);
            // After playing the tie, we can update the league table with the result.
            leagueTable.registerMatch(tie);
            // System.out
            // .println(tie.getClubAGoals1stLeg() + " " + tie.toCompactString() + " " +
            // tie.getClubBGoals1stLeg());
            playedNow++;
        }

        if (playedNow != tiesToPlayNow) {
            throw new IllegalStateException(
                    "Could not play expected number of ties for next match day in " + getName()
                            + ": expected=" + tiesToPlayNow + ", played=" + playedNow + ".");
        }
        remainingUnplayedTies -= playedNow;
        playedMatchDays++;
    }

    private int countUnplayedTies() {
        int unplayed = 0;
        for (NonKnockoutTie tie : ties) {
            if (!isTiePlayed(tie)) {
                unplayed++;
            }
        }
        return unplayed;
    }

    private static boolean isTiePlayed(NonKnockoutTie tie) {
        return tie.getClubAGoals1stLeg() != null && tie.getClubBGoals1stLeg() != null;
    }

    private int determineTiesToPlayThisMatchDay(int remainingMatchDays, int preferredTiesPerDay) {
        if (remainingMatchDays <= 1) {
            return remainingUnplayedTies;
        }

        int best = -1;
        int bestDistance = Integer.MAX_VALUE;
        int futureDays = remainingMatchDays - 1;

        for (int candidate = 1; candidate <= remainingUnplayedTies; candidate++) {
            int remainingAfterToday = remainingUnplayedTies - candidate;
            if (remainingAfterToday % futureDays != 0) {
                continue;
            }

            int distance = Math.abs(candidate - preferredTiesPerDay);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        if (best != -1) {
            return best;
        }

        int maxForFuture = remainingUnplayedTies - futureDays;
        if (maxForFuture < 1) {
            return 1;
        }

        return Math.max(1, Math.min(preferredTiesPerDay, maxForFuture));
    }

    /**
     * Registers clubs for the next rounds.
     * <p>
     * This method first calculates the league standings by calling
     * {@link leagueTable#calcStandings()}. After the standings are calculated, it
     * determines which clubs qualify to advance to subsequent rounds and registers
     * them accordingly.
     */
    public void regClubsForNextRounds() {
        // First, calculate the league standings based on the results of the played
        // matches.
        leagueTable.calcStandings();
        Round nextRound = this.nextPrimaryRnd;
        Round nextNextRound = nextRound.nextPrimaryRnd;
        int i = 0;
        int playoffClubs = ((KnockoutRoundPlayoff) nextRound).getExpectedClubCount();
        // Then, determine which clubs qualify for the next next round and register
        // them.
        for (; i < playoffClubs / 2; i++) {
            int clubIdx = leagueTable.getIdxByStanding(i);
            nextNextRound.addClubSlot(clubSlots.get(clubIdx));
        }
        // Finally, determine which clubs qualify for the next round and register them.
        for (; i < playoffClubs * 1.5; i++) {
            int clubIdx = leagueTable.getIdxByStanding(i);
            nextRound.addClubSlot(clubSlots.get(clubIdx));
        }
    }

    @Override
    public String toString() {
        return "LeaguePhaseRound [name=" + getName() + ", ties=" + ties + ", pots=" + pots + ", playedMatchDays="
                + playedMatchDays + ", remainingUnplayedTies=" + remainingUnplayedTies + ", leagueTable=" + leagueTable
                + "]";
    }
}