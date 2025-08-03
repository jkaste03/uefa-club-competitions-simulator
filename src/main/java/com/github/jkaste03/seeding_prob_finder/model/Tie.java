package com.github.jkaste03.seeding_prob_finder.model;

import java.io.Serializable;
import java.util.Random;
import java.util.function.Function;

import com.github.jkaste03.seeding_prob_finder.enums.CompetitionData.Tournament;
import com.github.jkaste03.seeding_prob_finder.service.ClubEloDataLoader;

/**
 * Represents a tie between two clubs at a specific competition level.
 * <p>
 * A {@code Tie} consists of two {@link Club} instances and an integer
 * indicating
 * the competition level at which the tie occurs. The class provides methods to
 * access the clubs and competition level, as well as to calculate a ranking
 * value for the tie relative to a given competition level.
 * <p>
 * The ranking calculation considers whether the tie's competition level is
 * higher
 * or lower than the caller's level, returning either the best or worst ranking
 * among the two clubs accordingly.
 *
 * @author jkaste03
 */
public class Tie implements Serializable {
    private ClubSlot clubSlot1;
    private ClubSlot clubSlot2;
    private Tournament tournament;
    private Integer club1Goals;
    private Integer club2Goals;
    private Boolean club1Winner;

    private static final int SIMS = 10_000; // kun én simulering
    private static final double HFA = 50; // hjemmebanefordel
    private static final double AVG_GOALS = 2.7;
    private static final Random rnd = new Random();

    public Tie(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament) {
        this.clubSlot1 = clubSlot1;
        this.clubSlot2 = clubSlot2;
        this.tournament = tournament;
    }

    public Tie(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament, Integer club1Goals,
            Integer club2Goals) {
        this.clubSlot1 = clubSlot1;
        this.clubSlot2 = clubSlot2;
        this.tournament = tournament;
        this.club1Goals = club1Goals;
        this.club2Goals = club2Goals;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public ClubSlot getClubSlot1() {
        return clubSlot1;
    }

    public ClubSlot getClubSlot2() {
        return clubSlot2;
    }

    public Integer getClub1Goals() {
        return club1Goals;
    }

    public Integer getClub2Goals() {
        return club2Goals;
    }

    /** Getter for å sjekke hvem som vant etter playOutcome() */
    public Boolean isClub1Winner() {
        return club1Winner;
    }

    public void incrementSeedingCounter(boolean isSeeded) {
        clubSlot1.incrementSeedingCounter(isSeeded);
        clubSlot2.incrementSeedingCounter(isSeeded);
    }

    /**
     * Calculates the ranking for this Tie based on the competition level of the
     * caller.
     * <p>
     * If ClubSlot1 or ClubSlot2 is a Tie and has a winner, assigns the correct club
     * (winner or loser depending on competition level) to ClubSlot1/ClubSlot2
     * before ranking calculation.
     * <p>
     * If the Tie has a winner, the method returns either the winner's or the
     * loser's ranking
     * depending on the relationship between this Tie's competition level and the
     * caller's.
     * <ul>
     * <li>If this Tie's competition level is better than the caller's, returns the
     * loser's ranking.</li>
     * <li>Else, returns the winner's ranking.</li>
     * </ul>
     * If the Tie does not have a winner, the method returns:
     * <ul>
     * <li>The worst ranking of the two clubs if this Tie's competition level is
     * better than the caller's.</li>
     * <li>The best ranking of the two clubs otherwise.</li>
     * </ul>
     *
     * @param callerCompLevel the competition level of the caller
     * @return the calculated ranking for this Tie at the given competition level
     */
    public float getRanking(Tournament callerTournament) {
        // Resolve ClubSlot1 if it's a Tie and has a winner
        if (clubSlot1.isTie()) {
            Tie innerTie = clubSlot1.getTie();
            Boolean winner = innerTie.isClub1Winner();
            if (winner != null) {
                if (clubSlot1.getTie().getTournament().compareTo(this.tournament) > 0) {
                    // assign loser
                    clubSlot1 = winner ? innerTie.getClubSlot2() : innerTie.getClubSlot1();
                } else {
                    // assign winner
                    clubSlot1 = winner ? innerTie.getClubSlot1() : innerTie.getClubSlot2();
                }
            }
        }
        // Resolve ClubSlot2 if it's a Tie and has a winner
        if (clubSlot2.isTie()) {
            Tie innerTie = clubSlot2.getTie();
            Boolean winner = innerTie.isClub1Winner();
            if (winner != null) {
                if (clubSlot2.getTie().getTournament().compareTo(this.tournament) > 0) {
                    // assign loser
                    clubSlot2 = winner ? innerTie.getClubSlot2() : innerTie.getClubSlot1();
                } else {
                    // assign winner
                    clubSlot2 = winner ? innerTie.getClubSlot1() : innerTie.getClubSlot2();
                }
            }
        }

        // If this is a Tie with a winner, use the loser's/winner's ranking depending on
        // the level
        if (isClub1Winner() != null) {
            boolean club1Won = isClub1Winner();
            if (this.tournament.compareTo(callerTournament) > 0) {
                // Return loser's ranking
                return club1Won
                        ? clubSlot2.getRanking(this.tournament)
                        : clubSlot1.getRanking(this.tournament);
            } else {
                // Return winner's ranking
                return club1Won
                        ? clubSlot1.getRanking(this.tournament)
                        : clubSlot2.getRanking(this.tournament);
            }
        }
        // Otherwise: existing logic
        if (this.tournament.compareTo(callerTournament) > 0) {
            return Math.max(clubSlot1.getRanking(this.tournament), clubSlot2.getRanking(this.tournament));
        } else {
            return Math.min(clubSlot1.getRanking(this.tournament), clubSlot2.getRanking(this.tournament));
        }
    }

    /**
     * Kjører én simulering av returkampen (eller analytisk første kamp hvis eneste
     * kamp)
     * og setter club1Winner = true/false.
     */
    public void play(ClubEloDataLoader clubEloDataLoader) {
        ClubIdWrapper club1 = clubSlot1.getClubIdWrapper();
        ClubIdWrapper club2 = clubSlot2.getClubIdWrapper();

        // Første ben ikke spilt: analytisk-probabilistisk enkeltutfall
        if (club1Goals == null) {
            double dr = club1.getEloRating(clubEloDataLoader) - club2.getEloRating(clubEloDataLoader);
            double p1 = 1.0 / (Math.pow(10, -dr / 400.0) + 1.0);
            // én trekning basert på p1
            this.club1Winner = rnd.nextDouble() < p1;
            return;
        }

        // Første kamp er spilt, simuler returkampen én gang:
        int agg1 = club1Goals;
        int agg2 = club2Goals;

        // Elo-justert hjemmebanefordel for returkamp (clubSlot2 er hjemmelag)
        double eloHome = club2.getEloRating(clubEloDataLoader) + HFA;
        double eloAway = club1.getEloRating(clubEloDataLoader);
        double dr2 = eloHome - eloAway;
        double pHomeWin = 1.0 / (Math.pow(10, -dr2 / 400.0) + 1.0);

        double lambdaHome = pHomeWin * AVG_GOALS;
        double lambdaAway = (1 - pHomeWin) * AVG_GOALS;

        // Poisson-trekker:
        Function<Double, Integer> drawPoisson = lambda -> {
            double L = Math.exp(-lambda), p = 1.0;
            int k = 0;
            while (p > L) {
                p *= rnd.nextDouble();
                k++;
            }
            return k - 1;
        };

        int goalsHome2 = drawPoisson.apply(lambdaHome);
        int goalsAway2 = drawPoisson.apply(lambdaAway);

        int total1 = agg1 + goalsAway2;
        int total2 = agg2 + goalsHome2;

        if (total1 > total2) {
            club1Winner = true;
        } else if (total2 > total1) {
            club1Winner = false;
        } else {
            // uavgjort etter aggregat – avgjør med coin flip (eller egen logikk)
            club1Winner = rnd.nextBoolean();
        }
    }

    // public void play() {
    // Club club1 = clubSlot1.getClub();
    // Club club2 = clubSlot2.getClub();

    // // Hent Elo for begge klubber
    // double elo1 = club1.getEloRating();
    // double elo2 = club2.getEloRating();

    // // Dersom første ben ikke spilt: analytisk formel på samlet oppgjør
    // if (club1Goals == null) {
    // double dr = elo1 - elo2;
    // double p1 = 1.0 / (Math.pow(10, -dr / 400.0) + 1.0);
    // System.out.printf("%s går videre: %.2f%%%n",
    // club1.getName(), p1 * 100);
    // System.out.printf("%s går videre: %.2f%%%n",
    // club2.getName(), (1 - p1) * 100);
    // return;
    // }

    // // Akkumulerte mål fra første kamp
    // int agg1Base = club1Goals;
    // int agg2Base = club2Goals;

    // int count1 = 0, count2 = 0;
    // int countTie = 0;

    // // Beregn forventet Poisson‑lambda for 2. kamp
    // // Her er clubSlot2 hjemmelag i returkampen:
    // double eloHome = elo2 + HFA;
    // double eloAway = elo1;
    // double dr2 = eloHome - eloAway;
    // // Vinn‑sannsynlighet for hjemmelag (uten uavgjort)
    // double pHomeWin = 1.0 / (Math.pow(10, -dr2 / 400.0) + 1.0);
    // // Fordel totalmålene proporsjonalt
    // double lambdaHome = pHomeWin * AVG_GOALS;
    // double lambdaAway = (1 - pHomeWin) * AVG_GOALS;

    // // Knuths algoritme for Poisson‑trekking
    // java.util.function.Function<Double, Integer> drawPoisson = (lambda) -> {
    // double L = Math.exp(-lambda), p = 1.0;
    // int k = 0;
    // while (p > L) {
    // p *= rnd.nextDouble();
    // k++;
    // }
    // return k - 1;
    // };

    // for (int i = 0; i < SIMS; i++) {
    // int goalsHome2 = drawPoisson.apply(lambdaHome);
    // int goalsAway2 = drawPoisson.apply(lambdaAway);

    // int total1 = agg1Base + goalsAway2;
    // int total2 = agg2Base + goalsHome2;

    // if (total1 > total2)
    // count1++;
    // else if (total2 > total1)
    // count2++;
    // else
    // countTie++;
    // }

    // double pAdvance1 = (count1 + countTie * 0.5) / (double) SIMS;
    // double pAdvance2 = (count2 + countTie * 0.5) / (double) SIMS;

    // System.out.printf("%s går videre: %.2f%%%n",
    // club1.getName(), pAdvance1 * 100);
    // System.out.printf("%s går videre: %.2f%%%n",
    // club2.getName(), pAdvance2 * 100);
    // }

    @Override
    public String toString() {
        return "Tie{" +
                "tournament=" + tournament +
                ", clubSlot1=" + clubSlot1 +
                ", clubSlot2=" + clubSlot2 +
                ", club1Goals=" + club1Goals +
                ", club2Goals=" + club2Goals +
                '}';
    }

    public String toCompactString() {
        return clubSlot1.toCompactString() + " vs " +
                clubSlot2.toCompactString();
    }
}
