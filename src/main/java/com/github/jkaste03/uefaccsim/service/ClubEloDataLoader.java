package com.github.jkaste03.uefaccsim.service;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.StreamSupport;

import com.github.jkaste03.uefaccsim.repository.ClubRepository;

/**
 * ClubEloDataLoader is responsible for managing the loading, updating, and
 * validation
 * of football club Elo ratings from a remote API and local CSV files.
 * <p>
 * Main responsibilities include:
 * <ul>
 * <li>Downloading the latest Elo ratings CSV from the ClubElo API for a
 * specified date.</li>
 * <li>Storing and managing Elo data in local files within a designated data
 * folder.</li>
 * <li>Loading Elo ratings into memory and mapping them to club IDs using the
 * ClubRepository.</li>
 * <li>Supporting pending (uncommitted) Elo deltas for clubs, allowing batch
 * updates before committing changes.</li>
 * <li>Validating that all clubs in the repository have corresponding Elo
 * ratings, ensuring data completeness.</li>
 * </ul>
 * <p>
 * <b>Note:</b> Club names in the local data.json must match those used by the
 * ClubElo API.
 * If a club is missing an Elo rating, update data.json accordingly.
 * </p>
 *
 * @author johan
 */
public class ClubEloDataLoader implements Serializable {
    /** URL for the club elo ratings API */
    private static final String BASE_URL = "http://api.clubelo.com/";
    /** Folder for storing downloaded data */
    private static final String DATA_FOLDER = "src/main/java/com/github/jkaste03/uefaccsim/data/";
    private static final LocalDate date = LocalDate.now();
    /** File path for the current date's CSV file */
    private static String filePath = DATA_FOLDER + date + ".csv";
    /** Map: clubId -> current Elo rating. */
    private final Map<Integer, Double> eloMap = new HashMap<>();
    /** Map: clubId -> pending uncommitted Elo delta. */
    private final Map<Integer, Double> pendingDeltas = new HashMap<>();

    /**
     * Retrieves the file path of the latest CSV file in the data folder.
     * The method searches for files matching the pattern "yyyy-MM-dd.csv" and
     * returns the one
     * with the highest (latest) name according to natural order
     * (lexicographically).
     *
     * @return the file path of the latest CSV file, or {@code null} if no matching
     *         file is found or an error occurs.
     */
    private static String getLatestCsvFilePath() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(DATA_FOLDER), "*.csv")) {
            return StreamSupport.stream(stream.spliterator(), false)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.matches("\\d{4}-\\d{2}-\\d{2}\\.csv"))
                    .max(Comparator.naturalOrder())
                    .map(name -> DATA_FOLDER + name)
                    .orElse(null);
        } catch (IOException e) {
            // System.err.println("Could not list CSV files: " + e.getMessage());
            return null;
        }
    }

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
     * <li>Loads elo ratings from the CSV file into the {@link #eloDataMap}.</li>
     * <li>Validates that every required club has an associated elo rating,
     * enforcing data completeness.</li>
     * </ol>
     * </p>
     */
    public void init() {
        // Download file if it does not exist
        if (!Files.exists(Path.of(filePath))) {
            downloadAndReplaceCSV(date);
        }
        loadEloRatings();
        validateAllClubsHaveElo();
    }

    /**
     * Downloads a CSV file from a remote API for the specified date.
     * <p>
     * The method constructs the API URL using the provided date, creates the
     * necessary data folder,
     * and attempts to download the CSV file. If the download is successful, it
     * deletes any existing CSV files
     * and saves the new file. If a timeout or other error occurs, it falls back to
     * a previous CSV file if available.
     * If no previous file is available and the download fails, an
     * {@link IllegalStateException} is thrown.
     * </p>
     *
     * @param date the {@link LocalDate} for which to download the CSV data
     * @throws IllegalStateException if the download fails and no previous CSV file
     *                               is available
     */
    private static void downloadAndReplaceCSV(LocalDate date) {
        String urlString = BASE_URL + date;
        boolean success = false;
        try {
            Files.createDirectories(Path.of(DATA_FOLDER));
            URI uri = new URI(urlString);
            java.net.URL url = uri.toURL();
            java.net.URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000); // 5 sekunder
            conn.setReadTimeout(10000); // 10 sekunder
            try (InputStream in = conn.getInputStream()) {
                deleteExistingCSVFiles();
                Files.copy(in, Path.of(filePath));
                System.out.println("Downloaded API data for " + date);
                success = true;
            }
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("Timeout while downloading API data from " + urlString + ": " + e.getMessage());
        } catch (URISyntaxException e) {
            System.err.println("Invalid URI syntax for API URL: " + urlString + ". " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error while downloading API data from " + urlString + ": " + e.getMessage());
        }
        // Try to fall back to previous file if download failed
        if (!success) {
            String previousFilePath = getLatestCsvFilePath();
            if (previousFilePath != null) {
                System.err.println("Falling back to previous CSV file: " + previousFilePath);
                filePath = previousFilePath;
            } else {
                throw new IllegalStateException("Failed to download API data and no previous CSV file available.");
            }
        }
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
     * <li>Stores the Elo rating in {@code eloMap} keyed by the resolved club
     * id</li>
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
     * @return the Elo rating for the club if available
     */
    public double getElo(int clubId) {
        Double elo = eloMap.get(clubId);
        return elo;
    }

    /**
     * Updates the Elo rating for a specified club id.
     *
     * @param clubId   the id of the club whose elo rating is to be updated
     * @param deltaElo the delta elo rating for the club
     */
    public void updateEloRating(int clubId, double deltaElo) {
        double current = eloMap.get(clubId);
        eloMap.put(clubId, current + deltaElo);
    }

    /**
     * Retrieves the uncommitedEloDelta for the specified club id.
     *
     * @param clubId the id of the club
     * @return the uncommitedEloDelta value
     */
    public double getUncommitedEloDelta(int clubId) {
        return pendingDeltas.getOrDefault(clubId, 0.0);
    }

    /**
     * Updates the uncommitedEloDelta for a specified club id.
     *
     * @param clubId                  the id of the club
     * @param deltaUncommitedEloDelta the delta uncommitedEloDelta value
     */
    public void updateUncommitedEloDelta(int clubId, double deltaUncommitedEloDelta) {
        double newVal = pendingDeltas.getOrDefault(clubId, 0.0) + deltaUncommitedEloDelta;
        pendingDeltas.put(clubId, newVal);
    }

    /**
     * Applies all uncommited elo adjustments to the current elo ratings
     * for each club.
     */
    public void applyAllUncommitedEloDeltas() {
        // Apply only the clubs that have a pending delta (O(k))
        for (Map.Entry<Integer, Double> e : pendingDeltas.entrySet()) {
            int clubId = e.getKey();
            double delta = e.getValue();
            double current = eloMap.get(clubId);
            eloMap.put(clubId, current + delta);
        }
        pendingDeltas.clear();
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
