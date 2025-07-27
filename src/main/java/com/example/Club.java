package com.example;

/**
 * Represents a football club with a name and ranking.
 *
 * <p>
 * Instances of this class store the club's name and its ranking coefficient.
 * </p>
 *
 * @author jkaste03
 */
public class Club {

    private static int id_counter = 0;
    private int id;
    private String name;
    private float ranking;

    public Club(String name, float ranking) {
        this.id = id_counter++;
        this.name = name;
        this.ranking = ranking;
        ClubRepository.addClub(this);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the ranking of the club.
     *
     * @param callerCompLevel the competition level of the caller is not relevant
     *                        for this class
     * @return the ranking value of the club
     */
    public float getRanking(int callerCompLevel) {
        return ranking;
    }

    public double getEloRating() {
        return ClubEloDataLoader.getEloRating(id);
    }

    @Override
    public String toString() {
        return "Club{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", coefficientRanking=" + ranking +
                ", eloRating=" + getEloRating() +
                '}';
    }
}
