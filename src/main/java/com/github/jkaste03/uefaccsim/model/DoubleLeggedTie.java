package com.github.jkaste03.uefaccsim.model;

import java.util.Random;

import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.service.ClubEloDataLoader;

/**
 * DoubleLeggedTie is a specialized implementation of the Tie class that
 * represents a double-legged tie between two clubs.
 *
 * This implementation contains a full, self-contained match-scoring model:
 * - Elo -> expected goals (exponential scaling)
 * - Negative-binomial sampling via Poisson-Gamma mixture
 * - Dixon-Coles style draw-inflation for low draws
 * - Extra-time and penalty resolution
 * - Two-legged simulation (simulerer første ben først, så returbenet)
 *
 * Tweak the PARAM_* constants to calibrate behaviour for your data.
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

    // ----------------- Model parameters (tuneable) -----------------
    private static final double PARAM_MEAN_GOALS_PER_TEAM = 1.35; // mu (team mean)
    private static final double PARAM_K = 0.55; // Elo -> goals scaling
    private static final double PARAM_HFA = 65.0; // home-field advantage in Elo points
    private static final double PARAM_THETA = 35.0; // NB dispersion
    private static final double[] PARAM_DRAW_INFLATION = { 0.09, 0.05, 0.01 }; // for fav goals 0,1,2
    private static final int PARAM_GMAX = 7; // max goals modelled explicitly
    private static final double PARAM_ET_FACTOR = 30.0 / 90.0; // extra time duration fraction
    // private static final int PARAM_MC_SIMULATIONS = 5000; // simulations when
    // estimating probabilities
    private static final double PARAM_PENALTY_BETA = 0.18; // logistic scale for penalty win prob

    private static final Random RNG = new Random();

    /**
     * Kjører én simulering av returkampen (eller simulerer begge ben hvis ingen
     * ben er spilt) og setter club1Winner = true/false.
     *
     * Behaviour:
     * - If first leg (club1Goals) is null: simulate FIRST LEG (club1 home), THEN
     * SECOND LEG (club2 home). Apply away-goals, then ET (in second leg), then
     * penalties.
     * - If first leg exists: treat that as first leg result (clubSlot1 home),
     * simulate return leg (clubSlot2 home), compute aggregate, apply away-goals,
     * then ET and penalties if needed.
     *
     * Note: Away-goals are applied before ET (common in many competitions). Adjust
     * logic if your tournament rules differ.
     */
    @Override
    public void play(ClubEloDataLoader clubEloDataLoader) {
        ClubIdWrapper club1 = clubSlot1.getClubIdWrapper();
        ClubIdWrapper club2 = clubSlot2.getClubIdWrapper();

        double elo1 = club1.getEloRating(clubEloDataLoader);
        double elo2 = club2.getEloRating(clubEloDataLoader);

        // If no first-leg score known -> simulate both legs in order
        if (club1Goals == null) {
            // FIRST LEG: club1 at home
            simulateMatch(elo1, elo2, true);
            System.out.println("First leg result: " + club1.getName() + " " + club1Goals + " - " + club2Goals + " "
                    + club2.getName());
            return;
        }

        // SECOND LEG: club2 at home
        simulateMatch(elo2, elo1, false);

        if (club1Goals != club2Goals) {
            club1Winner = club1Goals > club2Goals;
            System.out.println("Aggregate result: " + club1.getName() + " " + club1Goals + " - " + club2Goals + " "
                    + club2.getName());
            return;
        }

        // is club2,
        // but we want to add ET goals to aggregates for club1/club2 correctly.
        // We simulated as (elo1, elo2) above for convenience; adjust to simulate ET at
        // second leg venue:
        // Re-simulate ET correctly at second leg venue:
        simulateExtraTime(elo2, elo1); // simulate ET with club2 at home
        // et.home == extra-time goals for club2; et.away == extra-time goals for club1

        if (club1Goals != club2Goals) {
            club1Winner = club1Goals > club2Goals;
            System.out.println("Aggregate result (AET): " + club1.getName() + " " + club1Goals + " - " + club2Goals
                    + " " + club2.getName());
            return;
        }

        // Penalties in second leg
        // simulatePenaltyWinner expects (eloHome, eloAway) where home is venue of
        // penalties (club2)
        boolean homePenaltyWinner = simulatePenaltyWinner(elo2, elo1);
        // if homePenaltyWinner == true => club2 wins penalties => club1 loses
        club1Winner = !homePenaltyWinner;
        System.out.println("Aggregate result: " + club1.getName() + " " + club1Goals + " - " + club2Goals + " "
                + club2.getName());
        System.out.println("Penalty shootout winner: " + (club1Winner ? club1.getName() : club2.getName()));
        return;
    }

    // ----------------- Model core functions (adapted / self-contained)
    // -----------------

    private static double[] eloToLambdas(double eloHome, double eloAway) {
        double dr = eloHome - eloAway + PARAM_HFA; // add home advantage to home team's Elo
        double lambdaHome = PARAM_MEAN_GOALS_PER_TEAM * Math.exp(PARAM_K * dr / 400.0);
        double lambdaAway = PARAM_MEAN_GOALS_PER_TEAM * Math.exp(PARAM_K * (-dr) / 400.0);
        return new double[] { lambdaHome, lambdaAway };
    }

    // simulate one match (home vs away) using conditional NB-sampling and
    // draw-inflation
    private void simulateMatch(double eloHome, double eloAway, boolean firstLeg) {
        double[] lambdas = eloToLambdas(eloHome, eloAway);
        double lHome = lambdas[0], lAway = lambdas[1];

        boolean favIsHome = lHome >= lAway;
        double lambdaFav = favIsHome ? lHome : lAway;
        double lambdaUnd = favIsHome ? lAway : lHome;

        int gFav = sampleNegBinomial(lambdaFav, PARAM_THETA);
        if (gFav > PARAM_GMAX)
            gFav = PARAM_GMAX;

        double[] pmfUnd = negBinomPmfVector(lambdaUnd, PARAM_THETA, PARAM_GMAX);

        if (gFav <= 2) {
            int idx = gFav;
            double factor = 1.0 + PARAM_DRAW_INFLATION[idx];
            pmfUnd[idx] *= factor;
            // renormalize
            double sum = 0.0;
            for (double v : pmfUnd)
                sum += v;
            for (int i = 0; i < pmfUnd.length; i++)
                pmfUnd[i] /= sum;
        }

        int gUnd = sampleFromPmf(pmfUnd);
        int homeGoals = favIsHome ? gFav : gUnd;
        int awayGoals = favIsHome ? gUnd : gFav;
        club1Goals = (club1Goals == null) ? (firstLeg ? homeGoals : awayGoals)
                : club1Goals + (firstLeg ? homeGoals : awayGoals);
        club2Goals = (club2Goals == null) ? (firstLeg ? awayGoals : homeGoals)
                : club2Goals + (firstLeg ? awayGoals : homeGoals);
    }

    // simulate extra-time (shorter period). For ET in the second leg use
    // (eloHome=secondLegHome, eloAway=secondLegAway).
    private void simulateExtraTime(double eloHome, double eloAway) {
        double[] lambdas = eloToLambdas(eloHome, eloAway);
        double lHome = lambdas[0] * PARAM_ET_FACTOR;
        double lAway = lambdas[1] * PARAM_ET_FACTOR;
        club2Goals += sampleNegBinomial(lHome, PARAM_THETA);
        club1Goals += sampleNegBinomial(lAway, PARAM_THETA);
    }

    // simulate penalty winner based on Elo diff (logistic)
    private static boolean simulatePenaltyWinner(double eloHome, double eloAway) {
        double dr = eloHome - eloAway;
        double z = PARAM_PENALTY_BETA * dr / 400.0;
        double pHome = 1.0 / (1.0 + Math.exp(-z));
        return RNG.nextDouble() < pHome;
    }

    // ----------------- Probability primitives -----------------

    // Poisson sampler (Knuth for small lambda, normal approximation for large)
    private static int samplePoisson(double lambda) {
        if (lambda <= 0)
            return 0;
        if (lambda < 30.0) {
            double L = Math.exp(-lambda);
            double p = 1.0;
            int k = 0;
            while (p > L) {
                p *= RNG.nextDouble();
                k++;
                if (k > 10000)
                    break; // safety
            }
            return k - 1;
        } else {
            double std = Math.sqrt(lambda);
            double val = Math.round(RNG.nextGaussian() * std + lambda);
            return (int) Math.max(0, val);
        }
    }

    // Gamma sampler (Marsaglia & Tsang)
    private static double sampleGamma(double shape, double scale) {
        if (shape <= 0)
            throw new IllegalArgumentException("shape must be > 0");
        if (shape < 1.0) {
            double u = RNG.nextDouble();
            return sampleGamma(shape + 1.0, scale) * Math.pow(u, 1.0 / shape);
        }
        double d = shape - 1.0 / 3.0;
        double c = 1.0 / Math.sqrt(9.0 * d);
        while (true) {
            double x = RNG.nextGaussian();
            double v = 1.0 + c * x;
            if (v <= 0)
                continue;
            v = v * v * v;
            double u = RNG.nextDouble();
            double xsq = x * x;
            if (u < 1.0 - 0.0331 * xsq * xsq)
                return d * v * scale;
            if (Math.log(u) < 0.5 * xsq + d * (1.0 - v + Math.log(v)))
                return d * v * scale;
        }
    }

    // Negative-binomial via Poisson-Gamma mixture (returns integer goals)
    private static int sampleNegBinomial(double mean, double theta) {
        if (theta <= 0)
            return samplePoisson(mean);
        double scale = mean / theta;
        double lamPrime = sampleGamma(theta, scale);
        return samplePoisson(lamPrime);
    }

    // Log-Gamma (Lanczos)
    private static double logGamma(double x) {
        double[] p = {
                0.99999999999980993,
                676.5203681218851,
                -1259.1392167224028,
                771.32342877765313,
                -176.61502916214059,
                12.507343278686905,
                -0.13857109526572012,
                9.9843695780195716e-6,
                1.5056327351493116e-7
        };
        if (x < 0.5) {
            return Math.log(Math.PI) - Math.log(Math.sin(Math.PI * x)) - logGamma(1 - x);
        }
        x -= 1.0;
        double a = p[0];
        double t = x + 7.5;
        for (int i = 1; i < p.length; i++)
            a += p[i] / (x + i);
        return 0.5 * Math.log(2 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(a);
    }

    // Negative-binomial pmf (k = 0..)
    private static double negBinomPmf(int k, double mu, double theta) {
        if (k < 0)
            return 0.0;
        if (theta <= 0)
            return poissonPmf(k, mu);
        double p = theta / (theta + mu);
        double logCoef = logGamma(k + theta) - logGamma(theta) - logGamma(k + 1.0);
        double logPmf = logCoef + k * Math.log(1 - p) + theta * Math.log(p);
        return Math.exp(logPmf);
    }

    private static double poissonPmf(int k, double lambda) {
        if (k < 0)
            return 0.0;
        double logP = -lambda + k * Math.log(lambda) - logGamma(k + 1.0);
        return Math.exp(logP);
    }

    // Build pmf vector for k = 0..Gmax for negative-binomial (tail lumped at Gmax)
    private static double[] negBinomPmfVector(double mu, double theta, int Gmax) {
        double[] pmf = new double[Gmax + 1];
        double sum = 0.0;
        for (int k = 0; k <= Gmax; k++) {
            pmf[k] = negBinomPmf(k, mu, theta);
            sum += pmf[k];
        }
        if (sum < 1.0) {
            pmf[Gmax] += (1.0 - sum);
        }
        return pmf;
    }

    // Sample from pmf vector (defensive normalisation)
    private static int sampleFromPmf(double[] pmf) {
        double s = 0.0;
        for (double v : pmf)
            s += v;
        double u = RNG.nextDouble() * s;
        double c = 0.0;
        for (int i = 0; i < pmf.length; i++) {
            c += pmf[i];
            if (u <= c)
                return i;
        }
        return pmf.length - 1;
    }
}
