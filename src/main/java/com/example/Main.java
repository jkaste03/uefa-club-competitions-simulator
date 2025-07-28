package com.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.stream.IntStream;

public class Main {
    private static final int SIMS = 100_000;

    public static void main(String[] args) {
        // Create a new instance of Rounds
        Rounds rounds = new Rounds();

        // Antall simuleringer/oppgaver
        System.out.println("Simulations: " + SIMS);

        long startTime = System.currentTimeMillis();

        // Parallel løkke: hver iterasjon kjøres i Fork/Join‑poolens tråder
        IntStream.range(0, SIMS)
                .parallel() // gjør streamen parallell
                .forEach(i -> {
                    String taskName = "Sim-" + (i + 1);
                    // Hvis Rounds ikke er trådsikker: lag en kopi per oppgave
                    Rounds local = deepCopy(rounds);

                    local.run(taskName);
                });

        long endTime = System.currentTimeMillis();
        System.out.println("Total time: " + (endTime - startTime) + " ms");

        printClubSeedingStats();
    }

    private static void printClubSeedingStats() {
        System.out.println("UECL Playoff draw seeding:");

        // Betinget tabell
        System.out.println("\nConditional probabilities (given participation in playoff draw):");
        System.out.println("-----------------------------------------------------------------------------------");
        System.out.printf("%-21s %14s %18s%n", "Club", "Cond. Seeded", "Cond. Unseeded");
        System.out.println("-----------------------------------------------------------------------------------");
        ClubRepository.getAllClubs().stream()
                .filter(club -> club.getRanking(3) != -1)
                .sorted((c1, c2) -> {
                    int c1Seeded = c1.getTimesSeeded();
                    int c1Unseeded = c1.getTimesUnseeded();
                    int c1Participated = c1Seeded + c1Unseeded;
                    double c1CondSeed = c1Participated > 0 ? (double) c1Seeded / c1Participated : 0.0;
                    double c1CondUnseed = c1Participated > 0 ? (double) c1Unseeded / c1Participated : 0.0;

                    int c2Seeded = c2.getTimesSeeded();
                    int c2Unseeded = c2.getTimesUnseeded();
                    int c2Participated = c2Seeded + c2Unseeded;
                    double c2CondSeed = c2Participated > 0 ? (double) c2Seeded / c2Participated : 0.0;
                    double c2CondUnseed = c2Participated > 0 ? (double) c2Unseeded / c2Participated : 0.0;

                    // Hvis begge har 0% seeded, sorter på unseeded
                    if (c1CondSeed == 0.0 && c2CondSeed == 0.0) {
                        return Double.compare(c2CondUnseed, c1CondUnseed);
                    }
                    return Double.compare(c2CondSeed, c1CondSeed);
                })
                .forEach(club -> {
                    int seededCount = club.getTimesSeeded();
                    int unseededCount = club.getTimesUnseeded();
                    int participated = seededCount + unseededCount;

                    double condSeedPct = participated > 0
                            ? seededCount * 100.0 / participated
                            : 0.0;
                    double condUnseedPct = participated > 0
                            ? unseededCount * 100.0 / participated
                            : 0.0;

                    System.out.printf("%-21s %13.1f%% %17.1f%%%n",
                            club.getName(),
                            condSeedPct, condUnseedPct);
                });

        // Absolutt tabell
        System.out.println("\nAbsolute probabilities (out of all simulations):");
        System.out.println("---------------------------------------------------------------");
        System.out.printf("%-21s %12s %16s%n", "Club", "Abs. Seeded", "Abs. Unseeded");
        System.out.println("---------------------------------------------------------------");
        ClubRepository.getAllClubs().stream()
                .filter(club -> club.getRanking(3) != -1)
                .sorted((c1, c2) -> {
                    double c1AbsSeed = (double) c1.getTimesSeeded() / SIMS;
                    double c2AbsSeed = (double) c2.getTimesSeeded() / SIMS;
                    // Hvis begge har 0% seeded, sorter på unseeded
                    if ((c1AbsSeed == 0.0 && c2AbsSeed == 0.0) ||
                            (c1AbsSeed == 1.0 && c2AbsSeed == 1.0)) {
                        double c1AbsUnseed = (double) c1.getTimesUnseeded() / SIMS;
                        double c2AbsUnseed = (double) c2.getTimesUnseeded() / SIMS;
                        return Double.compare(c2AbsUnseed, c1AbsUnseed);
                    }
                    return Double.compare(c2AbsSeed, c1AbsSeed);
                })
                .forEach(club -> {
                    int seededCount = club.getTimesSeeded();
                    int unseededCount = club.getTimesUnseeded();

                    double absSeedPct = seededCount * 100.0 / SIMS;
                    double absUnseedPct = unseededCount * 100.0 / SIMS;

                    System.out.printf("%-21s %11.1f%% %15.1f%%%n",
                            club.getName(),
                            absSeedPct, absUnseedPct);
                });
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