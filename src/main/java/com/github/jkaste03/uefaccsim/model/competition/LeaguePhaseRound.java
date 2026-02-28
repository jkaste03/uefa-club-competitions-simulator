package com.github.jkaste03.uefaccsim.model.competition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.repository.ClubRepository;

/**
 * Class representing a league phase in the UEFA competitions.
 * This class handles the league phase rounds where clubs compete in a league
 * format.
 */
public abstract class LeaguePhaseRound extends Round {
    protected List<NonKnockoutTie> ties = new ArrayList<>();
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
     * Seeds, draws and schedules the ties.
     */
    public void seedDrawSchedule() {
        seed();
        draw();
        schedule();
        // ties.forEach(t -> System.out.println(t.toCompactString()));
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

    @Override
    public String toString() {
        return "LeaguePhaseRound [name=" + getName() + ", ties=" + ties + ", pots=" + pots + "]";
    }

    /**
     * Schedules non-knockout ties across multiple match days (MD) to ensure
     * balanced distribution.
     * 
     * This method organizes ties into match days such that:
     * <ul>
     * <li>Each club participates in the same number of matches across all match
     * days</li>
     * <li>No club plays more than once per match day</li>
     * <li>All ties are distributed evenly across match days</li>
     * </ul>
     * 
     * The scheduling algorithm uses a randomized greedy approach with restart
     * logic:
     * <ol>
     * <li>Validates that all clubs have equal match counts and that ties can be
     * evenly distributed</li>
     * <li>For each match day, attempts to select ties greedily while avoiding club
     * conflicts</li>
     * <li>Shuffles remaining ties to introduce randomness and increase success
     * rate</li>
     * <li>Restarts the entire process if a match day cannot be filled within the
     * attempt limit</li>
     * <li>Continues until a valid schedule is generated or maximum restarts are
     * exceeded</li>
     * </ol>
     * 
     * The resulting scheduled ties are stored in place of the original unscheduled
     * ties.
     * 
     * @throws IllegalStateException if clubs have unequal match counts, total ties
     *                               cannot be
     *                               evenly divided by matches per club, or a valid
     *                               schedule
     *                               cannot be generated within the restart limit
     */
    private void schedule() {
        int matchesPerClub = validateAndGetMatchesPerClub();
        if (matchesPerClub <= 0)
            return;

        ScheduleConfig config = new ScheduleConfig(matchesPerClub, ties.size());
        List<NonKnockoutTie> original = new ArrayList<>(ties);
        List<List<NonKnockoutTie>> mdGroups = buildSchedule(original, config);

        applySchedule(mdGroups);
    }

    /**
     * Validates that all clubs have equal match counts and returns the match count
     * per club.
     * 
     * @return the number of matches per club
     * @throws IllegalStateException if clubs have unequal match counts
     */
    private int validateAndGetMatchesPerClub() {
        Map<Integer, Integer> occ = new HashMap<>();
        for (NonKnockoutTie t : ties) {
            int a = t.getClubSlotA().getClubSimState().getId();
            int b = t.getClubSlotB().getClubSimState().getId();
            occ.put(a, occ.getOrDefault(a, 0) + 1);
            occ.put(b, occ.getOrDefault(b, 0) + 1);
        }

        int matchesPerClub = -1;
        for (Map.Entry<Integer, Integer> e : occ.entrySet()) {
            if (matchesPerClub == -1) {
                matchesPerClub = e.getValue();
            } else if (matchesPerClub != e.getValue()) {
                throw new IllegalStateException("Unequal number of matches per club: " + e.getKey());
            }
        }
        return matchesPerClub;
    }

    /**
     * Builds a valid schedule by attempting multiple times with restarts.
     * 
     * @param original the original list of ties
     * @param config   the schedule configuration
     * @return a list of match day groups
     * @throws IllegalStateException if a valid schedule cannot be generated
     */
    private List<List<NonKnockoutTie>> buildSchedule(List<NonKnockoutTie> original, ScheduleConfig config) {
        final int MAX_RESTARTS = 200;
        int restarts = 0;

        while (restarts < MAX_RESTARTS) {
            List<List<NonKnockoutTie>> mdGroups = attemptScheduleGeneration(original, config);
            if (mdGroups != null && mdGroups.size() == config.mdCount() &&
                    mdGroups.stream().mapToInt(List::size).sum() == config.totalTies()) {
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
     * @param config   the schedule configuration
     * @return a list of match day groups, or null if generation failed
     */
    private List<List<NonKnockoutTie>> attemptScheduleGeneration(List<NonKnockoutTie> original, ScheduleConfig config) {
        List<NonKnockoutTie> remaining = new ArrayList<>(original);
        List<List<NonKnockoutTie>> mdGroups = new ArrayList<>(config.mdCount());
        final int MAX_ATTEMPTS_PER_MD = 500;

        for (int md = 0; md < config.mdCount(); md++) {
            List<NonKnockoutTie> group = tryFillMatchDay(remaining, config.tiesPerMd(), MAX_ATTEMPTS_PER_MD);
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
     * Configuration record for match day scheduling.
     */
    private static record ScheduleConfig(int mdCount, int totalTies) {
        ScheduleConfig {
            if (totalTies % mdCount != 0) {
                throw new IllegalStateException(
                        "Totalt antall ties (" + totalTies + ") må være delelig med matchesPerClub (" + mdCount + ")");
            }
        }

        int tiesPerMd() {
            return totalTies / mdCount;
        }
    }
}