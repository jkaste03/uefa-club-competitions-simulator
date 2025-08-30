package com.github.jkaste03.uefaccsim.model;

import java.util.List;

import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.service.ClubEloDataLoader;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Abstract class representing a round in the UEFA competitions.
 */
public abstract class Round implements Serializable {
    protected final Tournament tournament;
    protected final RoundType roundType;
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
    public Round(Tournament tournament, RoundType roundType) {
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

    public Round getNextPrimaryRnd() {
        return nextPrimaryRnd;
    }

    public Round getNextSecondaryRnd() {
        return nextSecondaryRnd;
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
    public void addClubSlot(ClubSlot clubSlot) {
        clubSlots.add(clubSlot);
    }

    /**
     * Determines whether pairing the two specified club slots is prohibited in this
     * round.
     * <p>
     * For qualification (QRound) and league phase (LeaguePhaseRound) rounds, a tie
     * is illegal if:
     * <ul>
     * <li>The clubs share the same country (hasCommonCountry returns true), or</li>
     * <li>A political restriction applies (PoliticalTieRestrictions.isProhibited
     * returns true).</li>
     * </ul>
     * For all other round types, only political restrictions are considered.
     *
     * @param clubSlot1 the first club slot
     * @param clubSlot2 the second club slot
     * @return true if the pairing is not allowed; false otherwise
     */
    public boolean isIllegalTie(ClubSlot clubSlot1, ClubSlot clubSlot2) {
        if (this instanceof QRound || this instanceof LeaguePhaseRound) {
            return hasCommonCountry(clubSlot1, clubSlot2)
                    || PoliticalTieRestrictions.isProhibited(clubSlot1, clubSlot2);
        }
        return PoliticalTieRestrictions.isProhibited(clubSlot1, clubSlot2);
    }

    /**
     * Checks if two club slots share at least one common country.
     * 
     * @param clubSlot1 the first club slot.
     * @param clubSlot2 the second club slot.
     * @return true if they share at least one common country, false otherwise.
     */
    private static boolean hasCommonCountry(ClubSlot clubSlot1, ClubSlot clubSlot2) {
        return clubSlot1.getCountries().stream().anyMatch(clubSlot2.getCountries()::contains);
    }

    /**
     * Prints the names of the clubs in the provided list of ClubSlot objects.
     *
     * @param clubSlotList the list of ClubSlot objects whose names are to be
     *                     printed
     */
    protected static void printClubSlotList(List<ClubSlot> clubSlotList) {
        clubSlotList.forEach(clubSlot -> System.out.println(clubSlot.toCompactString()));
    }

    /**
     * Seeds and draws the ties.
     */
    public void seedDraw() {
        seed();
        draw();
    }

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
     * Plays the round. This method is responsible for playing the round.
     */
    public abstract void play(ClubEloDataLoader clubEloDataLoader);

    /**
     * Attempts to resolve every {@link ClubSlot} in this round to a concrete club
     * (if possible).
     *
     * <p>
     * For each slot:
     * </p>
     * <ul>
     * <li>If it already represents a concrete club, nothing happens.</li>
     * <li>If it depends on the outcome of a tie (e.g. a {@code DoubleLeggedTie}),
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