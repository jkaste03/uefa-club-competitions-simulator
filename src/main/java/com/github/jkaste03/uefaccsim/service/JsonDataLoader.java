package com.github.jkaste03.uefaccsim.service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.model.Club;
import com.github.jkaste03.uefaccsim.model.competition.ClubSimState;
import com.github.jkaste03.uefaccsim.model.competition.ClubSlot;
import com.github.jkaste03.uefaccsim.model.competition.Round;
import com.github.jkaste03.uefaccsim.repository.ClubRepository;
import com.github.jkaste03.uefaccsim.repository.ClubSimStateRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Utility class for loading JSON data into rounds using Gson.
 * Uses Gson to deserialize Club objects without invoking their constructor.
 */
public class JsonDataLoader {

    /**
     * The rounds root key in the JSON file that contains the rounds data.
     */
    private static final String ROUNDS_ROOT_KEY = "rounds";
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
     * <p>
     * Loads club data for the provided competition rounds from the JSON data source
     * and populates each round with corresponding {@link ClubSlot} instances.
     * </p>
     *
     * @param rounds           the ordered list of competition rounds to populate;
     *                         each round's name must correspond to a key in the
     *                         JSON structure
     * @param clubSimStateRepo repository used to create and track simulation state
     *                         for each club
     */
    public static void loadDataForRounds(List<Round> rounds, ClubSimStateRepository clubSimStateRepo) {
        Gson gson = new Gson();
        try (Reader reader = new FileReader(DATA_FILE)) {
            JsonObject roundsData = JsonParser.parseReader(reader)
                    .getAsJsonObject()
                    .getAsJsonObject(ROUNDS_ROOT_KEY);
            for (Round round : rounds) {
                // Attempt to retrieve the "clubs" array for the round from the JSON data.
                JsonArray clubsJson = null;
                try {
                    // Access the JSON object for the current round using its name as the key.
                    JsonObject roundData = roundsData.getAsJsonObject(round.getName());
                    // Get the "clubs" array from the round's JSON object.
                    clubsJson = roundData.getAsJsonArray("clubs");
                } catch (NullPointerException e) {
                    // If the round name is not found in the JSON or if the "clubs" array is
                    // missing, skip this round.
                    continue;
                }
                clubsJson.forEach(jsonElement -> {
                    // Deserialize JSON into a Club instance.
                    // Note: Gson will bypass the Club constructor.
                    Club club = gson.fromJson(jsonElement, Club.class); // no-arg ctor runs automatically
                    ClubSimState w = clubSimStateRepo.create(club.getId());
                    round.addClubSlot(new ClubSlot(w));
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Set the previous Champions League winner from the JSON data in
        // ClubRepository.
        setPreviousChampionsLeagueWinner();
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