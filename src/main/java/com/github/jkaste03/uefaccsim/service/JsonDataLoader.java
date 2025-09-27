package com.github.jkaste03.uefaccsim.service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

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
     * The root key in the JSON file that contains the rounds data.
     */
    private static final String DATA_MAIN_ROOT = "rounds";
    /**
     * The key in the JSON file that contains the previous Champions League winner.
     */
    private static final String PREVIOUS_UCL_WINNER = "previous_champions_league_winner";
    /**
     * The path to the JSON data file.
     */
    private static final String DATA_FILE = "src/main/java/com/github/jkaste03/uefaccsim/data/data.json";

    /**
     * <p>
     * Loads club data for the provided competition rounds from the JSON data source
     * and
     * populates each round with corresponding {@link ClubSlot} instances.
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
                    .getAsJsonObject(DATA_MAIN_ROOT);
            for (Round round : rounds) {
                JsonArray clubsJson = roundsData.getAsJsonArray(round.getName());
                if (clubsJson == null)
                    continue;
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
}