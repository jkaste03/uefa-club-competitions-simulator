package com.github.jkaste03.uefaccsim.model;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.jkaste03.uefaccsim.enums.Country;
import com.github.jkaste03.uefaccsim.enums.Tournament;

/**
 * Represents a slot in a tournament bracket, which can either be a specific
 * club or a tie (pairing) between two clubs.
 * <p>
 * A {@code ClubSlot} is a flexible abstraction.
 */
public class ClubSlot implements Serializable {

    // This either represents a tie between two clubs or a single club; whichever is
    // not used is null.
    private Tie tie;
    private ClubIdWrapper clubIdWrapper;

    /**
     * Constructs a single‑leg tie between the two given child slots. Order matters.
     * 
     * @param clubSlot1 home participant
     * @param clubSlot2 away participant
     */
    public ClubSlot(ClubSlot clubSlot1, ClubSlot clubSlot2) {
        this.tie = new SingleLeggedTie(clubSlot1, clubSlot2);
    }

    /**
     * Constructs a two‑legged tie for the given child slots in the specified
     * tournament. Order matters.
     * 
     * @param clubSlot1  home participant first leg
     * @param clubSlot2  away participant first leg
     * @param tournament tournament
     */
    public ClubSlot(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament) {
        this.tie = new DoubleLeggedTie(clubSlot1, clubSlot2, tournament);
    }

    /**
     * Constructs a two‑legged tie with preset goals (first leg). Order matters.
     * 
     * @param clubSlot1  home participant first leg
     * @param clubSlot2  away participant first leg
     * @param tournament tournament
     * @param club1Goals goals for club 1
     * @param club2Goals goals for club 2
     */
    public ClubSlot(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament, Integer club1Goals,
            Integer club2Goals) {
        this.tie = new DoubleLeggedTie(clubSlot1, clubSlot2, tournament, club1Goals, club2Goals);
    }

    /**
     * Creates a ClubSlot representing a specific club.
     *
     * @param clubId the ID of the club
     */
    public ClubSlot(int clubId) {
        this.clubIdWrapper = new ClubIdWrapper(clubId);
    }

    /**
     * Constructs a ClubSlot with the specified Tie.
     *
     * @param tie the Tie associated with this ClubSlot
     */
    public ClubSlot(Tie tie) {
        this.tie = tie;
    }

    public boolean isTie() {
        return tie != null;
    }

    public boolean isClub() {
        return clubIdWrapper != null;
    }

    public Tie getTie() {
        return tie;
    }

    public ClubIdWrapper getClubIdWrapper() {
        return clubIdWrapper;
    }

    /**
     * Retrieves the {@link Club} instance associated with the
     * {@code clubIdWrapper}'s ID of this {@link ClubSlot}.
     *
     * @return the {@link Club} corresponding to the {@code clubIdWrapper}'s ID
     */
    private Club getClub() {
        return ClubRepository.getClub(clubIdWrapper.id());
    }

    /**
     * Retrieves the ranking of this ClubSlot in the context of the given
     * tournament.
     *
     * @param callerTournament the tournament context
     * @return the ranking of this ClubSlot in the context of the given tournament
     */
    public float getRanking(Tournament callerTournament) {
        return isTie() ? tie.getRanking(callerTournament) : clubIdWrapper.getRanking();
    }

    /**
     * Returns the list of countries represented by this ClubSlot.
     * If this slot is a club, the list contains only that club's country.
     * If this slot is a tie, it aggregates (recursively) the countries of both
     * underlying club slots.
     *
     * @return list of countries for this ClubSlot.
     */
    public List<Country> getCountries() {
        if (isClub()) {
            return List.of(getClub().getCountry());
        } else {
            return Stream.of(tie.getClubSlot1(), tie.getClubSlot2())
                    .flatMap(cs -> cs.getCountries().stream())
                    .collect(Collectors.toList());
        }
    }

    /**
     * Resolves the current slot to a specific club based on the outcome of the
     * associated tie.
     * <p>
     * If this slot already represents a club, the method returns immediately.
     * Otherwise, it determines the winner of the tie and, depending on the
     * tournament hierarchy, assigns the appropriate club to this slot. If the
     * winner is not yet decided, the method returns without making changes.
     *
     * @param tournament the tournament context in which the slot is being
     *                   resolved
     */
    public void resolveSlot(Tournament tournament) {
        // System.out.println("1: " + toCompactString());
        if (isClub())
            return;
        DoubleLeggedTie t = (DoubleLeggedTie) getTie();
        Boolean club1Won = t.isClub1Winner();
        if (club1Won == null)
            return; // Vinner ikke avklart
        boolean higher = t.getTournament().compareTo(tournament) > 0; // innerTie på høyere nivå => ta taper
        ClubSlot chosen = (club1Won ^ higher) ? t.getClubSlot1() : t.getClubSlot2();
        this.clubIdWrapper = chosen.getClubIdWrapper();
        this.tie = null;
    }

    public void incrementSeedingCounter(boolean isSeeded) {
        if (isTie()) {
            tie.incrementSeedingCounter(isSeeded);
        } else if (isClub()) {
            clubIdWrapper.incrementSeedingCounter(isSeeded);
        }
    }

    /**
     * Returns a compact "Club1 vs Club2" string for ties, or the club name if not a
     * tie.
     *
     * @return compact string representation of this ClubSlot
     */
    public String toCompactString() {
        return isTie() ? tie.toCompactString() : clubIdWrapper.getName();
    }

    @Override
    public String toString() {
        return isTie() ? tie.toString() : clubIdWrapper.toString();
    }
}