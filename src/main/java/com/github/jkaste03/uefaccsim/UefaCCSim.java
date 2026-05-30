package com.github.jkaste03.uefaccsim;

import java.nio.file.Paths;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

import com.github.jkaste03.uefaccsim.model.competition.Rounds;
import com.github.jkaste03.uefaccsim.reporting.ClubReportWriter;
import com.github.jkaste03.uefaccsim.reporting.StatsAggregator;

/**
 * Entry point class for running multiple independent UEFA club competition
 * simulations in parallel. A single baseline Rounds instance is created up
 * front, and deep-copied per simulation task to guarantee isolation of mutable
 * state while leveraging the Fork/Join common pool via a parallel IntStream.
 * The number of simulations is controlled by the fixed {@code SIMS} constant.
 * Statistics are collected in thread-local aggregators and merged into a final
 * aggregator after all simulation tasks complete.
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
 * <li>Get the current thread's StatsAggregator from a ThreadLocal and attach it
 * to the copied rounds instance.</li>
 * <li>Execute the simulation via {@code Rounds.run(taskName)}.</li>
 * </ul>
 * </li>
 * <li>Merge all per-thread aggregators into one final StatsAggregator.</li>
 * </ol>
 */
public class UefaCCSim {
    private static final int SIMS = 10;

    /**
     * Application entry point that performs a fixed number of independent
     * tournament simulations in parallel using Java's Fork/Join common pool.
     *
     * Workflow:
     * <ol>
     * <li>Instantiate a baseline Rounds object that serves as the template for each
     * simulation.</li>
     * <li>Create one ThreadLocal StatsAggregator per worker thread and keep all
     * created instances in a concurrent collection for final merge.</li>
     * <li>Measure wall-clock execution time around the simulation phase.</li>
     * <li>Execute <code>IntStream.range(0, SIMS).parallel()</code> so each
     * simulation index is processed concurrently.</li>
     * <li>For each simulation:
     * <ul>
     * <li>Create an isolated deep copy of the baseline Rounds instance (to avoid
     * shared mutable state).</li>
     * <li>Assign a task name (e.g., "Sim-1") for traceability.</li>
     * <li>Resolve the current thread's StatsAggregator via
     * <code>ThreadLocal.get()</code>.</li>
     * <li>Attach that aggregator to the copied Rounds instance.</li>
     * <li>Invoke <code>run(...)</code> on the copied Rounds.</li>
     * </ul>
     * </li>
     * <li>Merge all per-thread aggregators into a final aggregator that is used for
     * reporting.</li>
     * </ol>
     *
     * @param args currently unused
     */
    public static void main(String[] args) {
        // Create a new instance of Rounds
        Rounds rounds = new Rounds();

        // Number of simulations
        System.out.println("Simulations: " + SIMS);

        long startTime = System.currentTimeMillis();

        // Thread-safe collection to hold all StatsAggregators created by worker
        // threads.
        ConcurrentLinkedQueue<StatsAggregator> threadAggregators = new ConcurrentLinkedQueue<>();
        // ThreadLocal to provide each worker thread with its own StatsAggregator
        // instance. Each instance is created on first access and added to the
        // threadAggregators collection for later merging.
        ThreadLocal<StatsAggregator> localAggregator = ThreadLocal.withInitial(() -> {
            StatsAggregator statsAggregator = new StatsAggregator();
            threadAggregators.add(statsAggregator);
            return statsAggregator;
        });

        // Atomic counter to track completed simulations for progress reporting.
        AtomicInteger completedSimulations = new AtomicInteger();
        // Determine how often to print progress updates.
        int progressStep = Math.max(1, SIMS / 50);

        // Run simulations in parallel, each with its own deep copy of the baseline
        // Rounds and its own StatsAggregator from the ThreadLocal.
        IntStream.range(0, SIMS)
                .parallel()
                .forEach(i -> {
                    String taskName = "Sim-" + (i + 1);
                    Rounds copiedRounds = deepCopy(rounds);

                    StatsAggregator localStatsAggregator = localAggregator.get();

                    copiedRounds.attachStatsAggregator(localStatsAggregator);
                    copiedRounds.run(taskName);

                    // Increment the completed simulations counter and print progress at defined
                    // intervals.
                    int completed = completedSimulations.incrementAndGet();
                    if (completed % progressStep == 0 || completed == SIMS) {
                        int percent = (completed * 100) / SIMS;
                        System.out.printf("Progress: %d/%d (%d%%)%n", completed, SIMS, percent);
                    }
                });

        // Final stats aggregator that is populated with data from all worker threads.
        StatsAggregator finalStatsAggregator = new StatsAggregator();

        // Merge stats from all worker threads into the final aggregator.
        threadAggregators.forEach(finalStatsAggregator::mergeFrom);

        long endTime = System.currentTimeMillis();
        System.out.println("Total time: " + (endTime - startTime) + " ms");

        // Write club reports based on the aggregated statistics.
        new ClubReportWriter(Paths.get("temp", "club-reports")).writeClubReports(finalStatsAggregator, rounds);
    }

    /**
     * Creates a deep copy of the given object using serialization.
     * The entire object graph must be serializable.
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