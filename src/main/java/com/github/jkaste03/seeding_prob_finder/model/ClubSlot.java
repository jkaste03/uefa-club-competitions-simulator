package com.github.jkaste03.seeding_prob_finder.model;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.jkaste03.seeding_prob_finder.enums.Tournament;
import com.github.jkaste03.seeding_prob_finder.enums.Country;

public class ClubSlot implements Serializable {

    // This either represents a tie between two clubs or a single club; whichever is
    // not used is null.
    private Tie tie;
    private ClubIdWrapper clubIdWrapper;

    // Enum representing the type of this ClubSlot
    private enum Type {
        TIE, CLUB
    }

    private Type type;

    /**
     * Constructs a single‑leg tie between the two given child slots.
     * 
     * @param clubSlot1 left/first participant
     * @param clubSlot2 right/second participant
     */
    public ClubSlot(ClubSlot clubSlot1, ClubSlot clubSlot2) {
        this.tie = new SingleLeggedTie(clubSlot1, clubSlot2);
        this.type = Type.TIE;
    }

    /**
     * Constructs a two‑legged tie for the given child slots in the specified
     * tournament.
     * 
     * @param clubSlot1  first participant
     * @param clubSlot2  second participant
     * @param tournament tournament context
     */
    public ClubSlot(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament) {
        this.tie = new DoubleLeggedTie(clubSlot1, clubSlot2, tournament);
        this.type = Type.TIE;
    }

    /**
     * Constructs a two‑legged tie with optional preset goals (first leg).
     * 
     * @param clubSlot1  first participant
     * @param clubSlot2  second participant
     * @param tournament tournament context
     * @param club1Goals goals for club 1
     * @param club2Goals goals for club 2
     */
    public ClubSlot(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament, Integer club1Goals,
            Integer club2Goals) {
        this.tie = new DoubleLeggedTie(clubSlot1, clubSlot2, tournament, club1Goals, club2Goals);
        this.type = Type.TIE;
    }

    /**
     * Wraps a concrete club.
     * 
     * @param club club
     */
    public ClubSlot(Club club) {
        this.clubIdWrapper = new ClubIdWrapper(club.getId());
        this.type = Type.CLUB;
    }

    public boolean isTie() {
        return type == Type.TIE;
    }

    public boolean isClub() {
        return type == Type.CLUB;
    }

    public Tie getTie() {
        return tie;
    }

    public ClubIdWrapper getClubIdWrapper() {
        return clubIdWrapper;
    }

    /**
     * Retrieves the {@code Club} associated with this slot.
     * <p>
     * This method is only valid when this slot actually represents a club
     * (i.e., {@link #isClub()} returns {@code true}). If the slot does not
     * represent a concrete club, an {@link IllegalStateException} is thrown.
     *
     * @return the resolved {@code Club} instance for this slot
     * @throws IllegalStateException if this slot does not currently hold a club
     */
    public Club getClub() {
        if (isClub()) {
            return ClubRepository.getClub(clubIdWrapper.id());
        } else {
            throw new IllegalStateException("This ClubSlot is not a club");
        }
    }

    public float getRanking(Tournament tournament) {
        if (isTie()) {
            return tie.getRanking(tournament);
        } else if (isClub()) {
            return clubIdWrapper.getRanking();
        } else {
            throw new IllegalStateException("ClubSlot must be either a Tie or a Club");
        }
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

    // Kompakt versjon
    public void resolveSlot(Tournament callerTournament) {
        if (!isTie())
            return;
        DoubleLeggedTie t = (DoubleLeggedTie) this.getTie();
        Boolean club1Won = t.isClub1Winner();
        if (club1Won == null)
            return; // Vinner ikke avklart
        boolean higher = t.getTournament().compareTo(callerTournament) > 0; // innerTie på høyere nivå => ta taper
        ClubSlot chosen = (club1Won ^ higher) ? t.getClubSlot1() : t.getClubSlot2();
        this.clubIdWrapper = chosen.getClubIdWrapper();
        this.tie = null;
        this.type = Type.CLUB;
    }

    public void incrementSeedingCounter(boolean isSeeded) {
        if (isTie()) {
            tie.incrementSeedingCounter(isSeeded);
        } else if (isClub()) {
            clubIdWrapper.incrementSeedingCounter(isSeeded);
        }
    }

    public String toCompactString() {
        return isTie() ? tie.toCompactString() : clubIdWrapper.getName();
    }

    @Override
    public String toString() {
        return isTie() ? tie.toString() : clubIdWrapper.toString();
    }
}