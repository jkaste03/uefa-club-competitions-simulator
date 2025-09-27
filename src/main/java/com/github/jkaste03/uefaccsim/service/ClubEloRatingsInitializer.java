package com.github.jkaste03.uefaccsim.service;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.StreamSupport;

import com.github.jkaste03.uefaccsim.repository.ClubRepository;
import com.github.jkaste03.uefaccsim.repository.ClubSimStateRepository;

/**
 * <p>
 * Utility class responsible for acquiring, caching, loading,
 * and
 * validating Club Elo ratings used in simulation logic.
 * </p>
 *
 * <p>
 * Core responsibilities:
 * </p>
 * <ol>
 * <li><strong>Download:</strong> Fetches the daily ClubElo CSV from
 * <code>http://api.clubelo.com/{yyyy-MM-dd}</code>.</li>
 * <li><strong>Replace:</strong> On successful download, deletes previously
 * cached CSV files
 * and stores only the current day's dataset under
 * <code>src/main/java/com/github/jkaste03/uefaccsim/data/{date}.csv</code>.</li>
 * <li><strong>Fallback:</strong> If the download (network / timeout / I/O)
 * fails, automatically
 * falls back to the most recent previously cached CSV (lexicographically latest
 * date‑named file). If no prior file exists, it fails fast with an
 * <code>IllegalStateException</code>.</li>
 * <li><strong>Parse &amp; Load:</strong> Reads the CSV, maps API club names to
 * internal club IDs via
 * <code>ClubRepository.getIdByName(String)</code>, and injects Elo values into
 * the
 * corresponding <code>ClubSimState</code> instances held by
 * <code>ClubSimStateRepository</code>.</li>
 * <li><strong>Validate Completeness:</strong> Ensures every club known to
 * <code>ClubRepository</code>
 * has a resolved (non -1) Elo rating. Throws an
 * <code>IllegalStateException</code>
 * enumerating all missing clubs with guidance for fixing name mismatches in
 * local
 * <code>data.json</code> (never by editing downloaded CSV files).</li>
 * </ol>
 */
public class ClubEloRatingsInitializer {
    /** URL for the club elo ratings API */
    private static final String BASE_URL = "http://api.clubelo.com/";
    /** Folder for storing downloaded data */
    private static final String DATA_FOLDER = "src/main/java/com/github/jkaste03/uefaccsim/data/";
    private static final LocalDate date = LocalDate.now();
    /** File path for the current date's CSV file */
    private static String filePath = DATA_FOLDER + date + ".csv";

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
     * <li>Loads elo ratings from the CSV file into the corresponding
     * {@code ClubSimState} within the provided {@link ClubSimStateRepository}.</li>
     * <li>Validates that every required club has an associated elo rating,
     * enforcing data completeness.</li>
     * </ol>
     * </p>
     * 
     * @param clubSimStateRepo the {@link ClubSimStateRepository} to populate with
     *                         club simulation states (like elo ratings)
     */
    public static void initializeEloRatings(ClubSimStateRepository clubSimStateRepo) {
        // Download file if it does not exist
        if (!Files.exists(Path.of(filePath))) {
            downloadAndReplaceCSV(date);
        }
        loadEloRatings(clubSimStateRepo);
        validateAllClubsHaveElo(clubSimStateRepo);
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
     * <li>Skips any club name that does not resolve to a valid id (i.e., returns
     * -1).</li>
     * <li>Parses the 5th column (index 4) as a {@code double} Elo rating.</li>
     * <li>Sets the Elo rating in the corresponding {@code ClubSimState} within the
     * provided {@link ClubSimStateRepository}.</li>
     * </ul>
     */
    private static void loadEloRatings(ClubSimStateRepository clubSimStateRepo) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 5)
                    continue;
                String clubName = values[1].trim();
                int clubid = ClubRepository.getIdByName(clubName);
                // Skip if club name not found
                if (clubid == -1)
                    continue;
                double elo = Double.parseDouble(values[4].trim());
                clubSimStateRepo.get(clubid).setElo(elo);
            }
        } catch (IOException e) {
            System.err.println("Could not read API data: " + e.getMessage());
        }
    }

    /**
     * Ensures that every club returned by {@code ClubRepository.getAllClubs()} has
     * a valid (non -1)
     * Elo rating stored in the provided {@link ClubSimStateRepository}. If any club
     * is found with
     * an Elo value of -1, an {@link IllegalStateException} is thrown that
     * enumerates all offending
     * clubs and provides guidance on how to correct the underlying data (i.e.,
     * aligning names in
     * the local {@code data.json} with the ClubElo API names as they appear in the
     * dated CSV file).
     *
     * @param clubSimStateRepo the repository supplying per-club simulation state,
     *                         expected to contain an entry for every club ID
     *                         present in {@code ClubRepository}
     * @throws IllegalStateException if one or more clubs have an Elo rating of -1
     */
    private static void validateAllClubsHaveElo(ClubSimStateRepository clubSimStateRepo) {
        List<String> missing = new ArrayList<>();
        ClubRepository.getAllClubs().forEach(club -> {
            int id = club.getId();
            if (clubSimStateRepo.get(id).getElo() == -1) {
                missing.add(club.getName() + " (id=" + id + ")");
            }
        });
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Missing Elo rating for " + missing.size() + " club(s). "
                            + "Unmatched clubs: " + String.join(", ", missing) + ". "
                            + "Update the local data.json so each club name matches the ClubElo API name (see "
                            + LocalDate.now() + ".csv). Do NOT edit the downloaded CSV; only adjust data.json.");
        }
    }
}
