package com.github.jkaste03.uefaccsim.service.match;

import com.github.jkaste03.uefaccsim.config.SimulationConfig;
import com.github.jkaste03.uefaccsim.model.competition.MatchResult;

/**
 * Responsible for simulating a single match (leg) or extra time outcome given
 * home/away strengths.
 */
public interface MatchSimulator {
    /**
     * Simulate a single match (leg) or extra time.
     * 
     * @param eloHome     Elo for home team
     * @param eloAway     Elo for away team
     * @param inExtraTime whether this is extra time (lambdas scaled accordingly)
     * @param config      parameters controlling the simulation
     * @return immutable MatchResult
     */
    MatchResult simulate(double eloHome, double eloAway, boolean inExtraTime, SimulationConfig config);
}
