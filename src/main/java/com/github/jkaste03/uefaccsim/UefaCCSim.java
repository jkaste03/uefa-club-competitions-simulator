package com.github.jkaste03.uefaccsim;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.stream.IntStream;

import com.github.jkaste03.uefaccsim.model.Rounds;

/**
 * Entry point class for running multiple independent UEFA club competition
 * simulations in parallel. A single baseline Rounds instance is created up
 * front
 * and deep-copied per simulation task to guarantee isolation of mutable state
 * while leveraging the Fork/Join common pool via a parallel IntStream.
 *
 * <p>
 * High-level workflow:
 * </p>
 * <ol>
 * <li>Instantiate a baseline Rounds object (acts as a template).</li>
 * <li>For each simulation index (0..SIMS-1) in parallel:
 * <ul>
 * <li>Generate a task label (e.g., "Sim-3").</li>
 * <li>Deep copy the baseline Rounds to avoid shared mutation.</li>
 * <li>Execute the simulation via {@code Rounds.run(taskName)}.</li>
 * </ul>
 * </li>
 * </ol>
 */
public class UefaCCSim {
    private static final int SIMS = 1;

    /**
     * Application entry point that performs a configurable number of independent
     * tournament simulations in parallel using Java's Fork/Join common pool.
     *
     * Workflow:
     * <ol>
     * <li>Instantiate a baseline Rounds object that serves as the template for each
     * simulation.</li>
     * <li>Measure wall-clock execution time around the simulation phase.</li>
     * <li>Execute <code>IntStream.range(0, SIMS).parallel()</code> so each
     * simulation index is processed concurrently.</li>
     * <li>For each simulation:
     * <ul>
     * <li>Create an isolated deep copy of the baseline Rounds instance (to avoid
     * shared mutable state).</li>
     * <li>Assign a task name (e.g., "Sim-1") for traceability.</li>
     * <li>Invoke <code>run(...)</code> on the copied Rounds.</li>
     * </ul>
     * </li>
     * </ol>
     */
    public static void main(String[] args) {
        // Create a new instance of Rounds
        Rounds rounds = new Rounds();

        // Number of simulations
        System.out.println("Simulations: " + SIMS);

        long startTime = System.currentTimeMillis();

        IntStream.range(0, SIMS)
                .parallel()
                .forEach(i -> {
                    String taskName = "Sim-" + (i + 1);
                    Rounds copiedRounds = deepCopy(rounds);
                    copiedRounds.run(taskName);
                });

        long endTime = System.currentTimeMillis();
        System.out.println("Total time: " + (endTime - startTime) + " ms");

        // printClubSeedingStats();
    }

    // private static void printClubSeedingStats() {
    // System.out.println("UECL Playoff draw seeding:");

    // // Conditional table
    // System.out.println("\nConditional probabilities (given participation in
    // playoff draw):");
    // System.out.println("-----------------------------------------------------------------------------------");
    // System.out.printf("%-21s %14s %18s%n", "Club", "Cond. Seeded", "Cond.
    // Unseeded");
    // System.out.println("-----------------------------------------------------------------------------------");
    // ClubRepository.getAllClubs().stream()
    // .filter(club -> club.getRanking() != -1)
    // .sorted((c1, c2) -> {
    // int c1Seeded = c1.getTimesSeeded();
    // int c1Unseeded = c1.getTimesUnseeded();
    // int c1Participated = c1Seeded + c1Unseeded;
    // double c1CondSeed = c1Participated > 0 ? (double) c1Seeded / c1Participated :
    // 0.0;
    // double c1CondUnseed = c1Participated > 0 ? (double) c1Unseeded /
    // c1Participated : 0.0;

    // int c2Seeded = c2.getTimesSeeded();
    // int c2Unseeded = c2.getTimesUnseeded();
    // int c2Participated = c2Seeded + c2Unseeded;
    // double c2CondSeed = c2Participated > 0 ? (double) c2Seeded / c2Participated :
    // 0.0;
    // double c2CondUnseed = c2Participated > 0 ? (double) c2Unseeded /
    // c2Participated : 0.0;

    // // Hvis begge har 0% seeded, sorter på unseeded
    // if (c1CondSeed == 0.0 && c2CondSeed == 0.0) {
    // return Double.compare(c2CondUnseed, c1CondUnseed);
    // }
    // return Double.compare(c2CondSeed, c1CondSeed);
    // })
    // .forEach(club -> {
    // int seededCount = club.getTimesSeeded();
    // int unseededCount = club.getTimesUnseeded();
    // int participated = seededCount + unseededCount;

    // double condSeedPct = participated > 0
    // ? seededCount * 100.0 / participated
    // : 0.0;
    // double condUnseedPct = participated > 0
    // ? unseededCount * 100.0 / participated
    // : 0.0;

    // System.out.printf("%-21s %13.1f%% %17.1f%%%n",
    // club.getName(),
    // condSeedPct, condUnseedPct);
    // });

    // // Absolute table
    // System.out.println("\nAbsolute probabilities (out of all simulations):");
    // System.out.println("---------------------------------------------------------------");
    // System.out.printf("%-21s %12s %16s%n", "Club", "Abs. Seeded", "Abs.
    // Unseeded");
    // System.out.println("---------------------------------------------------------------");
    // ClubRepository.getAllClubs().stream()
    // .filter(club -> club.getRanking() != -1)
    // .sorted((c1, c2) -> {
    // double c1AbsSeed = (double) c1.getTimesSeeded() / SIMS;
    // double c2AbsSeed = (double) c2.getTimesSeeded() / SIMS;
    // // Hvis begge har 0% seeded, sorter på unseeded
    // if ((c1AbsSeed == 0.0 && c2AbsSeed == 0.0) ||
    // (c1AbsSeed == 1.0 && c2AbsSeed == 1.0)) {
    // double c1AbsUnseed = (double) c1.getTimesUnseeded() / SIMS;
    // double c2AbsUnseed = (double) c2.getTimesUnseeded() / SIMS;
    // return Double.compare(c2AbsUnseed, c1AbsUnseed);
    // }
    // return Double.compare(c2AbsSeed, c1AbsSeed);
    // })
    // .forEach(club -> {
    // int seededCount = club.getTimesSeeded();
    // int unseededCount = club.getTimesUnseeded();

    // double absSeedPct = seededCount * 100.0 / SIMS;
    // double absUnseedPct = unseededCount * 100.0 / SIMS;

    // System.out.printf("%-21s %11.1f%% %15.1f%%%n",
    // club.getName(),
    // absSeedPct, absUnseedPct);
    // });
    // }

    /**
     * Creates a deep copy of the given object using serialization.
     *
     * @param <T>    The type of the object to be copied.
     * @param object The object to be copied.
     * @return A deep copy of the given object.
     * @throws RuntimeException If the deep copy process fails.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deepCopy(T object) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(object);
            out.flush();
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bis);
            return (T) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Deep copy failed", e);
        }
    }
}