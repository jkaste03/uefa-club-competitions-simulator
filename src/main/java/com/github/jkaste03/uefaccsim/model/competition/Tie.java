package com.github.jkaste03.uefaccsim.model.competition;

import java.io.Serializable;
import java.util.Random;

import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.service.ClubEloDataLoader;

/**
 * Abstract base for a tie between two club slots. May hold goals.
 */
public abstract class Tie implements Serializable {
    protected final ClubSlot clubSlotA;
    protected final ClubSlot clubSlotB;
    protected Integer clubAGoals1stLeg;
    protected Integer clubBGoals1stLeg;
    protected Integer clubAGoals2ndLeg;
    protected Integer clubBGoals2ndLeg;
    /**
     * The tournament this tie is part of. This is needed for qualifying rounds,
     * where the tournament affects seeding in the next round.
     */
    protected final Tournament tournament;

    // ----------------- Play parameters (tunable) -----------------
    // These parameters control the simulation of matches in the tie model.
    private static final double PARAM_MEAN_GOALS_PER_TEAM = 1.35; // Average goals per team
    private static final double PARAM_ELO_TO_GOALS_K = 0.50; // Elo to goals scaling factor
    protected static final double PARAM_ELO_UPDATE_K = 30.0; // Elo K-factor for Elo updates
    private static final double PARAM_HFA = 65.0; // Home-field advantage in Elo points
    private static final double PARAM_THETA = 35.0; // Negative binomial dispersion
    private static final double[] PARAM_DRAW_INFLATION = { 0.09, 0.05, 0.01 }; // Draw inflation for favorite goals
                                                                               // 0,1,2
    private static final int PARAM_GMAX = 7; // Maximum goals explicitly modeled
    protected static final double PARAM_ET_FACTOR = 30.0 / 90.0; // Extra time duration fraction
    protected static final double PARAM_PSO_FACTOR = 10.0 / 90.0; // Penalty shootout fraction (for Elo update only)
    // private static final int PARAM_MC_SIMULATIONS = 5000; // Simulations for
    // probability estimation
    private static final double PARAM_PENALTY_BETA = 0.18; // Logistic scale for penalty win probability

    private static final Random RNG = new Random();

    /**
     * Constructs a new Tie representing a pairing between two club slots. Order
     * matters.
     *
     * @param clubSlotA home participant first leg
     * @param clubSlotB away participant first leg
     */
    public Tie(ClubSlot clubSlotA, ClubSlot clubSlotB) {
        this.clubSlotA = clubSlotA;
        this.clubSlotB = clubSlotB;
        tournament = null;
    }

    /**
     * Constructs a new Tie representing a pairing between two club slots. Order
     * matters. Tournament is important in QRounds.
     *
     * @param clubSlotA  home participant first leg
     * @param clubSlotB  away participant first leg
     * @param tournament tournament
     */
    public Tie(ClubSlot clubSlotA, ClubSlot clubSlotB, Tournament tournament) {
        this.clubSlotA = clubSlotA;
        this.clubSlotB = clubSlotB;
        this.tournament = tournament;
    }

    /**
     * Constructs a new Tie representing a pairing between two club slots with
     * preset goals. Order matters. Tournament is important in QRounds.
     *
     * @param clubSlotA        home participant first leg
     * @param clubSlotB        away participant first leg
     * @param clubAGoals1stLeg first leg goals for club A
     * @param clubBGoals1stLeg first leg goals for club B
     * @param tournament       tournament
     */
    public Tie(ClubSlot clubSlotA, ClubSlot clubSlotB, Integer clubAGoals1stLeg, Integer clubBGoals1stLeg,
            Tournament tournament) {
        this.clubSlotA = clubSlotA;
        this.clubSlotB = clubSlotB;
        this.clubAGoals1stLeg = clubAGoals1stLeg;
        this.clubBGoals1stLeg = clubBGoals1stLeg;
        this.tournament = tournament;
    }

    public ClubSlot getClubSlotA() {
        return clubSlotA;
    }

    public ClubSlot getClubSlotB() {
        return clubSlotB;
    }

    public Integer getClubAGoals1stLeg() {
        return clubAGoals1stLeg;
    }

    public Integer getClubBGoals1stLeg() {
        return clubBGoals1stLeg;
    }

    /**
     * Returns the total goals scored by club A across both potential legs
     * 
     * @return total goals for club A
     */
    protected int getClubAGoals() {
        return (clubAGoals1stLeg == null ? 0 : clubAGoals1stLeg) + (clubAGoals2ndLeg == null ? 0 : clubAGoals2ndLeg);
    }

    /**
     * Returns the total goals scored by club B across both potential legs
     * 
     * @return total goals for club B
     */
    protected int getClubBGoals() {
        return (clubBGoals1stLeg == null ? 0 : clubBGoals1stLeg) + (clubBGoals2ndLeg == null ? 0 : clubBGoals2ndLeg);
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
        float rA = clubSlotA.getRanking(tournament), rB = clubSlotB.getRanking(tournament);
        return ((tournament.compareTo(callerTournament == null ? tournament : callerTournament) > 0) ^ (rA < rB)) ? rA
                : rB;
    }

    public void incrementSeedingCounter(boolean isSeeded) {
        clubSlotA.incrementSeedingCounter(isSeeded);
        clubSlotB.incrementSeedingCounter(isSeeded);
    }

    /**
     * Simulates the tie.
     * <p>
     * Implementing methods should play the tie, update the results,
     * and set the winner based on the tie outcome if it's a knockout tie.
     */
    public abstract void play(ClubEloDataLoader clubEloDataLoader);

    /**
     * Calculates the expected number of goals (lambda) for the home team using a
     * Poisson model. The calculation is based on the mean goals per team and an
     * adjustment factor derived from the Elo rating difference.
     *
     * @param PARAM_MEAN_GOALS_PER_TEAM The average number of goals scored per team.
     * @param PARAM_ELO_TO_GOALS_K      The scaling factor that converts Elo rating
     *                                  difference to goal expectation.
     * @param dr                        The Elo rating difference between the home
     *                                  and away teams.
     * @return The expected number of goals for the home team.
     */
    private static double[] eloToLambdas(double eloHome, double eloAway) {
        double dr = eloHome - eloAway + PARAM_HFA; // add home advantage to home team's Elo
        double lambdaHome = PARAM_MEAN_GOALS_PER_TEAM * Math.exp(PARAM_ELO_TO_GOALS_K * dr / 400.0);
        double lambdaAway = PARAM_MEAN_GOALS_PER_TEAM * Math.exp(PARAM_ELO_TO_GOALS_K * (-dr) / 400.0);
        return new double[] { lambdaHome, lambdaAway };
    }

    /**
     * Simulates a football match between two clubs, updating the goals for each
     * leg.
     *
     * <p>
     * The simulation uses club Elo ratings to determine expected goals (lambdas)
     * for home and away teams, adjusts for extra time (ET) if applicable, and
     * samples goals using a negative binomial distribution. Draw inflation is
     * applied for low goal counts to increase the probability of draws.
     *
     * @param firstLeg          true if simulating the first leg of the tie; false
     *                          for the second leg
     * @param ET                true if the match is in extra time; false otherwise
     * @param clubEloDataLoader loader for retrieving club Elo ratings
     */
    protected void simulateMatch(boolean firstLeg, boolean ET, ClubEloDataLoader clubEloDataLoader) {
        double eloA = clubEloDataLoader.getElo(clubSlotA.getClubIdWrapper().id());
        double eloB = clubEloDataLoader.getElo(clubSlotB.getClubIdWrapper().id());

        // Get lambdas for home/away
        double[] lambdas = firstLeg ? eloToLambdas(eloA, eloB) : eloToLambdas(eloB, eloA);
        // Adjust for ET if needed
        double lHome = ET ? lambdas[0] * PARAM_ET_FACTOR : lambdas[0];
        double lAway = ET ? lambdas[1] * PARAM_ET_FACTOR : lambdas[1];

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
            clubAGoals1stLeg = (clubAGoals1stLeg == null ? homeGoals : clubAGoals1stLeg + homeGoals);
            clubBGoals1stLeg = (clubBGoals1stLeg == null ? awayGoals : clubBGoals1stLeg + awayGoals);
        } else {
            clubAGoals2ndLeg = (clubAGoals2ndLeg == null ? awayGoals : clubAGoals2ndLeg + awayGoals);
            clubBGoals2ndLeg = (clubBGoals2ndLeg == null ? homeGoals : clubBGoals2ndLeg + homeGoals);
        }
    }

    // protected void simulateExtraTime(boolean firstLeg, ClubEloDataLoader
    // clubEloDataLoader) {
    // simulateExtraTime(firstLeg, clubEloDataLoader, PARAM_ET_FACTOR);
    // double eloA = clubEloDataLoader.getElo(clubSlotA.getClubIdWrapper().id());
    // double eloB = clubEloDataLoader.getElo(clubSlotB.getClubIdWrapper().id());

    // if (firstLeg) {
    // double[] lambdas = eloToLambdas(eloA, eloB);
    // double lHome = lambdas[0] * PARAM_ET_FACTOR;
    // double lAway = lambdas[1] * PARAM_ET_FACTOR;
    // clubAGoals1stLeg += sampleNegBinomial(lHome, PARAM_THETA);
    // clubBGoals1stLeg += sampleNegBinomial(lAway, PARAM_THETA);
    // } else {
    // double[] lambdas = eloToLambdas(eloB, eloA);
    // double lHome = lambdas[0] * PARAM_ET_FACTOR;
    // double lAway = lambdas[1] * PARAM_ET_FACTOR;
    // clubAGoals2ndLeg += sampleNegBinomial(lAway, PARAM_THETA);
    // clubBGoals2ndLeg += sampleNegBinomial(lHome, PARAM_THETA);
    // }
    // }

    /**
     * Simulates the outcome of a penalty shootout between two clubs based on their
     * Elo ratings.
     *
     * @param firstLeg          Indicates if the simulation is for the first leg
     *                          (true) or second leg (false) of the tie.
     * @param clubEloDataLoader Loader providing Elo ratings for the clubs involved.
     * @return {@code true} if the home club (determined by {@code firstLeg}) wins
     *         the penalty shootout; {@code false} otherwise.
     */
    protected boolean simulatePenaltyWinner(boolean firstLeg, ClubEloDataLoader clubEloDataLoader) {
        double eloA = clubEloDataLoader.getElo(clubSlotA.getClubIdWrapper().id());
        double eloB = clubEloDataLoader.getElo(clubSlotB.getClubIdWrapper().id());

        double dr = firstLeg ? eloA - eloB : eloB - eloA;
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

    // --- Elo helper functions ---
    private static double expectedScore(double eloA, double eloB) {
        return 1.0 / (1.0 + Math.pow(10.0, (eloB - eloA) / 400.0));
    }

    /**
     * Margin multiplier G:
     * G = ln(goalDiff + 1) * (2.2 / (0.001 * |eloDiff| + 2.2))
     */
    private static double marginMultiplier(int goalsA, int goalsB, double eloA, double eloB) {
        int goalDiff = Math.abs(goalsA - goalsB);
        if (goalDiff == 0)
            return 1.0; // ingen ekstra effekt ved uavgjort
        double logPart = Math.log(goalDiff + 1.0); // naturlig log
        double denom = (Math.abs(eloA - eloB) * 0.001) + 2.2;
        return logPart * (2.2 / denom);
    }

    /**
     * Computes the Elo rating delta for a match between two teams based on their
     * Elo ratings, the number of goals scored by each team, and a scaling factor K.
     * 
     * The calculation considers home field advantage (HFA), the match result (win,
     * draw, loss), the expected score based on Elo ratings, and a margin multiplier
     * based on the goal difference.
     * 
     * @param eloA   The Elo rating of team A (home team).
     * @param eloB   The Elo rating of team B (away team).
     * @param goalsA The number of goals scored by team A.
     * @param goalsB The number of goals scored by team B.
     * @param K      The scaling factor for Elo adjustment.
     * @return The computed Elo rating delta for team A.
     */
    protected static double computeEloDelta(double eloA, double eloB, int goalsA, int goalsB, double K) {
        // adjust Elo with HFA
        double eloAeff = eloA + PARAM_HFA;
        double S;
        if (goalsA > goalsB)
            S = 1.0;
        else if (goalsA == goalsB)
            S = 0.5;
        else
            S = 0.0;

        double E = expectedScore(eloAeff, eloB);
        double G = marginMultiplier(goalsA, goalsB, eloAeff, eloB);
        System.out.println(
                "Elo delta for " + eloA + " vs " + eloB + " (" + goalsA + "-" + goalsB + "): " + (K * G * (S - E)));
        return K * G * (S - E);
    }

    /**
     * Updates the Elo ratings for two clubs based on the result of a match leg.
     *
     * <p>
     * This method calculates the Elo rating change (delta) for both clubs involved
     * in the tie, considering whether the match is the first or second leg. The Elo
     * change is computed using the provided K-factor and the goals scored by each
     * club. The computed delta is then applied to both clubs using the
     * {@link ClubEloDataLoader} without committing the changes immediately.
     *
     * @param goalsA            the number of goals scored by club A
     * @param goalsB            the number of goals scored by club B
     * @param firstLeg          {@code true} if this is the first leg of the tie;
     *                          {@code false} otherwise
     * @param k                 the K-factor used in Elo calculation
     * @param clubEloDataLoader the loader responsible for retrieving and updating
     *                          club Elo ratings
     */
    protected void updateEloForResult(int goalsA, int goalsB, boolean firstLeg,
            double k, ClubEloDataLoader clubEloDataLoader) {
        double eloA = clubEloDataLoader.getElo(clubSlotA.getClubIdWrapper().id());
        double eloB = clubEloDataLoader.getElo(clubSlotB.getClubIdWrapper().id());

        double deltaElo = firstLeg ? computeEloDelta(eloA, eloB, goalsA, goalsB, k)
                : computeEloDelta(eloB, eloA, goalsB, goalsA, k);
        clubEloDataLoader.updateUncommitedEloDelta(clubSlotA.getClubIdWrapper().id(), deltaElo);
        clubEloDataLoader.updateUncommitedEloDelta(clubSlotB.getClubIdWrapper().id(), -deltaElo);
    }

    /**
     * Returns a compact "ClubA vs ClubB" string for the tie.
     * 
     * @return compact string representation of the tie
     */
    public String toCompactString() {
        return clubSlotA.toCompactString() + " vs " +
                clubSlotB.toCompactString();
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
        return "clubSlotA=" + clubSlotA +
                ", clubSlotB=" + clubSlotB +
                ", clubAGoals1stLeg=" + clubAGoals1stLeg +
                ", clubBGoals1stLeg=" + clubBGoals1stLeg +
                ", clubAGoals2ndLeg=" + clubAGoals2ndLeg +
                ", clubBGoals2ndLeg=" + clubBGoals2ndLeg +
                ", tournament=" + tournament;
    }
}
