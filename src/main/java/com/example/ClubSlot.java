package com.example;

public class ClubSlot {
    private Tie tie;
    private Club club;

    private enum Type {
        TIE, CLUB
    }

    private Type type;

    public ClubSlot(ClubSlot clubSlot1, ClubSlot clubSlot2, int compLevel) {
        this.tie = new Tie(clubSlot1, clubSlot2, compLevel);
        this.type = Type.TIE;
    }

    public ClubSlot(ClubSlot clubSlot1, ClubSlot clubSlot2, int compLevel, Integer club1Goals, Integer club2Goals) {
        this.tie = new Tie(clubSlot1, clubSlot2, compLevel, club1Goals, club2Goals);
        this.type = Type.TIE;
    }

    public ClubSlot(String name, float ranking) {
        this.club = new Club(name, ranking);
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

    public Club getClub() {
        return club;
    }

    public float getRanking(int compLevel) {
        if (isTie()) {
            return tie.getRanking(compLevel);
        } else if (isClub()) {
            return club.getRanking(compLevel);
        } else {
            throw new IllegalStateException("ClubSlot must be either a Tie or a Club");
        }
    }

    public String toCompactString() {
        return isTie() ? tie.toCompactString() : club.getName();
    }
}
