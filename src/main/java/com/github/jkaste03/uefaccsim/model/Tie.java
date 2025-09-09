package com.github.jkaste03.uefaccsim.model;

import java.io.Serializable;
import java.util.Random;

import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.service.ClubEloDataLoader;

/**
 * Abstract base for a tie between two club slots. May hold goals.
 */
public abstract class Tie implements Serializable {
    protected final ClubSlot clubSlot1;
    protected final ClubSlot clubSlot2;
    protected Integer club1Goals1stLeg;
    protected Integer club2Goals1stLeg;
    protected Integer club1Goals2ndLeg;
    protected Integer club2Goals2ndLeg;
    /**
     * The tournament this tie is part of. This is needed for qualifying rounds,
     * where the tournament affects seeding in the next round.
     */
    protected final Tournament tournament;

    // ----------------- Play parameters (tunable) -----------------
    // These parameters control the simulation of matches in the tie model.
    protected static final double PARAM_MEAN_GOALS_PER_TEAM = 1.35; // Average goals per team
    protected static final double PARAM_K = 0.50; // Elo to goals scaling factor
    protected static final double PARAM_HFA = 65.0; // Home-field advantage in Elo points
    protected static final double PARAM_THETA = 35.0; // Negative binomial dispersion
    protected static final double[] PARAM_DRAW_INFLATION = { 0.09, 0.05, 0.01 }; // Draw inflation for favorite goals
                                                                                 // 0,1,2
    protected static final int PARAM_GMAX = 7; // Maximum goals explicitly modeled
    protected static final double PARAM_ET_FACTOR = 30.0 / 90.0; // Extra time duration fraction
    // protected static final int PARAM_MC_SIMULATIONS = 5000; // Simulations for
    // probability estimation
    protected static final double PARAM_PENALTY_BETA = 0.18; // Logistic scale for penalty win probability

    protected static final Random RNG = new Random();

    /**
     * Constructs a new Tie representing a pairing between two club slots. Order
     * matters.
     *
     * @param clubSlot1 home participant first leg
     * @param clubSlot2 away participant first leg
     */
    public Tie(ClubSlot clubSlot1, ClubSlot clubSlot2) {
        this.clubSlot1 = clubSlot1;
        this.clubSlot2 = clubSlot2;
        tournament = null;
    }

    /**
     * Constructs a new Tie representing a pairing between two club slots. Order
     * matters. Tournament is important in QRounds.
     *
     * @param clubSlot1  home participant first leg
     * @param clubSlot2  away participant first leg
     * @param tournament tournament
     */
    public Tie(ClubSlot clubSlot1, ClubSlot clubSlot2, Tournament tournament) {
        this.clubSlot1 = clubSlot1;
        this.clubSlot2 = clubSlot2;
        this.tournament = tournament;
    }

    /**
     * Constructs a new Tie representing a pairing between two club slots with
     * preset goals. Order matters. Tournament is important in QRounds.
     *
     * @param clubSlot1        home participant first leg
     * @param clubSlot2        away participant first leg
     * @param club1Goals1stLeg first leg goals for club 1
     * @param club2Goals1stLeg first leg goals for club 2
     * @param tournament       tournament
     */
    public Tie(ClubSlot clubSlot1, ClubSlot clubSlot2, Integer club1Goals1stLeg, Integer club2Goals1stLeg,
            Tournament tournament) {
        this.clubSlot1 = clubSlot1;
        this.clubSlot2 = clubSlot2;
        this.club1Goals1stLeg = club1Goals1stLeg;
        this.club2Goals1stLeg = club2Goals1stLeg;
        this.tournament = tournament;
    }

    public ClubSlot getClubSlot1() {
        return clubSlot1;
    }

    public ClubSlot getClubSlot2() {
        return clubSlot2;
    }

    public Integer getClub1Goals1stLeg() {
        return club1Goals1stLeg;
    }

    public Integer getClub2Goals1stLeg() {
        return club2Goals1stLeg;
    }

    /**
     * Returns the total goals scored by club 1 across both potential legs
     * 
     * @return total goals for club 1
     */
    protected int getClub1Goals() {
        return (club1Goals1stLeg == null ? 0 : club1Goals1stLeg) + (club1Goals2ndLeg == null ? 0 : club1Goals2ndLeg);
    }

    /**
     * Returns the total goals scored by club 2 across both potential legs
     * 
     * @return total goals for club 2
     */
    protected int getClub2Goals() {
        return (club2Goals1stLeg == null ? 0 : club2Goals1stLeg) + (club2Goals2ndLeg == null ? 0 : club2Goals2ndLeg);
    }

    public Tournament getTournament() {
        return tournament;
    }

    /**
     * Computes this tie's effective ranking relative to a caller tournament by
     * selecting one of the two underlying club rankings. If the caller tournament
     * is at a worse level than this tie's tournament, the worst ranking is
     * returned; otherwise, the best ranking is returned. Caller tournament may be
     * null, because non-knockout ties may not need a tournament.
     *
     * @param callerTournament the tournament context requesting the ranking
     * @return the ranking
     */
    public float getRanking(Tournament callerTournament) {
        float r1 = clubSlot1.getRanking(tournament), r2 = clubSlot2.getRanking(tournament);
        return ((tournament.compareTo(callerTournament == null ? tournament : callerTournament) > 0) ^ (r1 < r2)) ? r1
                : r2;
    }

    public void incrementSeedingCounter(boolean isSeeded) {
        clubSlot1.incrementSeedingCounter(isSeeded);
        clubSlot2.incrementSeedingCounter(isSeeded);
    }

    /**
     * Simulates the tie.
     * <p>
     * Implementing methods should play the tie, update the results,
     * and set the winner based on the tie outcome if it's a knockout tie.
     */
    public abstract void play(ClubEloDataLoader clubEloDataLoader);

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
    protected void simulateMatch(double eloHome, double eloAway, boolean firstLeg) {
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
        if (firstLeg) {
            club1Goals1stLeg = homeGoals;
            club2Goals1stLeg = awayGoals;
        } else {
            club1Goals2ndLeg = awayGoals;
            club2Goals2ndLeg = homeGoals;
        }
    }

    // simulate extra-time (shorter period). For ET in the second leg use
    // (eloHome=secondLegHome, eloAway=secondLegAway).
    protected void simulateExtraTime(double eloHome, double eloAway) {
        double[] lambdas = eloToLambdas(eloHome, eloAway);
        double lHome = lambdas[0] * PARAM_ET_FACTOR;
        double lAway = lambdas[1] * PARAM_ET_FACTOR;
        club2Goals2ndLeg += sampleNegBinomial(lHome, PARAM_THETA);
        club1Goals2ndLeg += sampleNegBinomial(lAway, PARAM_THETA);
    }

    // simulate penalty winner based on Elo diff (logistic)
    protected static boolean simulatePenaltyWinner(double eloHome, double eloAway) {
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

    /**
     * Returns a compact "Club1 vs Club2" string for the tie.
     * 
     * @return compact string representation of the tie
     */
    public String toCompactString() {
        return clubSlot1.toCompactString() + " vs " +
                clubSlot2.toCompactString();
    }

    @Override
    public String toString() {
        return "Tie[" + fieldsToString() + "]";
    }

    /**
     * Returns a concise, comma-separated textual representation of this tie's key
     * fields. Used by all Tie subclasses in their toString implementations.
     *
     * @return a human-readable summary of this tie's state
     */
    protected String fieldsToString() {
        return "clubSlot1=" + clubSlot1 +
                ", clubSlot2=" + clubSlot2 +
                ", club1Goals1stLeg=" + club1Goals1stLeg +
                ", club2Goals1stLeg=" + club2Goals1stLeg +
                ", club1Goals2ndLeg=" + club1Goals2ndLeg +
                ", club2Goals2ndLeg=" + club2Goals2ndLeg +
                ", tournament=" + tournament;
    }
}
