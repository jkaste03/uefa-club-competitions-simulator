package com.github.jkaste03.seeding_prob_finder.model;

import java.io.Serializable;
import java.util.ArrayList;

import com.github.jkaste03.seeding_prob_finder.enums.CompetitionData.Tournament;
import com.github.jkaste03.seeding_prob_finder.enums.CompetitionData.RoundType;
import com.github.jkaste03.seeding_prob_finder.enums.CompetitionData.PathType;
import com.github.jkaste03.seeding_prob_finder.service.ClubEloDataLoader;

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
public class Round implements Serializable {
    private Tournament tournament;
    private RoundType roundType;
    private PathType pathType;
    private Round nextPrimaryRnd;
    private Round nextSecondaryRnd;
    private ArrayList<ClubSlot> clubSlots;
    private ArrayList<ClubSlot> seeded;
    private ArrayList<ClubSlot> unseeded;
    private ArrayList<ClubSlot> ties;

    public Round(Tournament tournament, RoundType roundType, PathType pathType) {
        this.tournament = tournament;
        this.roundType = roundType;
        this.pathType = pathType;
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

    public Tournament getTournament() {
        return tournament;
    }

    public RoundType getRoundType() {
        return roundType;
    }

    public String getName() {
        return tournament + " " + roundType + " " + pathType;
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
        clubSlots.sort((a, b) -> Float.compare(a.getRanking(tournament), b.getRanking(tournament)));
        int half = clubSlots.size() / 2;
        seeded.addAll(clubSlots.subList(0, half));
        unseeded.addAll(clubSlots.subList(half, clubSlots.size()));
    }

    public void play(ClubEloDataLoader clubEloDataLoader) {
        for (ClubSlot tie : ties) {
            tie.getTie().play(clubEloDataLoader);
        }
    }

    @Override
    public String toString() {
        return "Round{" +
                "tournament=" + tournament +
                ", roundType=" + roundType +
                ", clubSlots=" + clubSlots +
                ", seeded=" + seeded +
                ", unseeded=" + unseeded +
                ", ties=" + ties +
                '}';
    }
}
