package com.github.jkaste03.uefaccsim.reporting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.model.Club;
import com.github.jkaste03.uefaccsim.model.competition.QRound;
import com.github.jkaste03.uefaccsim.model.competition.Round;
import com.github.jkaste03.uefaccsim.model.competition.Rounds;
import com.github.jkaste03.uefaccsim.repository.ClubRepository;

/**
 * Writes individual reports for each club based on the aggregated statistics
 * from the simulations. Each report includes the club's name, national
 * association, and detailed statistics for each round the club participated in.
 * Reports are organized in a directory structure by country and club name.
 */
public final class ClubReportWriter {
    /**
     * Path to the root directory where club reports will be written.
     */
    private final Path reportRoot;

    /**
     * Formatter used to convert round statistics into report text.
     */
    private final RoundStatsReportFormatter formatter;

    /**
     * Creates a new instance of {@code ClubReportWriter} with the specified report
     * root directory. Uses a default {@link RoundStatsReportFormatter} for
     * formatting round statistics into report text.
     * 
     * @param reportRoot the root directory where club reports will be written
     */
    public ClubReportWriter(Path reportRoot) {
        this(reportRoot, new RoundStatsReportFormatter());
    }

    /**
     * Creates a new instance of {@code ClubReportWriter} with the specified report
     * root directory and formatter.
     * 
     * @param reportRoot the root directory where club reports will be written
     * @param formatter  the formatter used to convert round statistics into report
     *                   text
     */
    ClubReportWriter(Path reportRoot, RoundStatsReportFormatter formatter) {
        this.reportRoot = reportRoot;
        this.formatter = formatter;
    }

    /**
     * Writes individual reports for each club based on the provided statistics
     * aggregator and rounds information.
     */
    public void writeClubReports(StatsAggregator finalStatsAggregator, Rounds rounds) {
        try {
            // Create the root directory for reports.
            Files.createDirectories(reportRoot);

            // Iterate through all clubs to generate individual reports. Each report is
            // written to a file named after the club, organized within a subdirectory for
            // the club's country.
            for (Club club : ClubRepository.getAllClubs()) {
                // Create a subdirectory for the club's country.
                String countryName = club.getCountry() != null ? club.getCountry().getCountryName() : "Unknown";
                Path countryDirectory = reportRoot.resolve(sanitizeFileName(countryName));
                Files.createDirectories(countryDirectory);

                // Create the report content.
                StringBuilder report = new StringBuilder();
                // Basic club info.
                report.append("Club: ").append(club.getName()).append(System.lineSeparator());
                report.append("National association: ").append(countryName).append(System.lineSeparator());

                // Append sections for each round the club participated in. We iterate through
                // all rounds.
                for (Round round : rounds.getRounds()) {
                    // For now, we only report up to the Round of 16.
                    if (round.getRoundType() == RoundType.ROUND_OF_16) {
                        break;
                    }

                    // Format the round key for reporting. For qualifying rounds, we include the
                    // path type in the key; for league phases, we do not.
                    StatsAggregator.RoundKey roundKey = round instanceof QRound qRound
                            ? new StatsAggregator.RoundKey(round.getTournament(), round.getRoundType(),
                                    qRound.getPathType())
                            : new StatsAggregator.RoundKey(round.getTournament(), round.getRoundType(), null);

                    // Append the report section for this round.
                    report.append(formatter.format(roundKey,
                            finalStatsAggregator.getRoundStatsOrThrow(roundKey),
                            club.getName()));
                }

                // Write the report to a file named after the club, within the country
                // subdirectory.
                Path reportFile = countryDirectory.resolve(sanitizeFileName(club.getName()) + ".txt");
                // Using UTF-8 encoding to support a wide range of characters in club names and
                // report content.
                Files.write(reportFile, report.toString().getBytes(StandardCharsets.UTF_8));
            }

            System.out.println("Club reports written to: " + reportRoot.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write club reports", e);
        }
    }

    /**
     * Sanitizes a string to be safely used as a file name by replacing illegal
     * characters with underscores and trimming whitespace.
     * 
     * @param value the string to sanitize
     * @return a sanitized string safe for use as a file name
     */
    private static String sanitizeFileName(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}