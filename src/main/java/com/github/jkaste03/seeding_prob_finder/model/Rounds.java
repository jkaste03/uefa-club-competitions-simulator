package com.github.jkaste03.seeding_prob_finder.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.jkaste03.seeding_prob_finder.enums.Tournament;
import com.github.jkaste03.seeding_prob_finder.service.ClubEloDataLoader;

/**
 * The Rounds class is responsible for initializing, linking, and executing all
 * rounds for UEFA competitions. It sets up the rounds, linking each round to
 * define the progression sequence. This detailed simulation ensures that
 * seeding, draws, tie registrations, and match play are executed in an
 * organized manner.
 */
public class Rounds implements Serializable {
    // Declare qualifying rounds and league rounds for all competitions.
    private Round uclQ2, uelQ2, ueclQ2, uelQ3, ueclQ3, ueclPO;
    private List<Round> rounds;

    // Initialize the ClubEloDataLoader to fetch club elo ratings.
    private ClubEloDataLoader clubEloDataLoader = new ClubEloDataLoader();

    /**
     * Constructs all rounds for UEFA competitions, initializes club Elo API,
     * and sets up the interlink between rounds. This constructor prepares the
     * simulation by creating each qualifying and league phase round instance.
     */
    public Rounds() {
        uclQ2 = new Round(Tournament.CHAMPIONS_LEAGUE, 2);
        uelQ2 = new Round(Tournament.EUROPA_LEAGUE, 2);
        ueclQ2 = new Round(Tournament.CONFERENCE_LEAGUE, 2);

        uelQ3 = new Round(Tournament.EUROPA_LEAGUE, 3);
        ueclQ3 = new Round(Tournament.CONFERENCE_LEAGUE, 3);

        ueclPO = new Round(Tournament.CONFERENCE_LEAGUE, 4);

        // Aggregate all rounds into a list for streamlined processing.
        rounds = new ArrayList<>(
                Arrays.asList(uclQ2, uelQ2, ueclQ2, uelQ3, ueclQ3, ueclPO));

        // Initialize data for each round.
        // JsonDataLoader.loadDataForRounds(rounds);

        uclQ2.setNextSecondaryRnd(uelQ3);
        uelQ2.setNextRounds(uelQ3, ueclQ3);
        ueclQ2.setNextPrimaryRnd(ueclQ3);

        uelQ3.setNextSecondaryRnd(ueclPO);
        ueclQ3.setNextPrimaryRnd(ueclPO);

        // UCL Q2 ties
        ClubSlot uclQ2Tie1 = new ClubSlot(new ClubSlot("Brann", 189f, clubEloDataLoader), new ClubSlot("Salzburg", 44f, clubEloDataLoader), uclQ2.getTournament(), 1, 4);
        ClubSlot uclQ2Tie2 = new ClubSlot(new ClubSlot("Viktoria Plzen", 56f, clubEloDataLoader), new ClubSlot("Servette", 140f, clubEloDataLoader), uclQ2.getTournament(), 0, 1);
        ClubSlot uclQ2Tie3 = new ClubSlot(new ClubSlot("Rangers", 25f, clubEloDataLoader), new ClubSlot("Panathinaikos", 111f, clubEloDataLoader), uclQ2.getTournament(), 2, 0);

        uclQ2.addTies(new ArrayList<>(Arrays.asList(
                uclQ2Tie1,
                uclQ2Tie2,
                uclQ2Tie3)));

        // UEL Q2 ties
        ClubSlot uelQ2Tie1 = new ClubSlot(new ClubSlot("Banik Ostrava", 168f, clubEloDataLoader), new ClubSlot("Legia", 70f, clubEloDataLoader), uelQ2.getTournament(), 2, 2);
        ClubSlot uelQ2Tie2 = new ClubSlot(new ClubSlot("Sheriff Tiraspol", 88f, clubEloDataLoader), new ClubSlot("Utrecht", 125.1f, clubEloDataLoader), uelQ2.getTournament(), 1, 3);
        ClubSlot uelQ2Tie3 = new ClubSlot(new ClubSlot("Midtjylland", 67f, clubEloDataLoader), new ClubSlot("Hibernian", 207f, clubEloDataLoader), uelQ2.getTournament(), 1, 1);
        ClubSlot uelQ2Tie4 = new ClubSlot(new ClubSlot("Levski", 312f, clubEloDataLoader), new ClubSlot("Braga", 48f, clubEloDataLoader), uelQ2.getTournament(), 0, 0);
        ClubSlot uelQ2Tie5 = new ClubSlot(new ClubSlot("Anderlecht", 72f, clubEloDataLoader), new ClubSlot("Haecken", 211f, clubEloDataLoader), uelQ2.getTournament(), 1, 0);
        ClubSlot uelQ2Tie6 = new ClubSlot(new ClubSlot("Besiktas", 113f, clubEloDataLoader), new ClubSlot("Shakhtar", 41f, clubEloDataLoader), uelQ2.getTournament(), 2, 4);
        ClubSlot uelQ2Tie7 = new ClubSlot(new ClubSlot("Celje", 120f, clubEloDataLoader), new ClubSlot("Larnaca", 199f, clubEloDataLoader), uelQ2.getTournament(), 1, 1);
        ClubSlot uelQ2Tie8 = new ClubSlot(new ClubSlot("Lugano", 90f, clubEloDataLoader), new ClubSlot("CFR Cluj", 92f, clubEloDataLoader), uelQ2.getTournament(), 0, 0);

        uelQ2.addTies(new ArrayList<>(Arrays.asList(
                uelQ2Tie1,
                uelQ2Tie2,
                uelQ2Tie3,
                uelQ2Tie4,
                uelQ2Tie5,
                uelQ2Tie6,
                uelQ2Tie7,
                uelQ2Tie8)));

        // UECL Q2 ties
        // ClubSlot ueclQ2Tie1 = new ClubSlot(new ClubSlot("FC Infonet", 239f), new
        // ClubSlot("Saburtalo", 307f), ueclQ2.getCompLevel(), 1, 0);
        // ClubSlot ueclQ2Tie2 = new ClubSlot(new ClubSlot("Olimpija Ljubljana", 100f),
        // new ClubSlot("Escaldes", 201f), ueclQ2.getCompLevel(), 4, 2);
        // ClubSlot ueclQ2Tie3 = new ClubSlot(new ClubSlot("The New Saints", 163f), new
        // ClubSlot("Differdang", 276f), ueclQ2.getCompLevel(), 0, 1);
        // ClubSlot ueclQ2Tie4 = new ClubSlot(new ClubSlot("Podgorica", 414f), new
        // ClubSlot("Milsami Orhei", 259f), ueclQ2.getCompLevel(), 0, 0);
        // ClubSlot ueclQ2Tie5 = new ClubSlot(new ClubSlot("Zalgiris Vilnius", 137f),
        // new ClubSlot("Linfield", 179f), ueclQ2.getCompLevel(), 0, 0);
        ClubSlot ueclQ2Tie6 = new ClubSlot(new ClubSlot("Cherno More", 333f, clubEloDataLoader), new ClubSlot("Bueyueksehir", 82f, clubEloDataLoader), ueclQ2.getTournament(), 0, 1);
        // ClubSlot ueclQ2Tie7 = new ClubSlot(new ClubSlot("Dinamo Minsk", 249f), new
        // ClubSlot("Egnatia", 369f), ueclQ2.getCompLevel(), 0, 2);
        ClubSlot ueclQ2Tie8 = new ClubSlot(new ClubSlot("Dundee United", 208f, clubEloDataLoader), new ClubSlot("Una Strassen", 416f, clubEloDataLoader), ueclQ2.getTournament(), 1, 0);
        ClubSlot ueclQ2Tie9 = new ClubSlot(new ClubSlot("Larne", 212f, clubEloDataLoader), new ClubSlot("Prishtine", 377f, clubEloDataLoader), ueclQ2.getTournament(), 0, 0);
        ClubSlot ueclQ2Tie10 = new ClubSlot(new ClubSlot("Kosice", 314.1f, clubEloDataLoader), new ClubSlot("Neman Grodno", 341f, clubEloDataLoader), ueclQ2.getTournament(), 2, 3);
        ClubSlot ueclQ2Tie11 = new ClubSlot(new ClubSlot("Vaduz", 202f, clubEloDataLoader), new ClubSlot("Dungannon", 398.5f, clubEloDataLoader), ueclQ2.getTournament(), 0, 1);
        ClubSlot ueclQ2Tie12 = new ClubSlot(new ClubSlot("Silkeborg", 229f, clubEloDataLoader), new ClubSlot("Akureyri", 363f, clubEloDataLoader), ueclQ2.getTournament(), 1, 1);
        ClubSlot ueclQ2Tie13 = new ClubSlot(new ClubSlot("Rosenborg", 191f, clubEloDataLoader), new ClubSlot("Banga", 402.1f, clubEloDataLoader), ueclQ2.getTournament(), 5, 0);
        ClubSlot ueclQ2Tie14 = new ClubSlot(new ClubSlot("Atletic Club Escaldes", 344f, clubEloDataLoader), new ClubSlot("Dinamo Tirana", 406.1f, clubEloDataLoader), ueclQ2.getTournament(), 1, 2);
        ClubSlot ueclQ2Tie15 = new ClubSlot(new ClubSlot("Austria Wien", 200f, clubEloDataLoader), new ClubSlot("FC Spaeri", 420.1f, clubEloDataLoader), ueclQ2.getTournament(), 2, 0);
        ClubSlot ueclQ2Tie16 = new ClubSlot(new ClubSlot("Ballkani", 181f, clubEloDataLoader), new ClubSlot("Floriana", 309f, clubEloDataLoader), ueclQ2.getTournament(), 4, 2);
        ClubSlot ueclQ2Tie17 = new ClubSlot(new ClubSlot("Viking", 192f, clubEloDataLoader), new ClubSlot("Koper", 317f, clubEloDataLoader), ueclQ2.getTournament(), 7, 0);
        ClubSlot ueclQ2Tie18 = new ClubSlot(new ClubSlot("AEK", 160f, clubEloDataLoader), new ClubSlot("Beer-Sheva", 109f, clubEloDataLoader), ueclQ2.getTournament(), 1, 0);
        ClubSlot ueclQ2Tie19 = new ClubSlot(new ClubSlot("Pyunik", 176f, clubEloDataLoader), new ClubSlot("Gyoer", 297.1f, clubEloDataLoader), ueclQ2.getTournament(), 2, 1);
        ClubSlot ueclQ2Tie20 = new ClubSlot(new ClubSlot("FK Riga", 155f, clubEloDataLoader), new ClubSlot("Dila Gori", 328f, clubEloDataLoader), ueclQ2.getTournament(), 2, 1);
        ClubSlot ueclQ2Tie21 = new ClubSlot(new ClubSlot("Rakow", 186f, clubEloDataLoader), new ClubSlot("Zilina", 282f, clubEloDataLoader), ueclQ2.getTournament(), 3, 0);
        ClubSlot ueclQ2Tie22 = new ClubSlot(new ClubSlot("Petrocub", 177f, clubEloDataLoader), new ClubSlot("Sabah", 319f, clubEloDataLoader), ueclQ2.getTournament(), 0, 2);
        ClubSlot ueclQ2Tie23 = new ClubSlot(new ClubSlot("Ararat", 197f, clubEloDataLoader), new ClubSlot("Universitatea Cluj", 300.1f, clubEloDataLoader), ueclQ2.getTournament(), 0, 0);
        ClubSlot ueclQ2Tie24 = new ClubSlot(new ClubSlot("Varazdin", 270.1f, clubEloDataLoader), new ClubSlot("Santa Clara", 131f, clubEloDataLoader), ueclQ2.getTournament(), 2, 1);
        ClubSlot ueclQ2Tie25 = new ClubSlot(new ClubSlot("Kauno Zalgiris", 281f, clubEloDataLoader), new ClubSlot("Valur", 351f, clubEloDataLoader), ueclQ2.getTournament(), 1, 1);
        ClubSlot ueclQ2Tie26 = new ClubSlot(new ClubSlot("Paksi", 291f, clubEloDataLoader), new ClubSlot("Maribor", 159f, clubEloDataLoader), ueclQ2.getTournament(), 1, 0);
        ClubSlot ueclQ2Tie27 = new ClubSlot(new ClubSlot("Vllaznia", 279f, clubEloDataLoader), new ClubSlot("Vikingur", 150f, clubEloDataLoader), ueclQ2.getTournament(), 2, 1);
        ClubSlot ueclQ2Tie28 = new ClubSlot(new ClubSlot("Hammarby", 264f, clubEloDataLoader), new ClubSlot("Charleroi", 144f, clubEloDataLoader), ueclQ2.getTournament(), 0, 0);
        ClubSlot ueclQ2Tie29 = new ClubSlot(new ClubSlot("Kragujevac", 271f, clubEloDataLoader), new ClubSlot("Klaksvik", 148f, clubEloDataLoader), ueclQ2.getTournament(), 0, 0);
        ClubSlot ueclQ2Tie30 = new ClubSlot(new ClubSlot("Novi Pazar", 272.1f, clubEloDataLoader), new ClubSlot("Jagiellonia", 119f, clubEloDataLoader), ueclQ2.getTournament(), 1, 2);
        ClubSlot ueclQ2Tie31 = new ClubSlot(new ClubSlot("Polissya Zhytomyr", 287f, clubEloDataLoader), new ClubSlot("Santa Coloma", 329f, clubEloDataLoader), ueclQ2.getTournament(), 1, 2);
        ClubSlot ueclQ2Tie32 = new ClubSlot(new ClubSlot("Vardar", 422.1f, clubEloDataLoader), new ClubSlot("Lausanne", 235.1f, clubEloDataLoader), ueclQ2.getTournament(), 2, 1);
        ClubSlot ueclQ2Tie33 = new ClubSlot(new ClubSlot("HB Torshavn", 251f, clubEloDataLoader), new ClubSlot("Brondby", 215f, clubEloDataLoader), ueclQ2.getTournament(), 1, 1);
        ClubSlot ueclQ2Tie34 = new ClubSlot(new ClubSlot("Olexandriya", 290.1f, clubEloDataLoader), new ClubSlot("Partizan", 85f, clubEloDataLoader), ueclQ2.getTournament(), 0, 2);
        ClubSlot ueclQ2Tie35 = new ClubSlot(new ClubSlot("Hibernians Paola", 265f, clubEloDataLoader), new ClubSlot("Trnava", 178f, clubEloDataLoader), ueclQ2.getTournament(), 1, 2);
        ClubSlot ueclQ2Tie36 = new ClubSlot(new ClubSlot("St Patricks", 258f, clubEloDataLoader), new ClubSlot("Nomme Kalju", 405f, clubEloDataLoader), ueclQ2.getTournament(), 1, 0);
        ClubSlot ueclQ2Tie37 = new ClubSlot(new ClubSlot("Paide Linnameeskond", 214f, clubEloDataLoader), new ClubSlot("AIK", 268f, clubEloDataLoader), ueclQ2.getTournament(), 0, 2);
        ClubSlot ueclQ2Tie38 = new ClubSlot(new ClubSlot("FK Sarajevo", 252f, clubEloDataLoader), new ClubSlot("Craiova", 240f, clubEloDataLoader), ueclQ2.getTournament(), 2, 1);
        ClubSlot ueclQ2Tie39 = new ClubSlot(new ClubSlot("Aris Limassol", 256f, clubEloDataLoader), new ClubSlot("Puskas Akademia", 237f, clubEloDataLoader), ueclQ2.getTournament(), 3, 2);
        ClubSlot ueclQ2Tie40 = new ClubSlot(new ClubSlot("St Josephs", 327f, clubEloDataLoader), new ClubSlot("Shamrock", 99f, clubEloDataLoader), ueclQ2.getTournament(), 0, 4);
        ClubSlot ueclQ2Tie41 = new ClubSlot(new ClubSlot("Ilves Tampere", 348f, clubEloDataLoader), new ClubSlot("Alkmaar", 39f, clubEloDataLoader), ueclQ2.getTournament(), 4, 3);
        ClubSlot ueclQ2Tie42 = new ClubSlot(new ClubSlot("Zira", 318f, clubEloDataLoader), new ClubSlot("Hajduk", 154f, clubEloDataLoader), ueclQ2.getTournament(), 1, 1);
        ClubSlot ueclQ2Tie43 = new ClubSlot(new ClubSlot("Arda", 335f, clubEloDataLoader), new ClubSlot("HJK Helsinki", 128f, clubEloDataLoader), ueclQ2.getTournament(), 0, 0);
        ClubSlot ueclQ2Tie44 = new ClubSlot(new ClubSlot("Aktobe", 352f, clubEloDataLoader), new ClubSlot("Sparta Praha", 71f, clubEloDataLoader), ueclQ2.getTournament(), 2, 1);
        ClubSlot ueclQ2Tie45 = new ClubSlot(new ClubSlot("FK Astana", 134f, clubEloDataLoader), new ClubSlot("Zimbru", 349f, clubEloDataLoader), ueclQ2.getTournament(), 1, 1);
        ClubSlot ueclQ2Tie46 = new ClubSlot(new ClubSlot("Decic", 320f, clubEloDataLoader), new ClubSlot("Rapid Wien", 68f, clubEloDataLoader), ueclQ2.getTournament(), 0, 2);
        ClubSlot ueclQ2Tie47 = new ClubSlot(new ClubSlot("Torpedo Zhodino", 325f, clubEloDataLoader), new ClubSlot("Maccabi Haifa", 98f, clubEloDataLoader), ueclQ2.getTournament(), 1, 1);
        ClubSlot ueclQ2Tie48 = new ClubSlot(new ClubSlot("Nakchivan", 338.1f, clubEloDataLoader), new ClubSlot("Aris", 195f, clubEloDataLoader), ueclQ2.getTournament(), 2, 1);
        ClubSlot ueclQ2Tie49 = new ClubSlot(new ClubSlot("Omonia", 101f, clubEloDataLoader), new ClubSlot("Torpedo Kutaisi", 350f, clubEloDataLoader), ueclQ2.getTournament(), 1, 0);
        ClubSlot ueclQ2Tie50 = new ClubSlot(new ClubSlot("Sutjeska", 263f, clubEloDataLoader), new ClubSlot("Beitar", 246f, clubEloDataLoader), ueclQ2.getTournament(), 1, 2);

        ueclQ2.addTies(new ArrayList<>(Arrays.asList(
                ueclQ2Tie6, ueclQ2Tie8, ueclQ2Tie9, ueclQ2Tie10, ueclQ2Tie11, ueclQ2Tie12,
                ueclQ2Tie13, ueclQ2Tie14, ueclQ2Tie15, ueclQ2Tie16, ueclQ2Tie17, ueclQ2Tie18,
                ueclQ2Tie19, ueclQ2Tie20, ueclQ2Tie21, ueclQ2Tie22, ueclQ2Tie23, ueclQ2Tie24,
                ueclQ2Tie25, ueclQ2Tie26, ueclQ2Tie27, ueclQ2Tie28, ueclQ2Tie29, ueclQ2Tie30,
                ueclQ2Tie31, ueclQ2Tie32, ueclQ2Tie33, ueclQ2Tie34, ueclQ2Tie35, ueclQ2Tie36,
                ueclQ2Tie37, ueclQ2Tie38, ueclQ2Tie39, ueclQ2Tie40, ueclQ2Tie41, ueclQ2Tie42,
                ueclQ2Tie43, ueclQ2Tie44, ueclQ2Tie45, ueclQ2Tie46, ueclQ2Tie47, ueclQ2Tie48,
                ueclQ2Tie49, ueclQ2Tie50)));

        // Add ties to UEL Q3
        uelQ3.addTies(new ArrayList<>(Arrays.asList(
                new ClubSlot(uelQ2Tie5, uclQ2Tie1, uelQ3.getTournament()),
                new ClubSlot(uclQ2Tie2, uelQ2Tie2, uelQ3.getTournament()),
                new ClubSlot(uclQ2Tie3, uelQ2Tie6, uelQ3.getTournament()),
                new ClubSlot(uelQ2Tie7, uelQ2Tie1, uelQ3.getTournament()),
                new ClubSlot(new ClubSlot("Fredrikstad", 194.1f, clubEloDataLoader), uelQ2Tie3, uelQ3.getTournament()),
                new ClubSlot(uelQ2Tie8, uelQ2Tie4, uelQ3.getTournament()),
                new ClubSlot(new ClubSlot("PAOK", 52f, clubEloDataLoader), new ClubSlot("Wolfsberg", 162f, clubEloDataLoader), uelQ3.getTournament()))));

        // Add ties to UECL Q3
        ueclQ3.addTies(new ArrayList<>(Arrays.asList(
                new ClubSlot(ueclQ2Tie46, ueclQ2Tie8, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie44, ueclQ2Tie23, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie41, ueclQ2Tie11, ueclQ3.getTournament()),
                new ClubSlot(uelQ2Tie8, uelQ2Tie7, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie29, ueclQ2Tie10, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie20, ueclQ2Tie50, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie27, ueclQ2Tie33, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie42, ueclQ2Tie14, ueclQ3.getTournament()),
                new ClubSlot(uelQ2Tie5, uelQ2Tie2, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie32, ueclQ2Tie45, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie9, ueclQ2Tie24, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie39, ueclQ2Tie18, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie17, ueclQ2Tie6, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie48, ueclQ2Tie49, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie16, ueclQ2Tie40, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie21, ueclQ2Tie47, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie37, ueclQ2Tie19, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie38, ueclQ2Tie35, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie31, ueclQ2Tie26, ueclQ3.getTournament()),
                new ClubSlot(uelQ2Tie4, ueclQ2Tie22, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie12, ueclQ2Tie30, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie34, uelQ2Tie3, ueclQ3.getTournament()),
                new ClubSlot(uelQ2Tie1, ueclQ2Tie15, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie13, ueclQ2Tie28, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie36, uelQ2Tie6, ueclQ3.getTournament()),
                new ClubSlot(ueclQ2Tie25, ueclQ2Tie43, ueclQ3.getTournament()))));


        // Add clubs to UECL playoff
        ueclPO.addClubSlots(new ArrayList<>(Arrays.asList(
                new ClubSlot("Fiorentina", 33f, clubEloDataLoader),
                new ClubSlot("Crystal Palace", 80.1f, clubEloDataLoader),
                new ClubSlot("Rayo Vallecano", 96.1f, clubEloDataLoader),
                new ClubSlot("Mainz", 106.1f, clubEloDataLoader),
                new ClubSlot("Strasbourg", 117.1f, clubEloDataLoader))));

        // Initialize the ClubEloDataLoader to fetch club Elo ratings.
        clubEloDataLoader.init();

        // // Link rounds to define the progression flow.
        // linkRounds();
    }

    public List<Round> getRounds() {
        return rounds;
    }

    /**
     * Initiates the simulation by executing all rounds in their respective order.
     * This method drives the simulation from qualifiers through league matches.
     */
    public void run(String threadName) {
        // Play the Q2 matches
        uclQ2.play();
        uelQ2.play();
        ueclQ2.play();

        // Add ties to UECL playoff
        ArrayList<ClubSlot> combinedTies = new ArrayList<>(uelQ3.getTies());
        combinedTies.addAll(ueclQ3.getTies());
        ueclPO.addClubSlots(combinedTies);

        // Seed the UECL playoff
        ueclPO.seed();

        // Increment seeding counters for all clubs in UECL playoff
        ueclPO.getSeeded().forEach(clubSlot -> clubSlot.incrementSeedingCounter(true));
        ueclPO.getUnseeded().forEach(clubSlot -> clubSlot.incrementSeedingCounter(false));
    }

    /**
     * Returns a string representation of all rounds for logging and debugging
     * purposes.
     *
     * @return a string summarizing the rounds sequence in the simulation
     */
    @Override
    public String toString() {
        return "Rounds [" + Arrays.toString(rounds.toArray()) + "]";
    }
}