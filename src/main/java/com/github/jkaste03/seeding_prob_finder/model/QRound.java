package com.github.jkaste03.seeding_prob_finder.model;

import java.util.List;

import com.github.jkaste03.seeding_prob_finder.enums.CompetitionData;
import com.github.jkaste03.seeding_prob_finder.service.ClubEloDataLoader;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Class representing a qualifying round in the UEFA competitions.
 */
public class QRound extends Round {
    // Constants for clubs skipping a round (e.g., UCL Q1 CP to UECL Q3 CP)
    private final static int UCL_Q1_CP_TIES_NO_REBALANCE = 16;
    // private final static String ROUND_CLUBS_SKIP_TO =
    // Tournament.CONFERENCE_LEAGUE + " " + CompetitionData.RoundType.Q3
    // + " " + CompetitionData.PathType.CHAMPIONS_PATH;

    private CompetitionData.PathType pathType;
    private List<ClubSlot> seeded = new ArrayList<>();
    private List<ClubSlot> unseeded = new ArrayList<>();
    private List<ClubSlot> ties = new ArrayList<>();

    /**
     * Constructs a qualifying round for the specified tournament and round type,
     * using the provided path type to determine the qualifying route. Also adds
     * clubs by reading from a JSON source.
     *
     * @param tournament the tournament for which this qualifying round is
     *                   initialized.
     * @param roundType  the type of the qualifying round.
     * @param pathType   the path type representing the qualifying route.
     */
    public QRound(CompetitionData.Tournament tournament, CompetitionData.RoundType roundType,
            CompetitionData.PathType pathType) {
        super(tournament, roundType);
        this.pathType = pathType;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a string representation of the qualifying round, including the
     * tournament, round type, and path type.
     */
    @Override
    public String getName() {
        return super.getName() + " " + roundType + " " + pathType;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Seeds the clubSlots in the qualifying round.
     * Throws an IllegalArgumentException if the number of clubSlots is odd.
     */
    @Override
    public void seed() {
        System.out.println(getName());
        System.out.println(clubSlots.size());
        if (clubSlots == null || clubSlots.size() % 2 != 0) {
            throw new IllegalArgumentException("The number of clubSlots must be even to seed them properly.");
        }

        // Sort clubSlots by ranking descending
        clubSlots.sort((a, b) -> Float.compare(a.getRanking(tournament), b.getRanking(tournament)));
        int half = clubSlots.size() / 2;
        seeded.addAll(clubSlots.subList(0, half));
        unseeded.addAll(clubSlots.subList(half, clubSlots.size()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Draws the ties for the qualifying round.
     * Ensures that clubs from the same country do not meet.
     * First, it pairs seeded clubs that have at least one club from the same
     * country among the unseeded.
     * Then, it pairs the remaining seeded clubs with the remaining unseeded clubs.
     */
    @Override
    public void draw() {
        List<ClubSlot> remainingSeeded = new ArrayList<>(seeded);
        List<ClubSlot> remainingUnseeded = new ArrayList<>(unseeded);
        ties.clear();

        // First, draw opponents for seeded clubs that have at least one club that it's
        // illegal to meet
        seeded.stream()
                .filter(seeded -> remainingUnseeded.stream().anyMatch(unseeded -> isIllegalTie(seeded, unseeded)))
                .forEach(seeded -> {
                    ClubSlot opponent;
                    do {
                        opponent = remainingUnseeded.get((int) (Math.random() * remainingUnseeded.size()));
                    } while (isIllegalTie(seeded, opponent));
                    remainingSeeded.remove(seeded);
                    remainingUnseeded.remove(opponent);
                    ties.add(Math.random() < 0.5 ? new ClubSlot(seeded, opponent, tournament)
                            : new ClubSlot(opponent, seeded, tournament));
                });

        // Then, draw opponents for the remaining seeded clubs
        remainingSeeded.forEach(seeded -> {
            ClubSlot opponent = remainingUnseeded.remove((int) (Math.random() * remainingUnseeded.size()));
            ties.add(Math.random() < 0.5 ? new ClubSlot(seeded, opponent, tournament)
                    : new ClubSlot(opponent, seeded, tournament));
        });
    }

    /**
     * Registers ties for the next rounds.
     * <p>
     * If there are clubs that must skip the secondary round, the ties are shuffled
     * to randomize which ties get to skip. Then, ties are added to the next primary
     * round and, if applicable, to the next secondary round.
     * </p>
     * <p>
     * For each tie:
     * <ul>
     * <li>The tie is added to the next primary round.</li>
     * <li>If the next secondary round is applicable:
     * <ul>
     * <li>If the tie can skip the secondary round, it is added to the next primary
     * round of the secondary round.</li>
     * <li>Otherwise, it is added to the secondary round.</li>
     * </ul>
     * </li>
     * </ul>
     * </p>
     */
    public void regTiesForNextRounds() {
        // If ties must skip the secondary round, shuffle the ties to randomize which
        // ties get to skip
        int noOfClubsToSkipSecondary = noOfClubsCanSkipSecondary();
        if (noOfClubsToSkipSecondary > 0) {
            Collections.shuffle(ties);
        }
        // Add ties to the next primary round and the next secondary round if applicable
        ties.forEach(tie -> {
            // Add tie to the next primary round
            this.nextPrimaryRnd.addClubSlot(tie);
            // Add tie to the next secondary round if applicable
            if (this.nextSecondaryRnd != null) {
                // Add tie to the next primary round of the secondary round if it can skip,
                // otherwise add to the secondary round
                if (ties.indexOf(tie) < noOfClubsToSkipSecondary) {
                    this.nextSecondaryRnd.nextPrimaryRnd.addClubSlot(tie);
                } else {
                    this.nextSecondaryRnd.addClubSlot(tie);
                }
            }
        });
    }

    /**
     * Determines the number of clubs that can skip the secondary round.
     * 
     * @return the number of clubs that can skip the secondary round.
     */
    private int noOfClubsCanSkipSecondary() {
        int noOfClubsToSkip = (tournament == CompetitionData.Tournament.CHAMPIONS_LEAGUE
                && roundType == CompetitionData.RoundType.Q1
                && pathType == CompetitionData.PathType.CHAMPIONS_PATH) ? UCL_Q1_CP_TIES_NO_REBALANCE - ties.size() : 0;
        return noOfClubsToSkip;
    }

    /**
     * Plays the ties in the qualifying round.
     */
    public void play(ClubEloDataLoader clubEloDataLoader) {
        System.out.println("Playing qualifying round: " + getName());
        for (ClubSlot tie : ties) {
            System.out.println("Playing tie: " + tie.toCompactString());
        }
        for (ClubSlot tie : ties) {
            tie.getTie().play(clubEloDataLoader);
        }
        // Todo: Update the clubEloDataLoader with the new Elo ratings after the matches
    }

    @Override
    public String toString() {
        return "QRound [name=" + getName() + ", toString()=" + super.toString() + ", seededClubs="
                + seeded + ", unseededClubs=" + unseeded + "]";
    }
}
