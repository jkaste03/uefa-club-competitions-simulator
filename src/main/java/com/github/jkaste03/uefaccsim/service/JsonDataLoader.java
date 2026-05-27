package com.github.jkaste03.uefaccsim.service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.model.Club;
import com.github.jkaste03.uefaccsim.model.competition.ClubSimState;
import com.github.jkaste03.uefaccsim.model.competition.ClubSlot;
import com.github.jkaste03.uefaccsim.model.competition.KnockoutTie;
import com.github.jkaste03.uefaccsim.model.competition.Round;
import com.github.jkaste03.uefaccsim.repository.ClubRepository;
import com.github.jkaste03.uefaccsim.repository.ClubSimStateRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Helper for bootstrapping in-memory competition data from the repository's
 * JSON fixture file.
 *
 * <p>
 * This class encapsulates the logic required to populate the provided
 * {@link Round} instances with {@link ClubSlot} objects and
 * {@link KnockoutTie} instances by reading a single JSON file (see
 * {@link #DATA_FILE}). It is intentionally implemented as a collection of
 * static helpers because it is used during application startup and test
 * fixtures rather than as part of the simulation domain model.
 * </p>
 *
 * @see #DATA_FILE
 */
public class JsonDataLoader {

    /**
     * Simple data-transfer object used to deserialize tie records from the JSON
     * data file via Gson.
     */
    static class TieDto {
        String clubA;
        String clubB;
        Integer clubAGoals1stLeg;
        Integer clubBGoals1stLeg;
        Integer clubAGoals2ndLeg;
        Integer clubBGoals2ndLeg;
        Boolean clubAWinner;
    }

    /**
     * Logger for logging errors during JSON data loading.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JsonDataLoader.class);

    /**
     * The rounds root key in the JSON file that contains the rounds data.
     */
    private static final String ROUNDS_ROOT_KEY = "rounds";

    /**
     * The key in the JSON file that contains the list of clubs for each round.
     */
    private static final String CLUBS_KEY = "clubs";

    /**
     * The key in the JSON file that contains the list of ties for each round.
     */
    private static final String TIES_KEY = "ties";

    /**
     * The key in the JSON file that contains the previous Champions League winner.
     */
    private static final String PREVIOUS_UCL_WINNER = "previous_champions_league_winner";

    /**
     * JSON key for accessing the league phase play order grouped by day.
     */
    private static final String LEAGUE_PHASE_PLAY_ORDER_GROUPED_BY_DAY = "league_phase_play_order_grouped_by_day";
    /**
     * The path to the JSON data file.
     */
    private static final String DATA_FILE = "src/main/java/com/github/jkaste03/uefaccsim/data/data.json";

    /**
     * Cache of club names to the {@link ClubSlot} instances created while loading
     * clubs from JSON.
     *
     * <p>
     * The same club slots are reused when ties are loaded, to quickly go from club
     * names to club slots.
     * </p>
     */
    private static final Map<String, ClubSlot> clubSlotCache = new HashMap<>();

    /**
     * Loads all JSON-backed data for the supplied rounds.
     *
     * <p>
     * For each round, this method loads clubs first and then ties, so that tie
     * references can resolve to the {@link ClubSlot} instances created from the
     * club list. After all rounds are populated, it updates the stored previous
     * Champions League winner in {@link ClubRepository}.
     * </p>
     *
     * @param rounds           the ordered list of competition rounds to populate;
     *                         round names must exist as a key under the JSON
     *                         {@code rounds} object
     * @param clubSimStateRepo repository used to create and track simulation state
     *                         for each loaded club
     */
    public static void loadDataForRounds(List<Round> rounds, ClubSimStateRepository clubSimStateRepo) {
        // Load the entire rounds JSON object from the data file.
        JsonObject roundsData = null;
        try (Reader reader = new FileReader(DATA_FILE)) {
            roundsData = JsonParser.parseReader(reader)
                    .getAsJsonObject()
                    .getAsJsonObject(ROUNDS_ROOT_KEY);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Iterate through the provided rounds and attempt to load club and tie data for
        // each round from the JSON.
        for (Round round : rounds) {
            // Attempt to access the JSON object for the round using its name as the key.
            JsonObject roundData = null;
            roundData = roundsData.getAsJsonObject(round.getName());
            // If no data is found for the round, skip it and continue to the next round.
            if (roundData == null) {
                continue;
            }
            Gson gson = new Gson();
            // Load clubs first so that tie loading can reference the created club slots.
            loadClubData(round, roundData, clubSimStateRepo, gson);
            loadTieData(round, roundData, gson);
        }

        // Set the previous Champions League winner from the JSON data in
        // ClubRepository.
        setPreviousChampionsLeagueWinner();
    }

    /**
     * Loads the club list for a single round and registers each club in the round
     * and in the shared club-slot cache.
     *
     * <p>
     * The cache is populated here so that later tie loading can look up the same
     * {@link ClubSlot} instances by club name.
     * </p>
     *
     * @param round            the round being populated
     * @param roundData        the JSON object containing the round data
     * @param clubSimStateRepo repository used to create simulation state for each
     *                         club
     * @param gson             Gson instance used to deserialize the club JSON
     */
    private static void loadClubData(Round round, JsonObject roundData, ClubSimStateRepository clubSimStateRepo,
            Gson gson) {
        // Attempt to retrieve the "clubs" array for the round from the JSON data.
        JsonArray clubsJson = roundData.getAsJsonArray(CLUBS_KEY);
        // If the "clubs" array is present, iterate through each club JSON element.
        if (clubsJson != null) {
            clubsJson.forEach(jsonElement -> {
                // Deserialize JSON into a Club instance.
                // Note: Gson will bypass the Club constructor.
                Club club = gson.fromJson(jsonElement, Club.class); // no-arg constructor runs automatically
                ClubSimState w = clubSimStateRepo.create(club.getId());

                ClubSlot clubSlot = new ClubSlot(w);
                // Cache the club slot for potential reuse when processing ties.
                clubSlotCache.put(club.getName(), clubSlot);
                // Add the club slot to the round.
                round.addClubSlot(clubSlot);
            });
        }
    }

    /**
     * Loads the tie list for a single round and attaches each tie to the round.
     *
     * <p>
     * Each tie record is deserialized into a {@link TieDto}, resolved against the
     * club-slot cache, and then converted into a {@link KnockoutTie} that is added
     * to the round before simulation starts.
     * </p>
     *
     * @param round     the round being populated
     * @param roundData the JSON object containing the round data
     * @param gson      Gson instance used to deserialize the tie JSON
     */
    private static void loadTieData(Round round, JsonObject roundData, Gson gson) {
        // Attempt to retrieve the "ties" array for the round from the JSON data.
        JsonArray tiesJson = roundData.getAsJsonArray(TIES_KEY);
        if (tiesJson != null) {
            tiesJson.forEach(jsonElement -> {
                // Deserialize JSON into a TieDto instance.
                TieDto dto = gson.fromJson(jsonElement, TieDto.class);
                // Since ties reference clubs by name, look up the corresponding ClubSlot
                // instances from the cache.
                ClubSlot clubSlotA = clubSlotCache.get(dto.clubA);
                ClubSlot clubSlotB = clubSlotCache.get(dto.clubB);
                // If either club slot is not found in the cache, stop the program.
                if (clubSlotA == null || clubSlotB == null) {
                    String msg = "Invalid/Missing clubs in '" + round.getName() + "': clubA="
                            + dto.clubA + ", clubB=" + dto.clubB;
                    logger.error(msg);
                    throw new IllegalStateException(msg);
                }
                // Add a new KnockoutTie to the round using the data from the TieDto.
                round.addTiePreSim(new KnockoutTie(clubSlotA, clubSlotB, dto.clubAGoals1stLeg,
                        dto.clubBGoals1stLeg, dto.clubAGoals2ndLeg, dto.clubBGoals2ndLeg, round.getTournament(),
                        dto.clubAWinner));
            });
        }
    }

    /**
     * Fetches the value of previous_champions_league_winner from the JSON data, and
     * sets that club's ID as the last UCL winner ID in ClubRepository.
     */
    private static void setPreviousChampionsLeagueWinner() {
        try (Reader reader = new FileReader(DATA_FILE)) {
            JsonObject jsonData = JsonParser.parseReader(reader).getAsJsonObject();
            String previousUclWinnerName = jsonData.get(PREVIOUS_UCL_WINNER).getAsString();
            int previousUclWinnerId = ClubRepository.getIdByName(previousUclWinnerName);
            ClubRepository.setLastUclWinnerId(previousUclWinnerId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the league phase play order grouped by day from the JSON data file.
     * 
     * <p>
     * This method reads the JSON data file and parses the league phase play order,
     * which is organized as a nested array structure where each element represents
     * a day containing a list of tournaments scheduled for that day.
     * 
     * @return a list of lists of {@link Tournament} objects, where each inner list
     *         represents the tournaments scheduled for a specific day in the league
     *         phase.
     *         The returned list is immutable.
     * 
     * @throws IllegalStateException if the JSON data file cannot be read, if the
     *                               required
     *                               JSON array is missing from the data file, or if
     *                               an invalid tournament name
     *                               is encountered in the JSON data
     * 
     * @see Tournament
     * @see JsonDataLoader#DATA_FILE
     * @see JsonDataLoader#LEAGUE_PHASE_PLAY_ORDER_GROUPED_BY_DAY
     */
    public static List<List<Tournament>> loadLeaguePhasePlayOrderGroupedByDay() {
        try (Reader reader = new FileReader(DATA_FILE)) {
            JsonObject jsonData = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray orderJson = jsonData.getAsJsonArray(LEAGUE_PHASE_PLAY_ORDER_GROUPED_BY_DAY);
            if (orderJson == null) {
                throw new IllegalStateException(
                        "Missing JSON array: " + LEAGUE_PHASE_PLAY_ORDER_GROUPED_BY_DAY);
            }

            List<List<Tournament>> tournamentDays = new ArrayList<>(orderJson.size());
            for (int dayIndex = 0; dayIndex < orderJson.size(); dayIndex++) {
                JsonArray dayArray = orderJson.get(dayIndex).getAsJsonArray();
                List<Tournament> tournamentsOnDay = new ArrayList<>(dayArray.size());

                for (int tournamentIndex = 0; tournamentIndex < dayArray.size(); tournamentIndex++) {
                    String tournamentName = dayArray.get(tournamentIndex).getAsString();
                    try {
                        tournamentsOnDay.add(Tournament.valueOf(tournamentName));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException(
                                "Invalid tournament in " + LEAGUE_PHASE_PLAY_ORDER_GROUPED_BY_DAY
                                        + " at day " + dayIndex
                                        + ", index " + tournamentIndex + ": " + tournamentName,
                                e);
                    }
                }

                tournamentDays.add(List.copyOf(tournamentsOnDay));
            }

            return List.copyOf(tournamentDays);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load " + LEAGUE_PHASE_PLAY_ORDER_GROUPED_BY_DAY + " from JSON",
                    e);
        }
    }
}