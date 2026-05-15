package com.github.jkaste03.uefaccsim.model.competition;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.repository.ClubSimStateRepository;
import com.github.jkaste03.uefaccsim.config.SimulationConfig;
import com.github.jkaste03.uefaccsim.service.match.DefaultMatchSimulator;
import com.github.jkaste03.uefaccsim.service.match.MatchSimulator;

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
    protected static final double PARAM_ELO_UPDATE_K = 30.0; // Elo K-factor for
    // Elo updates
    private static final double PARAM_HFA = 65.0; // Home-field advantage in Elo points
    protected static final double PARAM_ET_FACTOR = 30.0 / 90.0; // Extra time duration fraction
    protected static final double PARAM_PSO_FACTOR = 10.0 / 90.0; // Penalty
    // shootout fraction (for Elo update only)
    private static final double PARAM_PENALTY_BETA = 0.18; // Logistic scale for penalty win probability

    /**
     * Constructs a new Tie representing a pairing between two club slots. Order
     * matters.
     *
     * @param clubSlotA home participant first leg
     * @param clubSlotB away participant first leg
     */
    protected Tie(ClubSlot clubSlotA, ClubSlot clubSlotB) {
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
    protected Tie(ClubSlot clubSlotA, ClubSlot clubSlotB, Tournament tournament) {
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
    protected Tie(ClubSlot clubSlotA, ClubSlot clubSlotB, Integer clubAGoals1stLeg, Integer clubBGoals1stLeg,
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

    public Integer getClubAGoals2ndLeg() {
        return clubAGoals2ndLeg;
    }

    public Integer getClubBGoals2ndLeg() {
        return clubBGoals2ndLeg;
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

    /**
     * Simulates the tie.
     * <p>
     * Implementing methods should play the next leg of the tie, update the results,
     * update the Elo ratings, and if applicable, set the winner based on the tie
     * outcome.
     */
    public abstract void play(ClubSimStateRepository clubSimStateRepo);

    /**
     * Simulates a leg for this tie and writes the resulting goals into the
     * appropriate leg fields.
     * <p>
     * The simulation is performed using a {@link MatchSimulator} (default:
     * {@link DefaultMatchSimulator}) with
     * {@link SimulationConfig#getDefault()}.
     * <p>
     * If the leg results in extra time, the method needs to be called again to
     * account for the additional goals scored during that period.
     *
     * @param firstLeg whether to simulate the first leg or the
     *                 second leg
     * @param ET       whether to simulate extra time
     * @see MatchSimulator
     * @see DefaultMatchSimulator
     * @see SimulationConfig
     * @see MatchResult
     */
    protected void simulateMatch(boolean firstLeg, boolean ET) {
        double eloA = clubSlotA.getClubSimState().getElo();
        double eloB = clubSlotB.getClubSimState().getElo();
        double eloHome = firstLeg ? eloA : eloB;
        double eloAway = firstLeg ? eloB : eloA;

        MatchSimulator simulator = new DefaultMatchSimulator();
        SimulationConfig cfg = SimulationConfig.getDefault();
        MatchResult mr = simulator.simulate(eloHome, eloAway, ET, cfg);

        int homeGoals = mr.homeGoals();
        int awayGoals = mr.awayGoals();
        // Update goals for the first or second leg
        if (firstLeg) {
            clubAGoals1stLeg = (mr.inExtraTime() ? clubAGoals1stLeg + homeGoals : homeGoals);
            clubBGoals1stLeg = (mr.inExtraTime() ? clubBGoals1stLeg + awayGoals : awayGoals);
        } else {
            clubAGoals2ndLeg = (mr.inExtraTime() ? clubAGoals2ndLeg + awayGoals : awayGoals);
            clubBGoals2ndLeg = (mr.inExtraTime() ? clubBGoals2ndLeg + homeGoals : homeGoals);
        }
    }

    // protected void simulateExtraTime(boolean firstLeg, ClubEloRatingsInitializer
    // clubEloRatingsInitializer) {
    // simulateExtraTime(firstLeg, clubEloRatingsInitializer, PARAM_ET_FACTOR);
    // double eloA = clubSlotA.getClubSimState().getElo();
    // double eloB = clubSlotB.getClubSimState().getElo();

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
     * @param firstLeg Indicates if the simulation is for the first leg or second
     *                 leg of the tie.
     */
    protected boolean simulatePenaltyWinner(boolean firstLeg) {
        double eloA = clubSlotA.getClubSimState().getElo();
        double eloB = clubSlotB.getClubSimState().getElo();

        double dr = firstLeg ? eloA - eloB : eloB - eloA;
        double z = PARAM_PENALTY_BETA * dr / 400.0;
        double pHome = 1.0 / (1.0 + Math.exp(-z));
        return ThreadLocalRandom.current().nextDouble() < pHome;
    }

    // (Probability primitives removed from Tie.)

    /**
     * Calculates the expected score for a team (A) against another team (B) based
     * on their Elo ratings. The expected score represents the probability that team
     * A will win or draw against team B.
     *
     * @param eloA the Elo rating of team A
     * @param eloB the Elo rating of team B
     * @return the expected score (probability) for team A, ranging from 0.0 to 1.0
     */
    private static double expectedScore(double eloA, double eloB) {
        return 1.0 / (1.0 + Math.pow(10.0, (eloB - eloA) / 400.0));
    }

    /**
     * Calculates a multiplier based on the goal margin and Elo rating difference
     * between two teams.
     * <p>
     * The multiplier increases with the absolute goal difference and decreases with
     * the Elo rating difference. If the match is a draw (goal difference is zero),
     * the multiplier is 1.0. Otherwise, it uses the natural logarithm of (goal
     * difference + 1) scaled by a factor that depends on the Elo ratings.
     *
     * @param goalsA the number of goals scored by team A
     * @param goalsB the number of goals scored by team B
     * @param eloA   the Elo rating of team A
     * @param eloB   the Elo rating of team B
     * @return a double value representing the margin multiplier
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
        // System.out.println(
        // "Elo delta for " + eloA + " vs " + eloB + " (" + goalsA + "-" + goalsB + "):
        // " + (K * G * (S - E)));
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
     * {@link ClubSimStateRepository} without committing the changes immediately.
     *
     * @param goalsA                 the number of goals scored by club A
     * @param goalsB                 the number of goals scored by club B
     * @param firstLeg               {@code true} if this is the first leg of the
     *                               tie;
     *                               {@code false} otherwise
     * @param k                      the K-factor used in Elo calculation
     * @param clubSimStateRepository the repository used to read and update club
     *                               states
     */
    protected void updateEloForResult(int goalsA, int goalsB, boolean firstLeg,
            double k, ClubSimStateRepository clubSimStateRepo) {
        // Retrieve Elo ratings
        double eloA = clubSlotA.getClubSimState().getElo();
        double eloB = clubSlotB.getClubSimState().getElo();

        // Compute Elo delta for home club. Away club gets -delta.
        double deltaElo = firstLeg
                ? computeEloDelta(eloA, eloB, goalsA, goalsB, k)
                : computeEloDelta(eloB, eloA, goalsB, goalsA, k);
        // Update Elo without committing
        clubSimStateRepo.updateUncommittedEloDelta(clubSlotA.getClubSimState().getId(),
                firstLeg ? deltaElo : -deltaElo);
        clubSimStateRepo.updateUncommittedEloDelta(clubSlotB.getClubSimState().getId(),
                firstLeg ? -deltaElo : deltaElo);
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
