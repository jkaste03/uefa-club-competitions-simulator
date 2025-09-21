package com.github.jkaste03.uefaccsim.service.match;

import java.util.concurrent.ThreadLocalRandom;

import com.github.jkaste03.uefaccsim.config.SimulationConfig;
import com.github.jkaste03.uefaccsim.model.competition.MatchResult;

/**
 * Default implementation of MatchSimulator that models football scores with
 * an overdispersed goal process derived from Elo ratings.
 *
 * <p>
 * Model summary:
 * <ul>
 * <li>Expected goals (lambda) for each team are computed from Elo difference
 * using an exponential mapping around a global mean and an optional home-field
 * advantage.</li>
 * <li>Goals are sampled from a Negative Binomial distribution (as a
 * Poisson–Gamma
 * mixture) to capture overdispersion compared to a Poisson process.</li>
 * <li>Optionally inflates draw probabilities for exact scorelines 0–0, 1–1, and
 * 2–2
 * via configurable multipliers.</li>
 * <li>Scores are capped at a configurable maximum gmax; any residual
 * probability mass
 * is absorbed into the cap bin and distributions are defensively
 * renormalized.</li>
 * <li>Extra time is modeled by scaling both teams’ expected goals by a
 * configurable factor.</li>
 * </ul>
 */
public final class DefaultMatchSimulator implements MatchSimulator {

    /** Thread-local Gaussian cache for Box–Muller transform */
    private static final ThreadLocal<GaussianCache> GAUSS_CACHE = ThreadLocal.withInitial(GaussianCache::new);

    /**
     * Cache used by the Gaussian (normal) random value generator to store a spare
     * sample produced by transforms such as Box–Muller. This avoids recomputation
     * by reusing the second value on the next request.
     */
    private static final class GaussianCache {
        boolean has;
        double spare;
    }

    @Override
    public MatchResult simulate(double eloHome, double eloAway, boolean inExtraTime, SimulationConfig cfg) {
        double dr = eloHome - eloAway + cfg.homeFieldAdvantage();
        double lambdaHome = cfg.meanGoalsPerTeam() * Math.exp(cfg.eloToGoalsK() * dr / 400.0);
        double lambdaAway = cfg.meanGoalsPerTeam() * Math.exp(cfg.eloToGoalsK() * (-dr) / 400.0);
        if (inExtraTime) {
            lambdaHome *= cfg.extraTimeFactor();
            lambdaAway *= cfg.extraTimeFactor();
        }

        boolean favIsHome = lambdaHome >= lambdaAway;
        double lambdaFav = favIsHome ? lambdaHome : lambdaAway;
        double lambdaUnd = favIsHome ? lambdaAway : lambdaHome;

        int gFav = sampleNegBinomial(lambdaFav, cfg.theta());
        if (gFav > cfg.gmax())
            gFav = cfg.gmax();
        double[] pmfUnd = negBinomPmfVector(lambdaUnd, cfg.theta(), cfg.gmax());
        if (gFav <= 2) {
            double factor = 1.0 + cfg.drawInflation()[gFav];
            pmfUnd[gFav] *= factor;
            // Renormalize defensively
            double s = 0.0;
            for (double v : pmfUnd)
                s += v;
            if (s > 0) {
                for (int i = 0; i < pmfUnd.length; i++)
                    pmfUnd[i] /= s;
            }
        }
        int gUnd = sampleFromPmf(pmfUnd);

        int homeGoals = favIsHome ? gFav : gUnd;
        int awayGoals = favIsHome ? gUnd : gFav;
        return new MatchResult(homeGoals, awayGoals, inExtraTime);
    }

    // ---------- Probability primitives (minimal copies) ----------

    private static int sampleFromPmf(double[] pmf) {
        double u = ThreadLocalRandom.current().nextDouble();
        double acc = 0.0;
        for (int k = 0; k < pmf.length; k++) {
            acc += pmf[k];
            if (u <= acc)
                return k;
        }
        return pmf.length - 1; // defensive tail
    }

    private static double[] negBinomPmfVector(double mu, double theta, int Gmax) {
        double[] pmf = new double[Gmax + 1];
        double sum = 0.0;
        for (int k = 0; k < Gmax; k++) {
            double p = negBinomPmf(k, mu, theta);
            pmf[k] = p;
            sum += p;
        }
        pmf[Gmax] = Math.max(0.0, 1.0 - sum);
        // Defensive normalize
        double s = 0.0;
        for (double v : pmf)
            s += v;
        if (s > 0) {
            for (int i = 0; i < pmf.length; i++)
                pmf[i] /= s;
        }
        return pmf;
    }

    private static double negBinomPmf(int k, double mu, double theta) {
        // NB as Poisson-Gamma mixture leads to pmf using gamma functions
        double r = theta;
        double p = r / (r + mu);
        return Math.exp(logGamma(k + r) - logGamma(r) - logGamma(k + 1)
                + r * Math.log(p) + k * Math.log(1 - p));
    }

    private static double logGamma(double x) {
        // Lanczos approximation (simple 7-term)
        double[] c = {
                676.5203681218851,
                -1259.1392167224028,
                771.32342877765313,
                -176.61502916214059,
                12.507343278686905,
                -0.13857109526572012,
                9.9843695780195716e-6,
                1.5056327351493116e-7
        };
        int g = 7;
        if (x < 0.5) {
            return Math.log(Math.PI) - Math.log(Math.sin(Math.PI * x)) - logGamma(1 - x);
        }
        x -= 1;
        double a = 0.99999999999980993;
        for (int i = 0; i < c.length; i++)
            a += c[i] / (x + i + 1);
        double t = x + g + 0.5;
        return 0.5 * Math.log(2 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(a);
    }

    private static int sampleNegBinomial(double mean, double theta) {
        // Poisson-Gamma mixture
        double shape = theta;
        double scale = mean / theta;
        double lambda = sampleGamma(shape, scale);
        return samplePoisson(lambda);
    }

    private static double sampleGamma(double shape, double scale) {
        // Marsaglia and Tsang's method (k > 0)
        if (shape < 1) {
            // Johnk's generator via boosting
            double u = ThreadLocalRandom.current().nextDouble();
            return sampleGamma(1 + shape, scale) * Math.pow(u, 1.0 / shape);
        }
        double d = shape - 1.0 / 3.0;
        double c = 1.0 / Math.sqrt(9.0 * d);
        while (true) {
            double x, v;
            do {
                x = nextGaussianBM();
                v = 1.0 + c * x;
            } while (v <= 0);
            v = v * v * v;
            double u = ThreadLocalRandom.current().nextDouble();
            if (u < 1 - 0.0331 * x * x * x * x)
                return scale * d * v;
            if (Math.log(u) < 0.5 * x * x + d * (1 - v + Math.log(v)))
                return scale * d * v;
        }
    }

    private static double nextGaussianBM() {
        GaussianCache gc = GAUSS_CACHE.get();
        if (gc.has) {
            gc.has = false;
            return gc.spare;
        }
        double u1, u2;
        do {
            u1 = ThreadLocalRandom.current().nextDouble();
        } while (u1 <= 1e-12);
        u2 = ThreadLocalRandom.current().nextDouble();
        double r = Math.sqrt(-2.0 * Math.log(u1));
        double theta = 2.0 * Math.PI * u2;
        double z0 = r * Math.cos(theta);
        double z1 = r * Math.sin(theta);
        gc.spare = z1;
        gc.has = true;
        return z0;
    }

    private static int samplePoisson(double lambda) {
        if (lambda <= 0)
            return 0;
        if (lambda < 30) {
            // Knuth
            double L = Math.exp(-lambda);
            int k = 0;
            double p = 1.0;
            do {
                k++;
                p *= ThreadLocalRandom.current().nextDouble();
            } while (p > L);
            return k - 1;
        } else {
            // Normal approximation with rounding and non-neg clamp
            double x = lambda + Math.sqrt(lambda) * nextGaussianBM();
            return (int) Math.max(0, Math.round(x));
        }
    }
}
