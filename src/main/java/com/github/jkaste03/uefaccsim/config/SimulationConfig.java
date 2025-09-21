package com.github.jkaste03.uefaccsim.config;

/**
 * Centralized match simulation parameters. Intentionally simple and immutable.
 */
public record SimulationConfig(
        // Average goals per team per 90 minutes
        double meanGoalsPerTeam,
        // Elo to goals scaling factor
        double eloToGoalsK,
        // Elo K-factor for Elo updates
        double eloUpdateK,
        // Home-field advantage in Elo points
        double homeFieldAdvantage,
        // Negative binomial dispersion (theta)
        double theta,
        // Draw inflation applied when favourite scores 0,1,2 (mass added to equal-score
        // cell)
        double[] drawInflation,
        // Maximum goals explicitly modeled in PMF vectors (tail is lumped at Gmax)
        int gmax,
        // Extra time duration fraction (e.g., 30/90)
        double extraTimeFactor,
        // Penalty shootout factor (for Elo update only)
        double penaltyShootoutFactor,
        // Logistic scale for penalty win probability
        double penaltyBeta) {

    /**
     * Initializes the configuration while preserving immutability of its internal
     * state. Performs a defensive copy of the draw inflation array so that external
     * modifications to the original input do not affect this instance.
     */
    public SimulationConfig {
        drawInflation = drawInflation.clone();
    }

    /**
     * Default SimulationConfig used by the UEFA club competitions simulator.
     *
     * Calibrated baseline parameters controlling goal intensity, Elo effects,
     * home advantage, dispersion, draw bias, and tie-break behavior.
     */
    private static final SimulationConfig DEFAULT = new SimulationConfig(
            1.35, // meanGoalsPerTeam
            0.50, // eloToGoalsK
            30.0, // eloUpdateK
            65.0, // homeFieldAdvantage
            35.0, // theta
            new double[] { 0.09, 0.05, 0.01 }, // drawInflation for fav goals = 0,1,2
            7, // gmax
            30.0 / 90.0, // extraTimeFactor
            10.0 / 90.0, // penaltyShootoutFactor
            0.18 // penaltyBeta
    );

    public static SimulationConfig getDefault() {
        return DEFAULT;
    }
}
