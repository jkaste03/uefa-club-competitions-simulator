package com.github.jkaste03.seeding_prob_finder.model;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.jkaste03.seeding_prob_finder.enums.CompetitionData.Tournament;
import com.github.jkaste03.seeding_prob_finder.enums.Country;

public class ClubSlot implements Serializable {
    private DoubleLeggedTie tie;
    private ClubIdWrapper clubIdWrapper;

    private enum Type {
        TIE, CLUB
    }

    private Type type;

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

    public DoubleLeggedTie getTie() {
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
            return tie.getRankingAndResolveSlots(tournament);
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