package com.github.jkaste03.seeding_prob_finder.model;

import java.util.function.Function;

import com.github.jkaste03.seeding_prob_finder.enums.Tournament;
import com.github.jkaste03.seeding_prob_finder.service.ClubEloDataLoader;

/**
 * SingleLeggedTie is a specialized implementation of the Tie class that
 * represents a single-legged tie between two clubs.
 * <p>
 * This class extends the abstract Tie class and implements the specific
 * behavior for a single-legged tie, including score calculation and determining
 * the winner.
 */
public class DoubleLeggedTie extends Tie {
    private Tournament tournament;

    /*
     * * Constructs a new double-legged tie with the specified club slots.
     */
    public DoubleLeggedTie(ClubSlot club1, ClubSlot club2, Tournament tournament) {
        super(club1, club2);
        this.tournament = tournament;
    }

    public DoubleLeggedTie(ClubSlot club1, ClubSlot club2, Tournament tournament, Integer club1Goals,
            Integer club2Goals) {
        super(club1, club2, club1Goals, club2Goals);
        this.tournament = tournament;
    }

    public Tournament getTournament() {
        return tournament;
    }

    // Ny metode: løser opp (resolves) eventuelle underliggende Ties til konkrete
    // ClubSlots
    public void resolveSlots() {
        clubSlot1 = resolveSlot(clubSlot1);
        clubSlot2 = resolveSlot(clubSlot2);
    }

    // Kompakt versjon
    private ClubSlot resolveSlot(ClubSlot slot) {
        if (!slot.isTie())
            return slot;
        DoubleLeggedTie t = (DoubleLeggedTie) slot.getTie();
        Boolean club1Won = t.isClub1Winner();
        if (club1Won == null)
            return slot; // Vinner ikke avklart
        boolean higher = t.getTournament().compareTo(tournament) > 0; // innerTie på høyere nivå => ta taper
        return (club1Won ^ higher) ? t.getClubSlot1() : t.getClubSlot2();
    }

    // Kompakt versjon
    @Override
    public float getRanking(Tournament callerTournament) {
        boolean higher = tournament.compareTo(callerTournament) > 0;
        Boolean club1Won = isClub1Winner();
        if (club1Won != null) {
            return ((club1Won ^ higher) ? clubSlot1 : clubSlot2).getRanking(tournament);
        }
        float r1 = clubSlot1.getRanking(tournament);
        float r2 = clubSlot2.getRanking(tournament);
        return higher ? Math.max(r1, r2) : Math.min(r1, r2);
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
}
