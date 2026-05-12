package com.github.jkaste03.uefaccsim.model.competition;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;

public class KnockoutRoundPlayoff extends PostLeagueKnockoutRound {

    private static final int CLUB_COUNT = 16;
    private static final int CLUBS_PER_SIDE = CLUB_COUNT / 2;
    private static final int PAIR_SIZE = 2;
    private static final int INF = 1_000_000_000;

    // Prioritize minimizing number of moved clubs, then minimizing movement
    // distance.
    private static final int MOVE_PENALTY = 100;

    public KnockoutRoundPlayoff(Tournament tournament) {
        super(tournament, RoundType.KO_ROUND_PLAYOFF);
    }

    public KnockoutRoundPlayoff(Tournament tournament, boolean isSingleLegged) {
        super(tournament, RoundType.KO_ROUND_PLAYOFF, isSingleLegged);
    }

    public int getExpectedClubCount() {
        return CLUB_COUNT;
    }

    @Override
    public void draw() {
        long start = System.nanoTime();

        if (clubSlots.size() != CLUB_COUNT) {
            throw new IllegalStateException(
                    "Knockout round playoffs require exactly " + CLUB_COUNT + " clubs, got " + clubSlots.size());
        }

        List<ClubSlot> seeded = clubSlots.subList(0, CLUBS_PER_SIDE); // 9-16
        List<ClubSlot> unseeded = clubSlots.subList(CLUBS_PER_SIDE, CLUB_COUNT); // 17-24

        // Fast path: keep all clubs in their default cross-block pairings when
        // possible.
        // This is the common case and avoids the DP solve entirely.
        int[] assignment = new int[CLUBS_PER_SIDE];
        if (tryDrawWithinDefaultBlocks(seeded, unseeded, assignment)) {
            ties.clear();
            for (int i = 0; i < CLUBS_PER_SIDE; i++) {
                ClubSlot s = seeded.get(i);
                ClubSlot u = unseeded.get(assignment[i]);
                ties.add(new KnockoutTie(u, s, tournament, isSingleLegged));
            }
            // long end = System.nanoTime();
            // long duration = end - start;
            // System.out.println("Draw took: " + duration / 1_000_000 + " ms for " +
            // getName());
            return;
        }

        boolean[][] legal = new boolean[CLUBS_PER_SIDE][CLUBS_PER_SIDE];
        int[][] edgeCost = new int[CLUBS_PER_SIDE][CLUBS_PER_SIDE];
        for (int i = 0; i < CLUBS_PER_SIDE; i++) {
            int seededPosition = 9 + i;
            int seededPair = i / PAIR_SIZE;
            int preferredUnseededLow = 23 - (2 * seededPair);
            int preferredUnseededHigh = preferredUnseededLow + 1;
            for (int j = 0; j < CLUBS_PER_SIDE; j++) {
                int unseededPosition = 17 + j;
                int unseededPair = j / PAIR_SIZE;
                int preferredSeededLow = 15 - (2 * unseededPair);
                int preferredSeededHigh = preferredSeededLow + 1;

                // Symmetric placement distance:
                // - seeded side: how far this unseeded position is from the seeded club's
                // default unseeded pair in the 17-24 bracket.
                // - unseeded side: how far this seeded position is from the unseeded club's
                // default seeded pair in the 9-16 bracket.
                int seededPerspectiveDistance = Math.min(Math.abs(unseededPosition - preferredUnseededLow),
                        Math.abs(unseededPosition - preferredUnseededHigh));
                int unseededPerspectiveDistance = Math.min(Math.abs(seededPosition - preferredSeededLow),
                        Math.abs(seededPosition - preferredSeededHigh));
                int distance = seededPerspectiveDistance + unseededPerspectiveDistance;

                legal[i][j] = !isIllegalTie(seeded.get(i), unseeded.get(j));
                edgeCost[i][j] = (distance == 0 ? 0 : MOVE_PENALTY) + distance;
            }
        }

        int maxMask = 1 << CLUBS_PER_SIDE;
        int[] memo = new int[maxMask];
        Arrays.fill(memo, Integer.MIN_VALUE);

        int optimalCost = minCostFromMask(0, legal, edgeCost, memo);
        if (optimalCost >= INF) {
            throw new IllegalStateException("Could not construct a fully legal knockout playoff draw.");
        }

        buildRandomOptimalAssignment(0, legal, edgeCost, memo, assignment);

        ties.clear();
        for (int i = 0; i < CLUBS_PER_SIDE; i++) {
            ClubSlot s = seeded.get(i);
            ClubSlot u = unseeded.get(assignment[i]);
            ties.add(new KnockoutTie(u, s, tournament, isSingleLegged));
        }
        long end = System.nanoTime();
        long duration = end - start;
        System.out.println("Draw took: " + duration / 1_000_000 + " ms for " + getName() + " (alternative rules)");
        printClubSlotList();
        ties.forEach(t -> System.out.println(t.toCompactString()));
    }

    private boolean tryDrawWithinDefaultBlocks(List<ClubSlot> seeded, List<ClubSlot> unseeded, int[] assignment) {
        for (int block = 0; block < CLUBS_PER_SIDE / PAIR_SIZE; block++) {
            int seededA = block * PAIR_SIZE;
            int seededB = seededA + 1;

            // Block mapping per UEFA playoff bracket:
            // 9-10 vs 23-24, 11-12 vs 21-22, 13-14 vs 19-20, 15-16 vs 17-18.
            int unseededA = (3 - block) * PAIR_SIZE;
            int unseededB = unseededA + 1;

            boolean straight = !isIllegalTie(seeded.get(seededA), unseeded.get(unseededA))
                    && !isIllegalTie(seeded.get(seededB), unseeded.get(unseededB));
            boolean crossed = !isIllegalTie(seeded.get(seededA), unseeded.get(unseededB))
                    && !isIllegalTie(seeded.get(seededB), unseeded.get(unseededA));

            if (!straight && !crossed) {
                return false;
            }

            if (straight && crossed) {
                if (ThreadLocalRandom.current().nextBoolean()) {
                    assignment[seededA] = unseededA;
                    assignment[seededB] = unseededB;
                } else {
                    assignment[seededA] = unseededB;
                    assignment[seededB] = unseededA;
                }
            } else if (straight) {
                assignment[seededA] = unseededA;
                assignment[seededB] = unseededB;
            } else {
                assignment[seededA] = unseededB;
                assignment[seededB] = unseededA;
            }
        }

        return true;
    }

    private int minCostFromMask(int usedMask, boolean[][] legal, int[][] edgeCost, int[] memo) {
        int cached = memo[usedMask];
        if (cached != Integer.MIN_VALUE) {
            return cached;
        }

        int seededIndex = Integer.bitCount(usedMask);
        if (seededIndex == CLUBS_PER_SIDE) {
            memo[usedMask] = 0;
            return 0;
        }

        int best = INF;
        for (int j = 0; j < CLUBS_PER_SIDE; j++) {
            int bit = 1 << j;
            if ((usedMask & bit) != 0 || !legal[seededIndex][j]) {
                continue;
            }
            int tail = minCostFromMask(usedMask | bit, legal, edgeCost, memo);
            if (tail >= INF) {
                continue;
            }
            int candidate = edgeCost[seededIndex][j] + tail;
            if (candidate < best) {
                best = candidate;
            }
        }

        memo[usedMask] = best;
        return best;
    }

    private void buildRandomOptimalAssignment(int usedMask, boolean[][] legal, int[][] edgeCost, int[] memo,
            int[] assignment) {
        int seededIndex = Integer.bitCount(usedMask);
        if (seededIndex == CLUBS_PER_SIDE) {
            return;
        }

        int best = memo[usedMask];
        int[] candidates = new int[CLUBS_PER_SIDE];
        int count = 0;

        for (int j = 0; j < CLUBS_PER_SIDE; j++) {
            int bit = 1 << j;
            if ((usedMask & bit) != 0 || !legal[seededIndex][j]) {
                continue;
            }
            int nextMask = usedMask | bit;
            int tail = memo[nextMask];
            if (tail == Integer.MIN_VALUE) {
                tail = minCostFromMask(nextMask, legal, edgeCost, memo);
            }
            if (tail >= INF) {
                continue;
            }
            int total = edgeCost[seededIndex][j] + tail;
            if (total == best) {
                candidates[count++] = j;
            }
        }

        if (count == 0) {
            throw new IllegalStateException("Internal error while reconstructing knockout playoff draw.");
        }

        int chosen = candidates[ThreadLocalRandom.current().nextInt(count)];
        assignment[seededIndex] = chosen;
        buildRandomOptimalAssignment(usedMask | (1 << chosen), legal, edgeCost, memo, assignment);

    }
}
