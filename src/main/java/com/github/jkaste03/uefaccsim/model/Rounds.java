package com.github.jkaste03.uefaccsim.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.jkaste03.uefaccsim.enums.PathType;
import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.service.ClubEloDataLoader;
import com.github.jkaste03.uefaccsim.service.JsonDataLoader;

import java.util.EnumMap;

/**
 * Central orchestrator for all UEFA competition rounds (Champions League,
 * Europa League, Conference League) from the first qualifying round to the
 * final.
 */
public class Rounds implements Serializable {

    // Declare qualifying rounds and league rounds for all competitions.
    private final QRound uclQ1CP, uclQ2CP, uclQ2LP, uclQ3CP, uclQ3LP, uclPoCP, uclPoLP;
    private final QRound uelQ1MP, uelQ2MP, uelQ3MP, uelQ3CP, uelPo;
    private final QRound ueclQ1MP, ueclQ2MP, ueclQ2CP, ueclQ3MP, ueclQ3CP, ueclPoMP, ueclPoCP;
    private final LeaguePhaseRound uclLP, uelLP, ueclLP;
    private final List<Round> rounds;
    // Index for fast lookup of rounds by RoundType (avoids repeated full scans)
    private final EnumMap<RoundType, List<Round>> roundsByType = new EnumMap<>(RoundType.class);

    // ClubEloDataLoader instance is created here; init() is explicitly called in
    // the Rounds constructor.
    private final ClubEloDataLoader clubEloDataLoader = new ClubEloDataLoader();

    /**
     * Initializes the collection of UEFA competition rounds (Champions League,
     * Europa League, and Conference League).
     * <p>
     * Responsibilities performed during construction:
     * <ul>
     * <li>Instantiates rounds for each tournament.</li>
     * <li>Aggregates every round instance into a unified list to enable uniform
     * downstream processing.</li>
     * <li>Loads structural data for all rounds via {@code JsonDataLoader}.</li>
     * <li>Initializes club Elo rating data used for probability calculations.</li>
     * <li>Links the rounds to define progression flow between successive
     * stages.</li>
     * <li>Builds an index for quick lookup of rounds by type.</li>
     * </ul>
     */
    public Rounds() {
        // Create instances for Champions League qualifier rounds.
        uclQ1CP = new QRound(Tournament.CHAMPIONS_LEAGUE, RoundType.Q1, PathType.CHAMPIONS_PATH);
        uclQ2CP = new QRound(Tournament.CHAMPIONS_LEAGUE, RoundType.Q2, PathType.CHAMPIONS_PATH);
        uclQ2LP = new QRound(Tournament.CHAMPIONS_LEAGUE, RoundType.Q2, PathType.LEAGUE_PATH);
        uclQ3CP = new QRound(Tournament.CHAMPIONS_LEAGUE, RoundType.Q3, PathType.CHAMPIONS_PATH);
        uclQ3LP = new QRound(Tournament.CHAMPIONS_LEAGUE, RoundType.Q3, PathType.LEAGUE_PATH);
        uclPoCP = new QRound(Tournament.CHAMPIONS_LEAGUE, RoundType.PLAYOFF, PathType.CHAMPIONS_PATH);
        uclPoLP = new QRound(Tournament.CHAMPIONS_LEAGUE, RoundType.PLAYOFF, PathType.LEAGUE_PATH);
        uclLP = new UclUelLeaguePhaseRound(Tournament.CHAMPIONS_LEAGUE);

        // Create instances for Europa League qualifier rounds.
        uelQ1MP = new QRound(Tournament.EUROPA_LEAGUE, RoundType.Q1, PathType.MAIN_PATH);
        uelQ2MP = new QRound(Tournament.EUROPA_LEAGUE, RoundType.Q2, PathType.MAIN_PATH);
        uelQ3MP = new QRound(Tournament.EUROPA_LEAGUE, RoundType.Q3, PathType.MAIN_PATH);
        uelQ3CP = new QRound(Tournament.EUROPA_LEAGUE, RoundType.Q3, PathType.CHAMPIONS_PATH);
        uelPo = new QRound(Tournament.EUROPA_LEAGUE, RoundType.PLAYOFF, PathType.MAIN_PATH);
        uelLP = new UclUelLeaguePhaseRound(Tournament.EUROPA_LEAGUE);

        // Create instances for Conference League qualifier rounds.
        ueclQ1MP = new QRound(Tournament.CONFERENCE_LEAGUE, RoundType.Q1, PathType.MAIN_PATH);
        ueclQ2MP = new QRound(Tournament.CONFERENCE_LEAGUE, RoundType.Q2, PathType.MAIN_PATH);
        ueclQ2CP = new QRound(Tournament.CONFERENCE_LEAGUE, RoundType.Q2, PathType.CHAMPIONS_PATH);
        ueclQ3MP = new QRound(Tournament.CONFERENCE_LEAGUE, RoundType.Q3, PathType.MAIN_PATH);
        ueclQ3CP = new QRound(Tournament.CONFERENCE_LEAGUE, RoundType.Q3, PathType.CHAMPIONS_PATH);
        ueclPoMP = new QRound(Tournament.CONFERENCE_LEAGUE, RoundType.PLAYOFF, PathType.MAIN_PATH);
        ueclPoCP = new QRound(Tournament.CONFERENCE_LEAGUE, RoundType.PLAYOFF, PathType.CHAMPIONS_PATH);
        ueclLP = new UeclLeaguePhaseRound();

        // Aggregate all rounds into a list for streamlined processing (order is
        // chronological).
        rounds = new ArrayList<>(Arrays.asList(
                uclQ1CP, uelQ1MP, ueclQ1MP,
                uclQ2CP, uclQ2LP, uelQ2MP, ueclQ2MP, ueclQ2CP,
                uclQ3CP, uclQ3LP, uelQ3MP, uelQ3CP, ueclQ3MP, ueclQ3CP,
                uclPoCP, uclPoLP, uelPo, ueclPoMP, ueclPoCP,
                uclLP, uelLP, ueclLP));

        // Initialize data for each round.
        JsonDataLoader.loadDataForRounds(rounds);

        // Initialize the ClubEloDataLoader to fetch club Elo ratings.
        clubEloDataLoader.init();

        // Link rounds to define the progression flow.
        linkRounds();

        // Build index for quick RoundType -> List<Round> lookup.
        indexRoundsByType();
    }

    public List<Round> getRounds() {
        return rounds;
    }

    /**
     * Builds the internal index mapping each {@link RoundType} to an immutable list
     * of its rounds. Keeps insertion / chronological order.
     */
    private void indexRoundsByType() {
        roundsByType.clear();
        for (Round r : rounds) {
            List<Round> list = roundsByType.get(r.getRoundType());
            if (list == null) {
                list = new ArrayList<>();
                roundsByType.put(r.getRoundType(), list);
            }
            list.add(r);
        }
        // Freeze lists (defensive copy) to prevent external mutation.
        for (RoundType rt : roundsByType.keySet()) {
            roundsByType.put(rt, List.copyOf(roundsByType.get(rt)));
        }
    }

    /**
     * Establishes connections between rounds by assigning the next primary and
     * secondary rounds. These links define the simulation flow from initial
     * qualifying rounds to the league phase.
     */
    private void linkRounds() {

        // Linking for Champions League
        uclQ1CP.setNextRounds(uclQ2CP, ueclQ2CP);
        uclQ2CP.setNextRounds(uclQ3CP, uelQ3CP);
        uclQ2LP.setNextRounds(uclQ3LP, uelQ3MP);
        uclQ3CP.setNextRounds(uclPoCP, uelPo);
        uclQ3LP.setNextRounds(uclPoLP, uelLP);
        uclPoCP.setNextRounds(uclLP, uelLP);
        uclPoLP.setNextRounds(uclLP, uelLP);
        uclLP.setNextRound(null);

        // Linking for Europa League
        uelQ1MP.setNextRounds(uelQ2MP, ueclQ2MP);
        uelQ2MP.setNextRounds(uelQ3MP, ueclQ3MP);
        uelQ3MP.setNextRounds(uelPo, ueclPoMP);
        uelQ3CP.setNextRounds(uelPo, ueclPoCP);
        uelPo.setNextRounds(uelLP, ueclLP);
        uelLP.setNextRound(null);

        // Linking for Conference League (single next round linkage in some cases)
        ueclQ1MP.setNextRound(ueclQ2MP);
        ueclQ2MP.setNextRound(ueclQ3MP);
        ueclQ2CP.setNextRound(ueclQ3CP);
        ueclQ3MP.setNextRound(ueclPoMP);
        ueclQ3CP.setNextRound(ueclPoCP);
        ueclPoMP.setNextRound(ueclLP);
        ueclPoCP.setNextRound(ueclLP);
        ueclLP.setNextRound(null);
    }

    /**
     * Initiates the simulation by executing all rounds in their respective order.
     * This method drives the simulation from qualifiers through leagues.
     */
    public void run(String threadName) {
        // Start by processing the qualifying rounds.
        runQRounds();
        // Proceed to the league phase rounds.
        runLeagueRounds();
    }

    /**
     * Executes the full qualification pipeline (Q1, Q2, etc.), performing (in a
     * tightly controlled order) seeding, drawing, slot resolution, tie registration
     * for subsequent rounds, match simulation, and final slot resolution for the
     * league phase.
     */
    private void runQRounds() {
        // Retrieve all defined round types
        final RoundType[] roundTypes = RoundType.values();
        List<Round> roundsOfType = null;
        // Execute seeding and draws for Q1 round type.
        seedDrawQRounds(getRoundsOfType(RoundType.Q1));
        for (int i = 0; roundTypes[i] != RoundType.LEAGUE_PHASE; i++) {
            // Filter rounds by the current round type.
            roundsOfType = getRoundsOfType(roundTypes[i]);
            // Resolve club slots for the current round type: convert any pending tie-based
            // club slots (e.g. Winner of Tie X / Loser of Tie Y) into concrete club entries
            resolveClubSlots(roundsOfType);
            // Register ties for the next round type.
            regTiesForNextRounds(roundsOfType);
            // Execute seeding and draws for next round type.
            seedDrawQRounds(getRoundsOfType(roundTypes[i + 1]));
            // Play the matches of the round type.
            playQRounds(roundsOfType);
        }
        // Finalize League Phase participants: convert any pending tie-based club slots
        // (e.g. Winner of Tie X / Loser of Tie Y) into concrete club entries for all
        // League Phase rounds.
        resolveClubSlots(getRoundsOfType(RoundType.LEAGUE_PHASE));
    }

    /**
     * Retrieves all rounds of the specified type.
     *
     * @param roundType the type of rounds to retrieve
     * @return a non-null list containing all rounds of the requested type
     */
    public List<Round> getRoundsOfType(RoundType roundType) {
        return roundsByType.getOrDefault(roundType, List.of());
    }

    /**
     * Seeds and draws the given rounds if applicable.
     * 
     * @param roundsOfType a list of rounds (only processed if they are qualifying
     *                     rounds)
     */
    private void seedDrawQRounds(List<Round> roundsOfType) {
        if (roundsOfType.get(0) instanceof QRound) {
            seedDrawRounds(roundsOfType);
        }
    }

    /**
     * Seeds and draws a collection of rounds.
     */
    private void seedDrawRounds(List<Round> roundsOfType) {
        roundsOfType.forEach(round -> {
            // if (round.getTournament() == Tournament.CONFERENCE_LEAGUE
            // && round.getRoundType() == RoundType.LEAGUE_PHASE) {
            // long start = System.nanoTime();
            // round.seedDraw();
            // long elapsedNs = System.nanoTime() - start;
            // System.out.printf("[%s] Seed/Draw for league phase round %s took %.2f ms%n",
            // Thread.currentThread().getName(), round.getName(), elapsedNs / 1_000_000.0);
            // } else {
            // round.seedDraw();
            // }
            round.seedDraw();
        });
    }

    /**
     * Attempts to resolve every {@link ClubSlot} in these rounds to a concrete club
     * (if possible).
     * 
     * @param roundsOfType list of rounds
     */
    private void resolveClubSlots(List<Round> roundsOfType) {
        roundsOfType.forEach(round -> {
            round.resolveClubSlots();
        });
    }

    /**
     * Registers ties for the next round type if the current rounds are QRounds.
     * This ensures that winners are correctly aligned for their subsequent matches.
     *
     * @param roundsOfType list of rounds
     */
    private void regTiesForNextRounds(List<Round> roundsOfType) {
        roundsOfType.forEach(round -> {
            ((QRound) round).regTiesForNextRounds();
        });
    }

    /**
     * Plays qualifying rounds for the provided list of rounds, grouped by
     * tournament type.
     * <p>
     * The method performs the following steps:
     * <ol>
     * <li>Plays all Champions League (UCL) rounds.</li>
     * <li>Applies temporary ELO changes to clubs of represented NAs in rounds
     * above.</li>
     * <li>Plays all Europa League (UEL) and Europa Conference League (UECL)
     * rounds.</li>
     * <li>Applies temporary ELO changes to clubs of represented NAs in rounds
     * above.</li>
     * <li>Plays all UCL rounds that use double-legged ties.</li>
     * <li>Applies temporary ELO changes to clubs of represented NAs in rounds
     * above.</li>
     * <li>Plays all UEL/UECL rounds that use double-legged ties.</li>
     * <li>Applies temporary ELO changes to clubs of represented NAs in rounds
     * above.</li>
     * </ol>
     *
     * The reason for this complex workflow is to make sure the inter-league ELO
     * adjustments (point 2, 4, 5 and 8) are done in a realistic order.
     *
     * @param roundsOfType the list of qualifying rounds to be played, grouped by
     *                     tournament type
     */
    private void playQRounds(List<Round> roundsOfType) {

        List<Round> uclRounds = roundsOfType.stream()
                .filter(r -> r.getTournament() == Tournament.CHAMPIONS_LEAGUE)
                .toList();
        List<Round> uelUeclRounds = roundsOfType.stream()
                .filter(r -> r.getTournament() != Tournament.CHAMPIONS_LEAGUE)
                .toList();

        // 1. Play all UCL rounds
        uclRounds.forEach(r -> r.play(clubEloDataLoader));
        // 2. TODO: Apply all temp ELO changes to the clubs of represented NAs.
        // TODO

        // 3. Play all UEL/UECL rounds
        uelUeclRounds.forEach(r -> r.play(clubEloDataLoader));
        // 4. TODO: Apply all temp ELO changes to the clubs of represented NAs.
        // TODO

        // 5. Play all UCL rounds with DoubleLeggedTie
        uclRounds.stream()
                .filter(r -> !r.getTies().isEmpty() && r.getTies().get(0) instanceof DoubleLeggedTie)
                .forEach(r -> r.play(clubEloDataLoader));
        // 6. TODO: Apply all temp ELO changes to the clubs of represented NAs.
        // TODO

        // 7. Play all UEL/UECL rounds with DoubleLeggedTie
        uelUeclRounds.stream()
                .filter(r -> !r.getTies().isEmpty() && r.getTies().get(0) instanceof DoubleLeggedTie)
                .forEach(r -> r.play(clubEloDataLoader));
        // 8. TODO: Apply all temp ELO changes to the clubs of represented NAs.
        // TODO
    }

    /**
     * Executes the workflow for league phase rounds.
     */
    private void runLeagueRounds() {
        // Execute seeding and draws for league phase rounds.
        seedDrawRounds(getRoundsOfType(RoundType.LEAGUE_PHASE));
        // Play the league phase rounds.
        // playRounds(getRoundsOfType(RoundType.LEAGUE_PHASE));
    }

    /**
     * Returns a string representation of all rounds for logging and debugging
     * purposes.
     *
     * @return a string summarizing the rounds sequence in the simulation
     */
    @Override
    public String toString() {
        return "Rounds [" + Arrays.toString(rounds.toArray()) + "]";
    }
}