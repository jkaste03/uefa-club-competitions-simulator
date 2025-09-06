package com.github.jkaste03.uefaccsim.service;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

import com.github.jkaste03.uefaccsim.model.ClubRepository;

/**
 * Primary purpose: maintain a lookup map ({@code Map<Integer, Double>}) from
 * internal club id to the current day's Elo rating (field {@link #eloMap}).
 * The class is centered around initializing and providing fast, read access to
 * this mapping.
 *
 * <p>
 * Secondary responsibilities:
 * <ul>
 * <li>Download (once per JVM run / per calendar day) the CSV for today from the
 * public ClubElo API (endpoint pattern:
 * {@code http://api.clubelo.com/yyyy-MM-dd}).</li>
 * <li>Persist the raw CSV under a deterministic, date‑stamped filename.</li>
 * <li>Parse the CSV, resolve each club name to an internal id via
 * {@code ClubRepository}, and populate {@link #eloMap}.</li>
 * </ul>
 */
public class ClubEloDataLoader implements Serializable {
    // URL for the club elo ratings API
    private static final String BASE_URL = "http://api.clubelo.com/";
    // Folder for storing downloaded data
    private static final String DATA_FOLDER = "src/main/java/com/github/jkaste03/uefaccsim/data/";
    private static String filePath = DATA_FOLDER + LocalDate.now() + ".csv";
    /**
     * Map for storing Elo ratings by club ID.
     */
    private final Map<Integer, Double> eloMap = new HashMap<>();

    /**
     * Initializes the elo data environment.
     * <p>
     * Workflow:
     * <ol>
     * <li>If the expected elo ratings CSV does not exist:
     * <ul>
     * <li>Downloads the latest elo dataset using the current system date, and
     * replaces the existing CSV(s).</li>
     * </ul>
     * </li>
     * <li>Loads elo ratings from the CSV file into the {@link #eloMap}.</li>
     * <li>Validates that every required club has an associated elo rating,
     * enforcing data completeness.</li>
     * </ol>
     * </p>
     */
    public void init() {
        // Download file if it does not exist
        if (!Files.exists(Path.of(filePath))) {
            downloadAndReplaceCSV(LocalDate.now());
        }
        loadEloRatings();
        validateAllClubsHaveElo();
    }

    /**
     * Deletes all existing CSV files in the data package.
     */
    private static void deleteExistingCSVFiles() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(DATA_FOLDER), "*.csv")) {
            for (Path entry : stream) {
                Files.delete(entry);
            }
            System.out.println("Deleted old API data");
        } catch (IOException e) {
            System.err.println("Could not delete existing CSV files: " + e.getMessage());
        }
    }

    /**
     * Downloads the CSV file for the given date from the API.
     *
     * @param date the date for which to download the CSV file.
     */
    private static void downloadAndReplaceCSV(LocalDate date) {
        String urlString = BASE_URL + date;
        try {
            Files.createDirectories(Path.of(DATA_FOLDER));
            URI uri = new URI(urlString);
            java.net.URL url = uri.toURL();
            java.net.URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000); // 5 sekunder
            conn.setReadTimeout(10000); // 10 sekunder
            try (InputStream in = conn.getInputStream()) {
                deleteExistingCSVFiles();
                Files.copy(in, Path.of(filePath), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Downloaded API data for " + date);
            }
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("Timeout while downloading API data from " + urlString + ": " + e.getMessage());
        } catch (URISyntaxException e) {
            System.err.println("Invalid URI syntax for API URL: " + urlString + ". " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error while downloading API data from " + urlString + ": " + e.getMessage());
        }
    }

    /**
     * Loads Elo rating values from the CSV file referenced by {@code filePath} into
     * the internal {@code eloMap}.
     *
     * The method:
     * <ul>
     * <li>Opens the file located at {@code filePath} using a
     * {@link BufferedReader}.</li>
     * <li>Skips the first line (assumed to be a header).</li>
     * <li>Parses subsequent lines by splitting on commas.</li>
     * <li>Ignores any line with fewer than 5 columns.</li>
     * <li>Uses the 2nd column (index 1) as the club's name to resolve a club id via
     * {@link ClubRepository#getIdByName(String)}.</li>
     * <li>Parses the 5th column (index 4) as a {@code double} Elo rating.</li>
     * <li>Stores the Elo rating in {@code eloMap} keyed by the resolved club id
     * (even if the id may be -1 if not found).</li>
     * </ul>
     */
    private void loadEloRatings() {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 5)
                    continue;
                String clubName = values[1].trim();
                int clubid = ClubRepository.getIdByName(clubName);
                double elo = Double.parseDouble(values[4].trim());
                eloMap.put(clubid, elo);
            }
        } catch (IOException e) {
            System.err.println("Could not read API data: " + e.getMessage());
        }
    }

    /**
     * Retrieves the Elo rating for the specified club id.
     *
     * @param clubId the id of the club whose Elo rating is requested
     * @return the Elo rating for the club if available, or 0.0 if not found
     */
    public double getEloRating(int clubId) {
        return eloMap.get(clubId);
    }

    /**
     * Sets the Elo rating for a specified club id.
     *
     * @param clubId the id of the club whose Elo rating is to be set
     * @param elo    the new Elo rating for the club
     */
    public void setEloRating(int clubId, double elo) {
        eloMap.put(clubId, elo);
    }

    /**
     * Verifies that all clubs in {@link ClubRepository} have a loaded Elo value.
     * Throws an IllegalStateException if one or more are missing.
     *
     * @throws IllegalStateException if any club is missing an elo rating
     */
    private void validateAllClubsHaveElo() {
        List<String> missing = new ArrayList<>();
        ClubRepository.getAllClubs().forEach(club -> {
            int id = club.getId();
            if (!eloMap.containsKey(id)) {
                missing.add(club.getName() + " (id=" + id + ")");
            }
        });
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Missing Elo rating for " + missing.size() + " club(s). "
                            + "Unmatched clubs: " + String.join(", ", missing) + ". "
                            + "Update the local data.json so each club name matches the ClubElo API name (see "
                            + LocalDate.now() + ".csv). Do NOT edit the downloaded CSV; only adjust data.json.");

            // TODO
            // No elo for Beitar currently. Using Hapoel H. for now. Need to fix soon.
        }
    }
}
