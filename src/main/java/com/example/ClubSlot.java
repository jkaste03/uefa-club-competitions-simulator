package com.example;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ClubSlot implements Serializable {
    private Tie tie;
    private transient Club club;

    private enum Type {
        TIE, CLUB
    }

    private Type type;
    private String clubName; // Store club name for serialization
    private float clubRanking; // Store club ranking for serialization

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
        // Store these values for serialization
        this.clubName = name;
        this.clubRanking = ranking;
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

    public void incrementSeedingCounter(boolean isSeeded) {
        if (isTie()) {
            tie.incrementSeedingCounter(isSeeded);
        } else if (isClub()) {
            club.incrementSeedingCounter(isSeeded);
        }
    }

    public String toCompactString() {
        return isTie() ? tie.toCompactString() : club.getName();
    }

    @Override
    public String toString() {
        return isTie() ? tie.toString() : club.toString();
    }

    // Custom serialization to handle the transient club field
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject(); // Write default serializable fields
    }

    // Custom deserialization to reconstruct the club object
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject(); // Read default serializable fields

        // If this was a club type, reconstruct the club object
        if (type == Type.CLUB && clubName != null) {
            this.club = ClubRepository.getClubByName(clubName);

            // If the club doesn't exist in repository, create a new one
            if (this.club == null) {
                this.club = new Club(clubName, clubRanking);
            }
        }
    }
}