package com.example;

/**
 * Represents a tie between two clubs at a specific competition level.
 * <p>
 * A {@code Tie} consists of two {@link Club} instances and an integer
 * indicating
 * the competition level at which the tie occurs. The class provides methods to
 * access the clubs and competition level, as well as to calculate a ranking
 * value for the tie relative to a given competition level.
 * <p>
 * The ranking calculation considers whether the tie's competition level is
 * higher
 * or lower than the caller's level, returning either the best or worst ranking
 * among the two clubs accordingly.
 *
 * @author jkaste03
 */
public class Tie extends ClubSlot {
    private ClubSlot clubSlot1;
    private ClubSlot clubSlot2;
    private int compLevel;
    private Integer club1Goals;
    private Integer club2Goals;

    public Tie(ClubSlot clubSlot1, ClubSlot clubSlot2, int compLevel) {
        this.clubSlot1 = clubSlot1;
        this.clubSlot2 = clubSlot2;
        this.compLevel = compLevel;
    }

    public Tie(ClubSlot clubSlot1, ClubSlot clubSlot2, int compLevel, Integer club1Goals, Integer club2Goals) {
        this.clubSlot1 = clubSlot1;
        this.clubSlot2 = clubSlot2;
        this.compLevel = compLevel;
        this.club1Goals = club1Goals;
        this.club2Goals = club2Goals;
    }

    public int getCompLevel() {
        return compLevel;
    }

    public ClubSlot getClubSlot1() {
        return clubSlot1;
    }

    public ClubSlot getClubSlot2() {
        return clubSlot2;
    }

    public Integer getClub1Goals() {
        return club1Goals;
    }

    public Integer getClub2Goals() {
        return club2Goals;
    }

    /**
     * Calculates the ranking for this tie based on the competition level of the
     * caller.
     * <p>
     * If this tie's competition level is higher than the caller's, returns the
     * worst ranking of the two clubs. Otherwise, returns the best ranking.
     *
     * @param callerCompLevel the competition level from which the ranking is
     *                        requested
     * @return the ranking value for this tie in relation to the caller's
     *         competition level
     */
    @Override
    protected float getRanking(int callerCompLevel) {
        if (this.compLevel < callerCompLevel) {
            return Math.max(clubSlot1.getRanking(callerCompLevel), clubSlot2.getRanking(callerCompLevel));
        } else {
            return Math.min(clubSlot1.getRanking(callerCompLevel), clubSlot2.getRanking(callerCompLevel));
        }
    }

    public void play() {
        // 1) Hent Elo for begge klubber
        double elo1 = ((Club) clubSlot1).getEloRating();
        double elo2 = ((Club) clubSlot2).getEloRating();

        // 2) Definer hjemmebanefordel (samme for alle klubber)
        final double HFA = 100; // kan justeres

        // 3) Beregn sannsynlighet uten hjemmebanefordel:
        // P(neutral) = 1 / (10^(-(elo1 - elo2)/400) + 1)
        double drNeutral = elo1 - elo2;
        double pNeutral = 1.0 / (Math.pow(10, -drNeutral / 400.0) + 1.0);

        // Hvis club1Goals er null, spiller vi begge ben samtidig (ingen
        // hjemmebanefordel)
        if (club1Goals == null) {
            System.out.printf("Sannsynlighet for at %s går videre: %.2f%%%n",
                    ((Club) clubSlot1).getName(), pNeutral * 100);
            System.out.printf("Sannsynlighet for at %s går videre: %.2f%%%n",
                    ((Club) clubSlot2).getName(), (1 - pNeutral) * 100);

        } else {
            // Første kamp er allerede spilt, vi har resultater i club1Goals/club2Goals.
            // Nå spiller vi én returkamp, der clubSlot2 er hjemmelag:
            // 4) Juster Elo-differanse med hjemmebanefordel for returkampen
            double drReturn = (elo2 + HFA) - elo1;
            double pHomeWin = 1.0 / (Math.pow(10, -drReturn / 400.0) + 1.0);

            // 5) Beregn sannsynlighet for at hver klubb går videre,
            // basert på kjent første kamp og sannsynlighet for returkamp.
            // Vi antar at dersom man vinner returkampen, man går videre,
            // ved uavgjort tar vi bortemålsregelen:
            int g1 = club1Goals;
            int g2 = club2Goals;

            // Sannsynlighet for at returkampen ender uavgjort:
            double pDrawReturn = 1 - pHomeWin - (1 - pHomeWin);
            // (her forenklet til ingen mulighet for uavgjort; kan settes til 0)

            // Sjekk aggregate-situasjonen:
            // Hvis g1 + x < g2 + y => club2 videre; > => club1;
            // ved uavgjort avgjøres det på bortemål, altså
            // club1 bortemål = x, club2 bortemål = y (x = number of goals clubSlot1 scorer
            // hjemme)

            // For enkelhets skyld, vi antar at:
            // - Med sjanse pHomeWin vinner hjemmelaget (clubSlot2) med et bortemål mer →
            // club2 går videre
            // - Med sjanse pHome lose (1-pHomeWin) vinner borte (clubSlot1) → club1 går
            // videre
            // - Vi ignorerer returkamp-uavgjort i denne forenklingen.

            double pClub2Advance = pHomeWin;
            double pClub1Advance = 1 - pHomeWin;

            System.out.printf("Basert på første kamp (%d–%d) og returkamp:\n", g1, g2);
            System.out.printf("  %s går videre med sannsynlighet: %.2f%%%n",
                    ((Club) clubSlot1).getName(), pClub1Advance * 100);
            System.out.printf("  %s går videre med sannsynlighet: %.2f%%%n",
                    ((Club) clubSlot2).getName(), pClub2Advance * 100);
        }
    }

    @Override
    public String toString() {
        return "Tie{" +
                "compLevel=" + compLevel +
                ", clubSlot1=" + clubSlot1 +
                ", clubSlot2=" + clubSlot2 +
                ", club1Goals=" + club1Goals +
                ", club2Goals=" + club2Goals +
                '}';
    }
}
