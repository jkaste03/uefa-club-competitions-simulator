package com.example;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Represents a round in a football competition, managing club slots and their
 * seeding.
 * <p>
 * Each round has a competition level and contains a list of {@link ClubSlot}
 * instances.
 * </p>
 *
 * @author jkaste03
 */
public class Round {
    private int compLevel;
    private int roundNumber;
    private Round nextPrimaryRnd;
    private Round nextSecondaryRnd;
    private ArrayList<ClubSlot> clubSlots;
    private ArrayList<ClubSlot> seeded;
    private ArrayList<ClubSlot> unseeded;
    private ArrayList<ClubSlot> ties;

    public Round(int compLevel, int roundNumber) {
        this.compLevel = compLevel;
        this.roundNumber = roundNumber;
        clubSlots = new ArrayList<>();
        seeded = new ArrayList<>();
        unseeded = new ArrayList<>();
        ties = new ArrayList<>();
    }

    public void addClubSlot(ClubSlot clubSlot) {
        clubSlots.add(clubSlot);
    }

    public void addClubSlots(ArrayList<ClubSlot> clubSlots) {
        this.clubSlots.addAll(clubSlots);
    }

    public void addTie(ClubSlot tie) {
        ties.add(tie);
    }

    public void addTies(ArrayList<ClubSlot> ties) {
        this.ties.addAll(ties);
    }

    public int getCompLevel() {
        return compLevel;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public Round getNextPrimaryRnd() {
        return nextPrimaryRnd;
    }

    public Round getNextSecondaryRnd() {
        return nextSecondaryRnd;
    }

    public void setNextPrimaryRnd(Round nextPrimaryRnd) {
        this.nextPrimaryRnd = nextPrimaryRnd;
    }

    public void setNextSecondaryRnd(Round nextSecondaryRnd) {
        this.nextSecondaryRnd = nextSecondaryRnd;
    }

    public void setNextRounds(Round nextPrimaryRnd, Round nextSecondaryRnd) {
        this.nextPrimaryRnd = nextPrimaryRnd;
        this.nextSecondaryRnd = nextSecondaryRnd;
    }

    public ArrayList<ClubSlot> getClubSlots() {
        return clubSlots;
    }

    public ArrayList<ClubSlot> getSeeded() {
        return seeded;
    }

    public ArrayList<ClubSlot> getUnseeded() {
        return unseeded;
    }

    public ArrayList<ClubSlot> getTies() {
        return ties;
    }

    /**
     * Seeds club slots by ranking for the current competition level.
     * Top half go to seeded, bottom half to unseeded.
     */
    public void seed() {
        // Sort clubSlots by ranking descending
        clubSlots.sort((a, b) -> Float.compare(b.getRanking(compLevel), a.getRanking(compLevel)));
        int half = clubSlots.size() / 2;
        seeded.addAll(clubSlots.subList(0, half));
        unseeded.addAll(clubSlots.subList(half, clubSlots.size()));
    }

    public void play() {
        for (ClubSlot tie : ties) {
            tie.getTie().play();
        }
    }

    @Override
    public String toString() {
        return "Round{" +
                "compLevel=" + compLevel +
                ", roundNumber=" + roundNumber +
                ", clubSlots=" + clubSlots +
                ", seeded=" + seeded +
                ", unseeded=" + unseeded +
                ", ties=" + ties +
                '}';
    }
}
