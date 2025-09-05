package com.github.jkaste03.uefaccsim.model;

import java.util.function.Function;

import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.service.ClubEloDataLoader;

/**
 * DoubleLeggedTie is a specialized implementation of the Tie class that
 * represents a double-legged tie between two clubs.
 * <p>
 * This class extends the abstract Tie class and implements the specific
 * behavior for a double-legged tie, including playing the two legs.
 */
public class DoubleLeggedTie extends Tie {

    /**
     * Constructs a two‑legged tie for the given slots in the specified
     * tournament. Order matters.
     * 
     * @param clubSlot1  home participant first leg
     * @param clubSlot2  away participant first leg
     * @param tournament tournament
     */
    public DoubleLeggedTie(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament) {
        super(clubSlot1, clubSlot2, tournament);
    }

    /**
     * Constructs a two‑legged tie with preset goals (first leg). Order matters.
     * 
     * @param clubSlot1  home participant first leg
     * @param clubSlot2  away participant first leg
     * @param club1Goals goals for club 1
     * @param club2Goals goals for club 2
     * @param tournament tournament
     */
    public DoubleLeggedTie(ClubSlot clubSlot1, ClubSlot clubSlot2, Integer club1Goals,
            Integer club2Goals, Tournament tournament) {
        super(clubSlot1, clubSlot2, club1Goals, club2Goals, tournament);
    }

    /**
     * Kjører én simulering av returkampen (eller analytisk første kamp hvis eneste
     * kamp)
     * og setter club1Winner = true/false.
     */
    @Override
    public void play(ClubEloDataLoader clubEloDataLoader) {
        // System.out.println(
        // "Playing double-legged tie: " + clubSlot1.toCompactString() + " vs " +
        // clubSlot2.toCompactString());
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
        return "DoubleLeggedTie[" + fieldsToString() + ", tournament=" + tournament + ", club1Winner=" + club1Winner
                + "]";
    }
}
