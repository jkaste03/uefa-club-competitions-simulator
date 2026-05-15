package com.github.jkaste03.uefaccsim.model.competition;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.jkaste03.uefaccsim.enums.Country;
import com.github.jkaste03.uefaccsim.enums.Tournament;

/**
 * Represents a slot in a tournament bracket, which can either be a specific
 * club or a knockout tie (pairing) between two clubs.
 * <p>
 * A {@code ClubSlot} is a flexible abstraction.
 */
public class ClubSlot implements Serializable {

    // This either represents a knockout tie between two clubs or a single club;
    // whichever is not used is null.
    private KnockoutTie tie;
    private ClubSimState clubSimState;

    /**
     * Creates a ClubSlot representing a specific club.
     *
     * @param clubSimState the ClubSimState representing the club
     */
    public ClubSlot(ClubSimState clubSimState) {
        this.clubSimState = clubSimState;
    }

    /**
     * Constructs a ClubSlot with the specified KnockoutTie.
     *
     * @param tie the KnockoutTie associated with this ClubSlot
     */
    public ClubSlot(KnockoutTie tie) {
        this.tie = tie;
    }

    public boolean isTie() {
        return tie != null;
    }

    public boolean isClub() {
        return clubSimState != null;
    }

    public KnockoutTie getTie() {
        return tie;
    }

    public ClubSimState getClubSimState() {
        return clubSimState;
    }

    /**
     * Retrieves the ranking of this ClubSlot in the context of the given
     * tournament.
     *
     * @param callerTournament the tournament context
     * @return the ranking of this ClubSlot in the context of the given tournament
     */
    public float getRanking(Tournament callerTournament) {
        return isTie() ? tie.getRanking(callerTournament) : clubSimState.getClub().getRanking();
    }

    /**
     * Returns the list of countries represented by this ClubSlot.
     * If this slot represents a club, the list contains only that club's country.
     * If this slot is a tie, it aggregates (recursively) the countries of both
     * underlying club slots.
     *
     * @return list of countries for this ClubSlot.
     */
    public List<Country> getCountries() {
        if (isClub()) {
            return List.of(clubSimState.getClub().getCountry());
        } else {
            return Stream.of(tie.getClubSlotA(), tie.getClubSlotB())
                    .flatMap(cs -> cs.getCountries().stream())
                    .collect(Collectors.toList());
        }
    }

    /**
     * Resolves the current slot to a representation of a specific club based on the
     * outcome of the associated tie.
     * <p>
     * If this slot already represents a club, the method returns immediately.
     * Otherwise, it determines the winner of the tie and, depending on the
     * tournament hierarchy, assigns the appropriate {@code ClubSimState} to this
     * slot. If the winner is not yet decided, the method returns without making
     * changes.
     *
     * @param tournament the tournament context in which the slot is being
     *                   resolved
     */
    public void resolveSlot(Tournament tournament) {
        if (isClub())
            return;
        Boolean clubAWon = tie.isClubAWinner();
        if (clubAWon == null)
            return; // Winner not decided yet
        boolean higher = tie.getTournament().compareTo(tournament) > 0; // innerTie on higher level => take loser
        ClubSlot chosen = (clubAWon ^ higher) ? tie.getClubSlotA() : tie.getClubSlotB();
        this.clubSimState = chosen.getClubSimState();
        this.tie = null;
    }

    /**
     * Returns a compact "ClubA vs ClubB" string for ties, or the club name if not a
     * tie.
     *
     * @return compact string representation of this ClubSlot
     */
    public String toCompactString() {
        return isTie() ? tie.toCompactString() : clubSimState.getClub().getName();
    }

    @Override
    public String toString() {
        return "ClubSlot [" + (isTie() ? tie.toString() : clubSimState.toString()) + "]";
    }
}