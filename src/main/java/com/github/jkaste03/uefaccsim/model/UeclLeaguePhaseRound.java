package com.github.jkaste03.uefaccsim.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.github.jkaste03.uefaccsim.enums.Country;
import com.github.jkaste03.uefaccsim.enums.Tournament;

/**
 * Class representing the league phase in the UEFA Conference League.
 * This class handles the league phase rounds where clubs compete in a league
 * format specific to the Conference League.
 */
public class UeclLeaguePhaseRound extends LeaguePhaseRound {

    /**
     * The number of pots used for seeding clubs in the league phase.
     */
    private final static int POT_COUNT = 6;
    Random rnd = new Random();

    /**
     * Constructs a ConferenceLeaguePhaseRound.
     */
    public UeclLeaguePhaseRound() {
        super(Tournament.CONFERENCE_LEAGUE);
    }

    /**
     * Returns the number of pots used in the league phase round.
     *
     * @return the pot count as an integer
     */
    @Override
    protected int getPotCount() {
        return POT_COUNT;
    }

    // TODO: Missing javadoc
    @Override
    protected void draw() {
        if (pots.size() != POT_COUNT)
            throw new IllegalStateException("Expected " + POT_COUNT + " pots before draw()");

        // --- bygg indeks/mapper ---
        List<ClubSlot> allClubs = pots.stream().flatMap(p -> p.clubs().stream()).collect(Collectors.toList());
        Map<ClubSlot, Integer> idxOf = new HashMap<>();
        ClubSlot[] idxToClub = new ClubSlot[allClubs.size()];
        for (int i = 0; i < allClubs.size(); ++i) {
            idxOf.put(allClubs.get(i), i);
            idxToClub[i] = allClubs.get(i);
        }

        // pot -> liste av indekser (0..35)
        List<List<Integer>> potIndices = new ArrayList<>(POT_COUNT);
        for (int p = 0; p < POT_COUNT; ++p) {
            potIndices.add(pots.get(p).clubs().stream().map(idxOf::get)
                    .collect(Collectors.toCollection(ArrayList::new)));
        }

        // country counters (foreign only)
        Map<Integer, Map<Country, Integer>> countryCounts = new HashMap<>();
        for (int i = 0; i < idxToClub.length; ++i)
            countryCounts.put(i, new HashMap<>());

        java.util.function.BiPredicate<Integer, Integer> canAddPair = (aIdx, bIdx) -> {
            ClubSlot a = idxToClub[aIdx];
            ClubSlot b = idxToClub[bIdx];
            for (Country bc : b.getCountries()) {
                if (a.getCountries().contains(bc))
                    continue;
                int cur = countryCounts.get(aIdx).getOrDefault(bc, 0);
                if (cur >= 2)
                    return false;
            }
            return true;
        };
        java.util.function.BiConsumer<Integer, Integer> addPair = (aIdx, bIdx) -> {
            ClubSlot a = idxToClub[aIdx];
            ClubSlot b = idxToClub[bIdx];
            for (Country bc : b.getCountries()) {
                if (a.getCountries().contains(bc))
                    continue;
                Map<Country, Integer> m = countryCounts.get(aIdx);
                m.put(bc, m.getOrDefault(bc, 0) + 1);
            }
        };
        java.util.function.BiConsumer<Integer, Integer> removePair = (aIdx, bIdx) -> {
            ClubSlot a = idxToClub[aIdx];
            ClubSlot b = idxToClub[bIdx];
            for (Country bc : b.getCountries()) {
                if (a.getCountries().contains(bc))
                    continue;
                Map<Country, Integer> m = countryCounts.get(aIdx);
                int v = m.getOrDefault(bc, 0);
                if (v <= 1)
                    m.remove(bc);
                else
                    m.put(bc, v - 1);
            }
        };

        // alle unordered pot-par (i<=j)
        List<int[]> unorderedPairs = new ArrayList<>();
        for (int i = 0; i < POT_COUNT; ++i)
            for (int j = i; j < POT_COUNT; ++j)
                unorderedPairs.add(new int[] { i, j });

        // cached intra matchinger per pot (pruned for political forbids,
        // and constrained to minimal necessary same-country edges)
        List<List<int[]>> intraCache = new ArrayList<>();
        for (int p = 0; p < POT_COUNT; ++p) {
            List<int[]> list = new ArrayList<>();
            List<Integer> members = potIndices.get(p);
            boolean[] used = new boolean[6];
            int[] cur = new int[6];

            // Temporary store of all non-political perfect pairings and counts of
            // same-country edges
            List<int[]> allPairings = new ArrayList<>();
            List<Integer> sameCounts = new ArrayList<>();

            class Gen {
                void dfs(int pos) {
                    int i = 0;
                    while (i < 6 && used[i])
                        i++;
                    if (i == 6) {
                        int[] pairFlat = new int[6];
                        for (int k = 0; k < 6; ++k)
                            pairFlat[k] = members.get(cur[k]);
                        // check for political forbids and count same-country edges
                        boolean ok = true;
                        int same = 0;
                        for (int k = 0; k < 6; k += 2) {
                            ClubSlot a = idxToClub[pairFlat[k]];
                            ClubSlot b = idxToClub[pairFlat[k + 1]];
                            Country ca = a.getCountries().get(0);
                            Country cb = b.getCountries().get(0);
                            boolean sameCountry = (ca == cb);
                            boolean illegal = isIllegalTie(a, b);
                            boolean political = illegal && !sameCountry;
                            if (political) {
                                ok = false;
                                break;
                            }
                            if (sameCountry)
                                same++;
                        }
                        if (ok) {
                            allPairings.add(pairFlat);
                            sameCounts.add(same);
                        }
                        return;
                    }
                    used[i] = true;
                    cur[pos] = i;
                    for (int j = i + 1; j < 6; ++j)
                        if (!used[j]) {
                            used[j] = true;
                            cur[pos + 1] = j;
                            dfs(pos + 2);
                            used[j] = false;
                        }
                    used[i] = false;
                }
            }
            new Gen().dfs(0);

            if (allPairings.isEmpty()) {
                // ingen mulig pairing uten å bryte politiske forbud -> umulig
                throw new IllegalStateException("No valid (non-political) intra-pot pairing for pot " + p);
            }

            // Finn minste antall same-country edges blant gyldige pairinger
            int minSame = Integer.MAX_VALUE;
            for (int v : sameCounts)
                minSame = Math.min(minSame, v);

            // Behold kun pairinger som har akkurat minSame same-country edges
            for (int i = 0; i < allPairings.size(); ++i) {
                if (sameCounts.get(i) == minSame)
                    list.add(allPairings.get(i));
            }

            intraCache.add(list);
        }

        // map pairKey -> liste med edges (u,v) (u<v)
        Map<String, List<int[]>> pairEdges = new HashMap<>();

        // rekursiv solver for unorderedPairs (bygger pairEdges og oppdaterer
        // countryCounts)
        class Solver {
            boolean solve(int idx) {
                if (idx >= unorderedPairs.size())
                    return true;
                int pa = unorderedPairs.get(idx)[0], pb = unorderedPairs.get(idx)[1];
                String key = pa + "-" + pb;
                if (pa == pb) {
                    List<int[]> candidates = new ArrayList<>(intraCache.get(pa));
                    Collections.shuffle(candidates, rnd);
                    for (int[] flat : candidates) {
                        boolean okAll = true;
                        for (int k = 0; k < 6; k += 2) {
                            int u = flat[k], v = flat[k + 1];
                            if (!canAddPair.test(u, v) || !canAddPair.test(v, u)) {
                                okAll = false;
                                break;
                            }
                        }
                        if (!okAll)
                            continue;
                        for (int k = 0; k < 6; k += 2) {
                            int u = flat[k], v = flat[k + 1];
                            addPair.accept(u, v);
                            addPair.accept(v, u);
                        }
                        List<int[]> edges = new ArrayList<>();
                        for (int k = 0; k < 6; k += 2) {
                            int u = flat[k], v = flat[k + 1];
                            int a = Math.min(u, v), b = Math.max(u, v);
                            edges.add(new int[] { a, b });
                        }
                        pairEdges.put(key, edges);
                        if (solve(idx + 1))
                            return true;
                        pairEdges.remove(key);
                        for (int k = 0; k < 6; k += 2) {
                            int u = flat[k], v = flat[k + 1];
                            removePair.accept(u, v);
                            removePair.accept(v, u);
                        }
                    }
                    return false;
                } else {
                    List<Integer> A = potIndices.get(pa);
                    List<Integer> B = potIndices.get(pb);
                    int n = A.size();
                    boolean[][] allowed = new boolean[n][n];
                    for (int i = 0; i < n; ++i)
                        for (int j = 0; j < n; ++j) {
                            ClubSlot ca = idxToClub[A.get(i)];
                            ClubSlot cb = idxToClub[B.get(j)];
                            if (isIllegalTie(ca, cb))
                                continue;
                            if (!canAddPair.test(A.get(i), B.get(j)) || !canAddPair.test(B.get(j), A.get(i)))
                                continue;
                            allowed[i][j] = true;
                        }
                    int[] matchToB = new int[n], matchToA = new int[n];
                    java.util.Arrays.fill(matchToB, -1);
                    java.util.Arrays.fill(matchToA, -1);
                    boolean ok = true;
                    for (int i = 0; i < n && ok; ++i) {
                        boolean[] seen = new boolean[n];
                        ok = dfsMatch(i, allowed, seen, matchToB, matchToA, rnd);
                    }
                    if (!ok)
                        return false;
                    List<int[]> edges = new ArrayList<>();
                    for (int i = 0; i < n; ++i) {
                        int u = A.get(i), v = B.get(matchToB[i]);
                        addPair.accept(u, v);
                        addPair.accept(v, u);
                        int a = Math.min(u, v), b = Math.max(u, v);
                        edges.add(new int[] { a, b });
                    }
                    pairEdges.put(key, edges);
                    if (solve(idx + 1))
                        return true;
                    pairEdges.remove(key);
                    for (int i = 0; i < n; ++i) {
                        int u = A.get(i), v = B.get(matchToB[i]);
                        removePair.accept(u, v);
                        removePair.accept(v, u);
                    }
                    return false;
                }
            }

            boolean dfsMatch(int aIdx, boolean[][] allowed, boolean[] seen, int[] matchToB, int[] matchToA, Random r) {
                int n = allowed.length;
                int[] order = new int[n];
                for (int i = 0; i < n; ++i)
                    order[i] = i;
                for (int i = n - 1; i > 0; --i) {
                    int j = r.nextInt(i + 1);
                    int tmp = order[i];
                    order[i] = order[j];
                    order[j] = tmp;
                }
                for (int bi : order) {
                    if (!allowed[aIdx][bi] || seen[bi])
                        continue;
                    seen[bi] = true;
                    if (matchToA[bi] == -1 || dfsMatch(matchToA[bi], allowed, seen, matchToB, matchToA, r)) {
                        matchToB[aIdx] = bi;
                        matchToA[bi] = aIdx;
                        return true;
                    }
                }
                return false;
            }
        }

        Solver s = new Solver();
        boolean solved = s.solve(0);
        if (!solved) {
            pots.forEach(p -> System.out.println(p.toCompactString()));
            throw new RuntimeException("Conference League draw failed: no valid configuration found");
        }

        // --- samle alle edges (108) ---
        List<int[]> allEdges = new ArrayList<>();
        for (int[] pair : unorderedPairs) {
            String k = pair[0] + "-" + pair[1];
            List<int[]> es = pairEdges.get(k);
            if (es == null)
                throw new IllegalStateException("Missing edges for " + k);
            allEdges.addAll(es);
        }

        // --- orientering: bygg lineært system over GF(2) ---
        int m = allEdges.size(); // 108
        // map pair index -> endpoints as stored (u,v)
        // precompute potOfIndex for quick lookup
        int[] potOf = new int[idxToClub.length];
        for (int p = 0; p < POT_COUNT; ++p)
            for (int id : potIndices.get(p))
                potOf[id] = p;

        // adjacency from node -> list of edge indices
        List<List<Integer>> incident = new ArrayList<>(idxToClub.length);
        for (int i = 0; i < idxToClub.length; ++i)
            incident.add(new ArrayList<>());
        for (int ei = 0; ei < m; ++ei) {
            int u = allEdges.get(ei)[0], v = allEdges.get(ei)[1];
            incident.get(u).add(ei);
            incident.get(v).add(ei);
        }

        // equations: rows = numClubs * numGroups(3)
        List<BitSet> eqCoeffs = new ArrayList<>();
        List<Integer> eqConst = new ArrayList<>();
        int[][] groups = new int[][] { { 0, 1 }, { 2, 3 }, { 4, 5 } };

        for (int team = 0; team < idxToClub.length; ++team) {
            for (int[] g : groups) {
                int g0 = g[0], g1 = g[1];
                // find two incident edges where the opponent's pot is in {g0,g1}
                int[] two = new int[2];
                int found = 0;
                for (int ei : incident.get(team)) {
                    int a = allEdges.get(ei)[0], b = allEdges.get(ei)[1];
                    int other = (a == team) ? b : a;
                    int otherPot = potOf[other];
                    if (otherPot == g0 || otherPot == g1) {
                        if (found < 2)
                            two[found++] = ei;
                    }
                }
                if (found != 2) {
                    // dette betyr at løsningen av matchings er feil — burde ikke skje
                    throw new IllegalStateException("Club " + idxToClub[team] + " does not have two opponents in group "
                            + g0 + "/" + g1 + " (found " + found + ")");
                }
                BitSet bs = new BitSet(m);
                int c = 0;
                for (int k = 0; k < 2; ++k) {
                    int ei = two[k];
                    int a = allEdges.get(ei)[0];
                    // isHome(team, ei) = (team == a) ? (1 - x_e) : x_e
                    // => coefficient on x_e is 1 in both cases; constant c increments by 1 if
                    // team==a
                    bs.set(ei);
                    if (team == a)
                        c ^= 1; // toggle constant
                }
                int rhs = (1 ^ c); // sum coeff*x_e == rhs (mod2)
                eqCoeffs.add(bs);
                eqConst.add(rhs);
            }
        }

        // Solve linear system over GF(2)
        BitSet[] A = new BitSet[eqCoeffs.size()];
        int[] B = new int[eqCoeffs.size()];
        for (int i = 0; i < eqCoeffs.size(); ++i) {
            A[i] = eqCoeffs.get(i);
            B[i] = eqConst.get(i);
        }

        int[] sol = gaussGF2(A, B, m);
        // If no solution, perform a number of randomized restarts of the entire
        // matching process to break deadlocks
        int restart = 0;
        while (sol == null && restart < 40) {
            restart++;
            pairEdges.clear();
            // reset countryCounts
            for (int i = 0; i < idxToClub.length; ++i)
                countryCounts.get(i).clear();
            // rerun solver with new RNG seed
            rnd = new Random(rnd.nextLong());
            s = new Solver();
            if (!s.solve(0))
                continue;
            // rebuild allEdges & incident
            allEdges.clear();
            for (int[] pair : unorderedPairs) {
                String k = pair[0] + "-" + pair[1];
                List<int[]> es = pairEdges.get(k);
                if (es == null)
                    throw new IllegalStateException("Missing edges after restart for " + k);
                allEdges.addAll(es);
            }
            m = allEdges.size();
            incident = new ArrayList<>(idxToClub.length);
            for (int i = 0; i < idxToClub.length; ++i)
                incident.add(new ArrayList<>());
            for (int ei = 0; ei < m; ++ei) {
                int u = allEdges.get(ei)[0], v = allEdges.get(ei)[1];
                incident.get(u).add(ei);
                incident.get(v).add(ei);
            }
            // rebuild eqs
            eqCoeffs.clear();
            eqConst.clear();
            for (int team = 0; team < idxToClub.length; ++team) {
                for (int[] g : groups) {
                    int g0 = g[0], g1 = g[1];
                    int[] two = new int[2];
                    int found = 0;
                    for (int ei : incident.get(team)) {
                        int a = allEdges.get(ei)[0], b = allEdges.get(ei)[1];
                        int other = (a == team) ? b : a;
                        int otherPot = potOf[other];
                        if (otherPot == g0 || otherPot == g1) {
                            if (found < 2)
                                two[found++] = ei;
                        }
                    }
                    if (found != 2) {
                        sol = null;
                        break;
                    }
                    BitSet bs = new BitSet(m);
                    int c = 0;
                    for (int k = 0; k < 2; ++k) {
                        int ei = two[k];
                        int a = allEdges.get(ei)[0];
                        bs.set(ei);
                        if (team == a)
                            c ^= 1;
                    }
                    int rhs = (1 ^ c);
                    eqCoeffs.add(bs);
                    eqConst.add(rhs);
                }
                if (sol == null && restart > 0 && eqCoeffs.size() == 0)
                    break;
            }
            A = new BitSet[eqCoeffs.size()];
            B = new int[eqCoeffs.size()];
            for (int i = 0; i < eqCoeffs.size(); ++i) {
                A[i] = eqCoeffs.get(i);
                B[i] = eqConst.get(i);
            }
            sol = gaussGF2(A, B, m);
        }
        if (sol == null)
            throw new RuntimeException("Failed to compute home/away orientation after restarts");

        // Build final ties from orientation (x_e solution)
        List<Tie> resultTies = new ArrayList<>();
        for (int ei = 0; ei < allEdges.size(); ++ei) {
            int u = allEdges.get(ei)[0], v = allEdges.get(ei)[1];
            // convention: if x_e == 0 -> keep stored order u home, v away; if x_e==1 ->
            // reversed
            int x = sol[ei];
            if (x == 0)
                resultTies.add(new SingleLeggedTie(idxToClub[u], idxToClub[v], tournament));
            else
                resultTies.add(new SingleLeggedTie(idxToClub[v], idxToClub[u], tournament));
        }

        ties = resultTies;

        // ties.forEach(t -> System.out.println(t.toCompactString()));
    }

    // --- Gaussian elim over GF(2) helper ---
    private int[] gaussGF2(BitSet[] Arows, int[] B, int nvars) {
        int rows = Arows.length;
        BitSet[] M = new BitSet[rows];
        for (int i = 0; i < rows; ++i)
            M[i] = (BitSet) Arows[i].clone();
        int[] rhs = Arrays.copyOf(B, B.length);
        int row = 0;
        int[] where = new int[nvars];
        Arrays.fill(where, -1);
        for (int col = 0; col < nvars && row < rows; ++col) {
            int sel = -1;
            for (int i = row; i < rows; ++i)
                if (M[i].get(col)) {
                    sel = i;
                    break;
                }
            if (sel == -1)
                continue;
            BitSet tmp = M[row];
            M[row] = M[sel];
            M[sel] = tmp;
            int tr = rhs[row];
            rhs[row] = rhs[sel];
            rhs[sel] = tr;
            where[col] = row;
            for (int i = 0; i < rows; ++i)
                if (i != row && M[i].get(col)) {
                    M[i].xor(M[row]);
                    rhs[i] ^= rhs[row];
                }
            row++;
        }
        for (int i = row; i < rows; ++i) {
            if (M[i].isEmpty() && rhs[i] == 1)
                return null;
        }
        int[] ans = new int[nvars];
        for (int i = 0; i < nvars; ++i) {
            if (where[i] != -1)
                ans[i] = rhs[where[i]];
            else
                ans[i] = 0;
        }
        return ans;
    }

}
