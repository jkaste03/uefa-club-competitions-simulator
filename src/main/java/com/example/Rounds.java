package com.example;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    // Map to hold club Elo ratings for each club.
    private ClubEloDataLoader clubEloDataLoader;

    /**
     * Constructs all rounds for UEFA competitions, initializes club Elo API,
     * and sets up the interlink between rounds. This constructor prepares the
     * simulation by creating each qualifying and league phase round instance.
     */
    public Rounds() {
        uclQ2 = new Round(1, 2);
        uelQ2 = new Round(2, 2);
        ueclQ2 = new Round(3, 2);

        uelQ3 = new Round(2, 3);
        ueclQ3 = new Round(3, 3);

        ueclPO = new Round(3, 4);

        // Aggregate all rounds into a list for streamlined processing.
        rounds = new ArrayList<>(
                Arrays.asList(uclQ2, uelQ2, ueclQ2, uelQ3, ueclQ3, ueclPO));

        // Initialize data for each round.
        // JsonDataLoader.loadDataForRounds(rounds);

        // uclQ2.setNextSecondaryRnd(uelQ3);
        // uelQ2.setNextRounds(uelQ3, ueclQ3);
        // ueclQ2.setNextPrimaryRnd(ueclQ3);

        // uelQ3.setNextSecondaryRnd(ueclPO);
        // ueclQ3.setNextPrimaryRnd(ueclPO);

        // UCL Q2 ties
        ClubSlot uclQ2Tie1 = new ClubSlot(new ClubSlot("Brann", 189f), new ClubSlot("Salzburg", 44f), uclQ2.getCompLevel(), 1, 4);
        ClubSlot uclQ2Tie2 = new ClubSlot(new ClubSlot("Viktoria Plzen", 56f), new ClubSlot("Servette", 140f), uclQ2.getCompLevel(), 0, 1);
        ClubSlot uclQ2Tie3 = new ClubSlot(new ClubSlot("Rangers", 25f), new ClubSlot("Panathinaikos", 111f), uclQ2.getCompLevel(), 2, 0);

        uclQ2.addTies(new ArrayList<>(Arrays.asList(
                uclQ2Tie1,
                uclQ2Tie2,
                uclQ2Tie3)));

        // UEL Q2 ties
        ClubSlot uelQ2Tie1 = new ClubSlot(new ClubSlot("Banik Ostrava", 168f), new ClubSlot("Legia", 70f), uelQ2.getCompLevel(), 2, 2);
        ClubSlot uelQ2Tie2 = new ClubSlot(new ClubSlot("Sheriff Tiraspol", 88f), new ClubSlot("Utrecht", 125.1f), uelQ2.getCompLevel(), 1, 3);
        ClubSlot uelQ2Tie3 = new ClubSlot(new ClubSlot("Midtjylland", 67f), new ClubSlot("Hibernian", 207f), uelQ2.getCompLevel(), 1, 1);
        ClubSlot uelQ2Tie4 = new ClubSlot(new ClubSlot("Levski", 312f), new ClubSlot("Braga", 48f), uelQ2.getCompLevel(), 0, 0);
        ClubSlot uelQ2Tie5 = new ClubSlot(new ClubSlot("Anderlecht", 72f), new ClubSlot("Haecken", 211f), uelQ2.getCompLevel(), 1, 0);
        ClubSlot uelQ2Tie6 = new ClubSlot(new ClubSlot("Besiktas", 113f), new ClubSlot("Shakhtar", 41f), uelQ2.getCompLevel(), 2, 4);
        ClubSlot uelQ2Tie7 = new ClubSlot(new ClubSlot("Celje", 120f), new ClubSlot("Larnaca", 199f), uelQ2.getCompLevel(), 1, 1);
        ClubSlot uelQ2Tie8 = new ClubSlot(new ClubSlot("Lugano", 90f), new ClubSlot("CFR Cluj", 92f), uelQ2.getCompLevel(), 0, 0);

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
        ClubSlot ueclQ2Tie6 = new ClubSlot(new ClubSlot("Cherno More", 333f), new ClubSlot("Bueyueksehir", 82f), ueclQ2.getCompLevel(), 0, 1);
        // ClubSlot ueclQ2Tie7 = new ClubSlot(new ClubSlot("Dinamo Minsk", 249f), new
        // ClubSlot("Egnatia", 369f), ueclQ2.getCompLevel(), 0, 2);
        ClubSlot ueclQ2Tie8 = new ClubSlot(new ClubSlot("Dundee United", 208f), new ClubSlot("Una Strassen", 416f), ueclQ2.getCompLevel(), 1, 0);
        ClubSlot ueclQ2Tie9 = new ClubSlot(new ClubSlot("Larne", 212f), new ClubSlot("Prishtine", 377f), ueclQ2.getCompLevel(), 0, 0);
        ClubSlot ueclQ2Tie10 = new ClubSlot(new ClubSlot("Kosice", 314.1f), new ClubSlot("Neman Grodno", 341f), ueclQ2.getCompLevel(), 2, 3);
        ClubSlot ueclQ2Tie11 = new ClubSlot(new ClubSlot("Vaduz", 202f), new ClubSlot("Dungannon", 398.5f), ueclQ2.getCompLevel(), 0, 1);
        ClubSlot ueclQ2Tie12 = new ClubSlot(new ClubSlot("Silkeborg", 229f), new ClubSlot("Akureyri", 363f), ueclQ2.getCompLevel(), 1, 1);
        ClubSlot ueclQ2Tie13 = new ClubSlot(new ClubSlot("Rosenborg", 191f), new ClubSlot("Banga", 402.1f), ueclQ2.getCompLevel(), 5, 0);
        ClubSlot ueclQ2Tie14 = new ClubSlot(new ClubSlot("Atletic Club Escaldes", 344f), new ClubSlot("Dinamo Tirana", 406.1f), ueclQ2.getCompLevel(), 1, 2);
        ClubSlot ueclQ2Tie15 = new ClubSlot(new ClubSlot("Austria Wien", 200f), new ClubSlot("FC Spaeri", 420.1f), ueclQ2.getCompLevel(), 2, 0);
        ClubSlot ueclQ2Tie16 = new ClubSlot(new ClubSlot("Ballkani", 181f), new ClubSlot("Floriana", 309f), ueclQ2.getCompLevel(), 4, 2);
        ClubSlot ueclQ2Tie17 = new ClubSlot(new ClubSlot("Viking", 192f), new ClubSlot("Koper", 317f), ueclQ2.getCompLevel(), 7, 0);
        ClubSlot ueclQ2Tie18 = new ClubSlot(new ClubSlot("AEK", 160f), new ClubSlot("Beer-Sheva", 109f), ueclQ2.getCompLevel(), 1, 0);
        ClubSlot ueclQ2Tie19 = new ClubSlot(new ClubSlot("Pyunik", 176f), new ClubSlot("Gyoer", 297.1f), ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie20 = new ClubSlot(new ClubSlot("FK Riga", 155f), new ClubSlot("Dila Gori", 328f), ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie21 = new ClubSlot(new ClubSlot("Rakow", 186f), new ClubSlot("Zilina", 282f), ueclQ2.getCompLevel(), 3, 0);
        ClubSlot ueclQ2Tie22 = new ClubSlot(new ClubSlot("Petrocub", 177f), new ClubSlot("Sabah", 319f), ueclQ2.getCompLevel(), 0, 2);
        ClubSlot ueclQ2Tie23 = new ClubSlot(new ClubSlot("Ararat", 197f), new ClubSlot("Universitatea Cluj", 300.1f), ueclQ2.getCompLevel(), 0, 0);
        ClubSlot ueclQ2Tie24 = new ClubSlot(new ClubSlot("Varazdin", 270.1f), new ClubSlot("Santa Clara", 131f), ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie25 = new ClubSlot(new ClubSlot("Kauno Zalgiris", 281f), new ClubSlot("Valur", 351f), ueclQ2.getCompLevel(), 1, 1);
        ClubSlot ueclQ2Tie26 = new ClubSlot(new ClubSlot("Paksi", 291f), new ClubSlot("Maribor", 159f), ueclQ2.getCompLevel(), 1, 0);
        ClubSlot ueclQ2Tie27 = new ClubSlot(new ClubSlot("Vllaznia", 279f), new ClubSlot("Vikingur", 150f), ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie28 = new ClubSlot(new ClubSlot("Hammarby", 264f), new ClubSlot("Charleroi", 144f), ueclQ2.getCompLevel(), 0, 0);
        ClubSlot ueclQ2Tie29 = new ClubSlot(new ClubSlot("Kragujevac", 271f), new ClubSlot("Klaksvik", 148f), ueclQ2.getCompLevel(), 0, 0);
        ClubSlot ueclQ2Tie30 = new ClubSlot(new ClubSlot("Novi Pazar", 272.1f), new ClubSlot("Jagiellonia", 119f), ueclQ2.getCompLevel(), 1, 2);
        ClubSlot ueclQ2Tie31 = new ClubSlot(new ClubSlot("Polissya Zhytomyr", 287f), new ClubSlot("Santa Coloma", 329f), ueclQ2.getCompLevel(), 1, 2);
        ClubSlot ueclQ2Tie32 = new ClubSlot(new ClubSlot("Vardar", 422.1f), new ClubSlot("Lausanne", 235.1f), ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie33 = new ClubSlot(new ClubSlot("HB Torshavn", 251f), new ClubSlot("Brondby", 215f), ueclQ2.getCompLevel(), 1, 1);
        ClubSlot ueclQ2Tie34 = new ClubSlot(new ClubSlot("Olexandriya", 290.1f), new ClubSlot("Partizan", 85f), ueclQ2.getCompLevel(), 0, 2);
        ClubSlot ueclQ2Tie35 = new ClubSlot(new ClubSlot("Hibernians Paola", 265f), new ClubSlot("Trnava", 178f), ueclQ2.getCompLevel(), 1, 2);
        ClubSlot ueclQ2Tie36 = new ClubSlot(new ClubSlot("St Patricks", 258f), new ClubSlot("Nomme Kalju", 405f), ueclQ2.getCompLevel(), 1, 0);
        ClubSlot ueclQ2Tie37 = new ClubSlot(new ClubSlot("Paide Linnameeskond", 214f), new ClubSlot("AIK", 268f), ueclQ2.getCompLevel(), 0, 2);
        ClubSlot ueclQ2Tie38 = new ClubSlot(new ClubSlot("FK Sarajevo", 252f), new ClubSlot("Craiova", 240f), ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie39 = new ClubSlot(new ClubSlot("Aris Limassol", 256f), new ClubSlot("Puskas Akademia", 237f), ueclQ2.getCompLevel(), 3, 2);
        ClubSlot ueclQ2Tie40 = new ClubSlot(new ClubSlot("St Josephs", 327f), new ClubSlot("Shamrock", 99f), ueclQ2.getCompLevel(), 0, 4);
        ClubSlot ueclQ2Tie41 = new ClubSlot(new ClubSlot("Ilves Tampere", 348f), new ClubSlot("Alkmaar", 39f), ueclQ2.getCompLevel(), 4, 3);
        ClubSlot ueclQ2Tie42 = new ClubSlot(new ClubSlot("Zira", 318f), new ClubSlot("Hajduk", 154f), ueclQ2.getCompLevel(), 1, 1);
        ClubSlot ueclQ2Tie43 = new ClubSlot(new ClubSlot("Arda", 335f), new ClubSlot("HJK Helsinki", 128f), ueclQ2.getCompLevel(), 0, 0);
        ClubSlot ueclQ2Tie44 = new ClubSlot(new ClubSlot("Aktobe", 352f), new ClubSlot("Sparta Praha", 71f), ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie45 = new ClubSlot(new ClubSlot("FK Astana", 134f), new ClubSlot("Zimbru", 349f), ueclQ2.getCompLevel(), 1, 1);
        ClubSlot ueclQ2Tie46 = new ClubSlot(new ClubSlot("Decic", 320f), new ClubSlot("Rapid Wien", 68f), ueclQ2.getCompLevel(), 0, 2);
        ClubSlot ueclQ2Tie47 = new ClubSlot(new ClubSlot("Torpedo Zhodino", 325f), new ClubSlot("Maccabi Haifa", 98f), ueclQ2.getCompLevel(), 1, 1);
        ClubSlot ueclQ2Tie48 = new ClubSlot(new ClubSlot("Nakchivan", 338.1f), new ClubSlot("Aris", 195f), ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie49 = new ClubSlot(new ClubSlot("Omonia", 101f), new ClubSlot("Torpedo Kutaisi", 350f), ueclQ2.getCompLevel(), 1, 0);
        ClubSlot ueclQ2Tie50 = new ClubSlot(new ClubSlot("Sutjeska", 263f), new ClubSlot("Beitar", 246f), ueclQ2.getCompLevel(), 1, 2);

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
                new ClubSlot(uelQ2Tie5, uclQ2Tie1, uelQ3.getCompLevel()),
                new ClubSlot(uclQ2Tie2, uelQ2Tie2, uelQ3.getCompLevel()),
                new ClubSlot(uclQ2Tie3, uelQ2Tie6, uelQ3.getCompLevel()),
                new ClubSlot(uelQ2Tie7, uelQ2Tie1, uelQ3.getCompLevel()),
                new ClubSlot(new ClubSlot("Fredrikstad", 194.1f), uelQ2Tie3, uelQ3.getCompLevel()),
                new ClubSlot(uelQ2Tie8, uelQ2Tie4, uelQ3.getCompLevel()),
                new ClubSlot(new ClubSlot("PAOK", 52f), new ClubSlot("Wolfsberg", 162f), uelQ3.getCompLevel()))));

        // Add ties to UECL Q3
        ueclQ3.addTies(new ArrayList<>(Arrays.asList(
                new ClubSlot(ueclQ2Tie46, ueclQ2Tie8, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie44, ueclQ2Tie23, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie41, ueclQ2Tie11, ueclQ3.getCompLevel()),
                new ClubSlot(uelQ2Tie8, uelQ2Tie7, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie29, ueclQ2Tie10, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie20, ueclQ2Tie50, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie27, ueclQ2Tie33, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie42, ueclQ2Tie14, ueclQ3.getCompLevel()),
                new ClubSlot(uelQ2Tie5, uelQ2Tie2, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie32, ueclQ2Tie45, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie9, ueclQ2Tie24, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie39, ueclQ2Tie18, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie17, ueclQ2Tie6, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie48, ueclQ2Tie49, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie16, ueclQ2Tie40, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie21, ueclQ2Tie47, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie37, ueclQ2Tie19, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie38, ueclQ2Tie35, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie31, ueclQ2Tie26, ueclQ3.getCompLevel()),
                new ClubSlot(uelQ2Tie4, ueclQ2Tie22, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie12, ueclQ2Tie30, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie34, uelQ2Tie3, ueclQ3.getCompLevel()),
                new ClubSlot(uelQ2Tie1, ueclQ2Tie15, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie13, ueclQ2Tie28, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie36, uelQ2Tie6, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie25, ueclQ2Tie43, ueclQ3.getCompLevel()))));


        // Add clubs to UECL playoff
        ueclPO.addClubSlots(new ArrayList<>(Arrays.asList(
                new ClubSlot("Fiorentina", 33f),
                new ClubSlot("Crystal Palace", 80.1f),
                new ClubSlot("Rayo Vallecano", 96.1f),
                new ClubSlot("Mainz", 106.1f),
                new ClubSlot("Strasbourg", 117.1f))));

        // Initialize external service to fetch club elo ratings
        // clubEloDataLoader = new ClubEloDataLoader();
        // clubEloDataLoader.init();
        ClubEloDataLoader.init();

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