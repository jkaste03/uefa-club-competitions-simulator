package com.github.jkaste03.uefaccsim.model.competition;

import java.util.List;

import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.model.rule.PoliticalTieRestrictions;
import com.github.jkaste03.uefaccsim.reporting.StatsAggregator;
import com.github.jkaste03.uefaccsim.reporting.StatsAggregator.RoundKey;
import com.github.jkaste03.uefaccsim.repository.ClubSimStateRepository;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Abstract class representing a round in the UEFA competitions.
 */
public abstract class Round implements Serializable {
    protected final Tournament tournament;
    protected final RoundType roundType;
    protected transient StatsAggregator statsAggregator;
    // References to the next rounds
    protected Round nextPrimaryRnd;
    protected Round nextSecondaryRnd;

    protected final List<ClubSlot> clubSlots = new ArrayList<>();

    /**
     * Constructor that initializes the round with a tournament and round type.
     * 
     * @param tournament the tournament of the round.
     * @param roundType  the type of the round.
     */
    protected Round(Tournament tournament, RoundType roundType) {
        this.tournament = tournament;
        this.roundType = roundType;
    }

    /**
     * Returns the name of the round. Is overridden by subclasses.
     * 
     * @return the name of the round.
     */
    public String getName() {
        return tournament + "";
    }

    public Tournament getTournament() {
        return tournament;
    }

    public RoundType getRoundType() {
        return roundType;
    }

    /**
     * Attaches a stats aggregator used to record statistics for this round.
     * 
     * @param statsAggregator the stats aggregator to attach.
     */
    public void attachStatsAggregator(StatsAggregator statsAggregator) {
        this.statsAggregator = statsAggregator;
    }

    public void setNextRounds(Round nextPrimaryRnd, Round nextSecondaryRnd) {
        this.nextPrimaryRnd = nextPrimaryRnd;
        this.nextSecondaryRnd = nextSecondaryRnd;
    }

    public void setNextRound(Round nextPrimaryRnd) {
        this.nextPrimaryRnd = nextPrimaryRnd;
    }

    public List<ClubSlot> getClubSlots() {
        return clubSlots;
    }

    /**
     * Adds a club slot to the round.
     * 
     * @param clubSlot the club slot to add.
     */
    public final void addClubSlot(ClubSlot clubSlot) {
        clubSlots.add(clubSlot);
    }

    /**
     * Determines whether pairing the two specified club slots is prohibited in this
     * round.
     * <p>
     * A tie is illegal if:
     * <ul>
     * <li>The clubs share the same country (hasCommonCountry returns true), or</li>
     * <li>A political restriction applies (PoliticalTieRestrictions.isProhibited
     * returns true).</li>
     * </ul>
     *
     * @param clubSlotA the first club slot
     * @param clubSlotB the second club slot
     * @return true if the pairing is not allowed; false otherwise
     */
    public boolean isIllegalTie(ClubSlot clubSlotA, ClubSlot clubSlotB) {
        return hasCommonCountry(clubSlotA, clubSlotB)
                || PoliticalTieRestrictions.isProhibited(clubSlotA, clubSlotB);
    }

    /**
     * Checks if two club slots share at least one common country.
     * 
     * @param clubSlotA the first club slot.
     * @param clubSlotB the second club slot.
     * @return true if they share at least one common country, false otherwise.
     */
    private static boolean hasCommonCountry(ClubSlot clubSlotA, ClubSlot clubSlotB) {
        return clubSlotA.getCountries().stream().anyMatch(clubSlotB.getCountries()::contains);
    }

    /**
     * Prints the names of the clubs in the round.
     */
    protected void printClubSlotList() {
        clubSlots.forEach(clubSlot -> System.out.println(clubSlot.toCompactString()));
    }

    /**
     * Seeds, draws and if relevant schedules the matches.
     */
    protected abstract void seedDrawSchedule();

    /**
     * Seeds the clubs for the round.
     * <p>
     * This method is responsible for seeding the clubs in the round. The specific
     * implementation may vary depending on the type of round (e.g., qualifying,
     * league phase).
     */
    protected abstract void seed();

    /**
     * Draws the ties for the round.
     * <p>
     * This method is responsible for drawing the ties for the round. The specific
     * implementation may vary depending on the type of round (e.g., qualifying,
     * league phase).
     */
    protected abstract void draw();

    /**
     * Retrieves the list of ties associated with this round. The specific type of
     * {@link Tie}s returned may vary depending on the implementation.
     *
     * @return a list of ties for this round
     */
    protected abstract List<? extends Tie> getTies();

    /**
     * Plays the round. This method is responsible for playing the round.
     */
    protected abstract void play(ClubSimStateRepository clubSimStateRepo);

    /**
     * Attempts to resolve every {@link ClubSlot} in this round to a concrete club
     * (if possible).
     *
     * <p>
     * For each slot:
     * </p>
     * <ul>
     * <li>If it already represents a concrete club, nothing happens.</li>
     * <li>If it depends on the outcome of a tie (e.g. a {@code KnockoutTie}),
     * the underlying tie is inspected:
     * <ul>
     * <li>If the tie has a decided winner, the slot is replaced by either the
     * winner or (when the originating
     * tie belongs to a higher tournament level) the loser, mirroring the logic in
     * {@link ClubSlot#resolveSlot(Tournament)}.</li>
     * <li>If the tie is still undecided, the slot remains unresolved.</li>
     * </ul>
     * </li>
     * </ul>
     *
     * <p>
     * The tournament argument passed to {@code ClubSlot#resolveSlot} is this
     * round's {@link Tournament}, allowing the
     * slot logic to determine whether it should take the winner or loser based on
     * tournament hierarchy.
     * </p>
     */
    public void resolveClubSlots() {
        for (ClubSlot clubSlot : clubSlots) {
            clubSlot.resolveSlot(tournament);
        }
    }

    /**
     * Records the round's matchup statistics via the attached aggregator.
     * <p>
     * Creates a round key from this round's tournament and round type, then
     * delegates to the aggregator to record all ties as matchups.
     * </p>
     *
     * @throws IllegalStateException if no stats aggregator has been attached
     */
    protected void recordMatchup() {
        RoundKey roundKey = getRoundKey();
        statsAggregator.recordMatchup(roundKey, getTies());
    }

    /**
     * Builds the statistics round key for this round.
     *
     * @return the round key for this round
     */
    protected RoundKey getRoundKey() {
        validateStatsAggregator();
        return new RoundKey(tournament, roundType, null);
    }

    /**
     * Validates that a stats aggregator has been attached to this round.
     *
     * @throws IllegalStateException if no stats aggregator has been attached
     */
    protected void validateStatsAggregator() {
        if (statsAggregator == null) {
            throw new IllegalStateException("StatsAggregator has not been attached to " + getName());
        }
    }

    @Override
    public String toString() {
        return "Round [name=" + getName() + ", " + fieldsToString() + "]";
    }

    /**
     * Returns a concise, comma-separated textual representation of this Round's key
     * fields. Used by all Round subclasses in their toString implementations.
     *
     * @return a human-readable summary of this Round's state
     */
    protected String fieldsToString() {
        return "nextPrimaryRnd=" + (nextPrimaryRnd != null ? nextPrimaryRnd.getName() : "null")
                + ", nextSecondaryRnd=" + (nextSecondaryRnd != null ? nextSecondaryRnd.getName() : "null")
                + ", clubSlots=" + clubSlots;
    }
}