package com.example;

import java.util.Map;
import java.util.Random;

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
public class Tie extends ClubSlot {
    private ClubSlot clubSlot1;
    private ClubSlot clubSlot2;
    private int compLevel;
    private Integer club1Goals;
    private Integer club2Goals;

    public Tie(ClubSlot clubSlot1, ClubSlot clubSlot2, int compLevel) {
        this.clubSlot1 = clubSlot1;
        this.clubSlot2 = clubSlot2;
        this.compLevel = compLevel;
    }

    public Tie(ClubSlot clubSlot1, ClubSlot clubSlot2, int compLevel, Integer club1Goals, Integer club2Goals) {
        this.clubSlot1 = clubSlot1;
        this.clubSlot2 = clubSlot2;
        this.compLevel = compLevel;
        this.club1Goals = club1Goals;
        this.club2Goals = club2Goals;
    }

    public int getCompLevel() {
        return compLevel;
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

    /**
     * Calculates the ranking for this tie based on the competition level of the
     * caller.
     * <p>
     * If this tie's competition level is higher than the caller's, returns the
     * worst ranking of the two clubs. Otherwise, returns the best ranking.
     *
     * @param callerCompLevel the competition level from which the ranking is
     *                        requested
     * @return the ranking value for this tie in relation to the caller's
     *         competition level
     */
    @Override
    protected float getRanking(int callerCompLevel) {
        if (this.compLevel < callerCompLevel) {
            return Math.max(clubSlot1.getRanking(callerCompLevel), clubSlot2.getRanking(callerCompLevel));
        } else {
            return Math.min(clubSlot1.getRanking(callerCompLevel), clubSlot2.getRanking(callerCompLevel));
        }
    }

    public void play() {
        // Hent Elo for begge klubber
        double elo1 = ((Club) clubSlot1).getEloRating();
        double elo2 = ((Club) clubSlot2).getEloRating();

        // Dersom første ben ikke spilt: analytisk formel på samlet oppgjør
        if (club1Goals == null) {
            double dr = elo1 - elo2;
            double p1 = 1.0 / (Math.pow(10, -dr / 400.0) + 1.0);
            System.out.printf("%s går videre: %.2f%%%n",
                    ((Club) clubSlot1).getName(), p1 * 100);
            System.out.printf("%s går videre: %.2f%%%n",
                    ((Club) clubSlot2).getName(), (1 - p1) * 100);
            return;
        }

        // Første kamp spilt – simuler 2. kamp
        final int SIMS = 10_000;
        double hfa = 50; // konstant hjemmebanefordel (alle lik)
        double avgTotalGoals = 2.7; // anslått gjennomsnitt mål per kamp
        Random rnd = new Random();

        // Akkumulerte mål fra første kamp
        int agg1Base = club1Goals;
        int agg2Base = club2Goals;

        int count1 = 0, count2 = 0;
        int countTie = 0;

        // Beregn forventet Poisson‑lambda for 2. kamp
        // Her er clubSlot2 hjemmelag i returkampen:
        double eloHome = elo2 + hfa;
        double eloAway = elo1;
        double dr2 = eloHome - eloAway;
        // Vinn‑sannsynlighet for hjemmelag (uten uavgjort)
        double pHomeWin = 1.0 / (Math.pow(10, -dr2 / 400.0) + 1.0);
        // Fordel totalmålene proporsjonalt
        double lambdaHome = pHomeWin * avgTotalGoals;
        double lambdaAway = (1 - pHomeWin) * avgTotalGoals;

        // Knuths algoritme for Poisson‑trekking
        java.util.function.Function<Double, Integer> drawPoisson = (lambda) -> {
            double L = Math.exp(-lambda), p = 1.0;
            int k = 0;
            while (p > L) {
                p *= rnd.nextDouble();
                k++;
            }
            return k - 1;
        };

        for (int i = 0; i < SIMS; i++) {
            int goalsHome2 = drawPoisson.apply(lambdaHome);
            int goalsAway2 = drawPoisson.apply(lambdaAway);

            int total1 = agg1Base + goalsAway2;
            int total2 = agg2Base + goalsHome2;

            if (total1 > total2)
                count1++;
            else if (total2 > total1)
                count2++;
            else
                countTie++;
        }

        double pAdvance1 = (count1 + countTie * 0.5) / (double) SIMS;
        double pAdvance2 = (count2 + countTie * 0.5) / (double) SIMS;

        System.out.printf("%s går videre: %.2f%%%n",
                ((Club) clubSlot1).getName(), pAdvance1 * 100);
        System.out.printf("%s går videre: %.2f%%%n",
                ((Club) clubSlot2).getName(), pAdvance2 * 100);
    }

    @Override
    public String toString() {
        return "Tie{" +
                "compLevel=" + compLevel +
                ", clubSlot1=" + clubSlot1 +
                ", clubSlot2=" + clubSlot2 +
                ", club1Goals=" + club1Goals +
                ", club2Goals=" + club2Goals +
                '}';
    }
}
