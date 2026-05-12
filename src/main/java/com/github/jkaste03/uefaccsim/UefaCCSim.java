package com.github.jkaste03.uefaccsim;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.stream.IntStream;

import com.github.jkaste03.uefaccsim.model.competition.Rounds;

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
    private static final int SIMS = 100;

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