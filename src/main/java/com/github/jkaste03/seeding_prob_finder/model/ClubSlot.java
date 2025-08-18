package com.github.jkaste03.seeding_prob_finder.model;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.jkaste03.seeding_prob_finder.enums.CompetitionData.Tournament;
import com.github.jkaste03.seeding_prob_finder.enums.Country;

public class ClubSlot implements Serializable {
    private Tie tie;
    private ClubIdWrapper clubIdWrapper;

    private enum Type {
        TIE, CLUB
    }

    private Type type;

    public ClubSlot(ClubSlot clubSlot1, ClubSlot clubSlot2) {
        this.tie = new SingleLeggedTie(clubSlot1, clubSlot2);
        this.type = Type.TIE;
    }

    public ClubSlot(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament) {
        this.tie = new DoubleLeggedTie(clubSlot1, clubSlot2, tournament);
        this.type = Type.TIE;
    }

    public ClubSlot(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament, Integer club1Goals,
            Integer club2Goals) {
        this.tie = new DoubleLeggedTie(clubSlot1, clubSlot2, tournament, club1Goals, club2Goals);
        this.type = Type.TIE;
    }

    public ClubSlot(String name, Country country, float ranking) {
        this.clubIdWrapper = new ClubIdWrapper(new Club(name, country, ranking));
        this.type = Type.CLUB;
    }

    public ClubSlot(Club club) {
        this.clubIdWrapper = new ClubIdWrapper(club);
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

    public Club getClub() {
        if (isClub()) {
            return ClubRepository.getClub(clubIdWrapper.getId());
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