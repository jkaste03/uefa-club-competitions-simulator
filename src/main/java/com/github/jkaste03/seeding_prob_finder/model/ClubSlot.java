package com.github.jkaste03.seeding_prob_finder.model;

import java.io.Serializable;

import com.github.jkaste03.seeding_prob_finder.enums.Tournament;
import com.github.jkaste03.seeding_prob_finder.service.ClubEloDataLoader;

public class ClubSlot implements Serializable {
    private Tie tie;
    private ClubIdWrapper clubIdWrapper;

    private enum Type {
        TIE, CLUB
    }

    private Type type;

    public ClubSlot(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament) {
        this.tie = new Tie(clubSlot1, clubSlot2, tournament);
        this.type = Type.TIE;
    }

    public ClubSlot(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament, Integer club1Goals,
            Integer club2Goals) {
        this.tie = new Tie(clubSlot1, clubSlot2, tournament, club1Goals, club2Goals);
        this.type = Type.TIE;
    }

    public ClubSlot(String name, float ranking, ClubEloDataLoader clubEloDataLoader) {
        this.clubIdWrapper = new ClubIdWrapper(new Club(name, ranking), clubEloDataLoader);
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