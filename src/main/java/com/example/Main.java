package com.example;

import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        Round uclQ2 = new Round(1, 2);
        Round uelQ2 = new Round(2, 2);
        Round ueclQ2 = new Round(3, 2);

        Round uelQ3 = new Round(2, 3);
        Round ueclQ3 = new Round(3, 3);

        Round ueclPO = new Round(3, 4);

        uclQ2.setNextSecondaryRnd(uelQ3);
        uelQ2.setNextRounds(uelQ3, ueclQ3);
        ueclQ2.setNextPrimaryRnd(ueclQ3);

        uelQ3.setNextSecondaryRnd(ueclPO);
        ueclQ3.setNextPrimaryRnd(ueclPO);

        // Example clubs
        // UCL Q2 ties
        ClubSlot uclQ2Tie1 = new ClubSlot(new ClubSlot("Brann", 189f), new ClubSlot("Salzburg", 44f),
                uclQ2.getCompLevel(), 1, 4);
        ClubSlot uclQ2Tie2 = new ClubSlot(new ClubSlot("Viktoria Plzen", 56f), new ClubSlot("Servette", 140f),
                uclQ2.getCompLevel(), 0, 1);
        ClubSlot uclQ2Tie3 = new ClubSlot(new ClubSlot("Rangers", 25f), new ClubSlot("Panathinaikos", 111f),
                uclQ2.getCompLevel(), 2, 0);

        uclQ2.addTies(new ArrayList<>(Arrays.asList(
                uclQ2Tie1,
                uclQ2Tie2,
                uclQ2Tie3)));

        // UEL Q2 ties
        ClubSlot uelQ2Tie1 = new ClubSlot(new ClubSlot("Banik Ostrava", 168f), new ClubSlot("Legia", 70f),
                uelQ2.getCompLevel(), 2, 2);
        ClubSlot uelQ2Tie2 = new ClubSlot(new ClubSlot("Sheriff Tiraspol", 88f), new ClubSlot("Utrecht", 125.1f),
                uelQ2.getCompLevel(), 1, 3);
        ClubSlot uelQ2Tie3 = new ClubSlot(new ClubSlot("Midtjylland", 67f), new ClubSlot("Hibernian", 207f),
                uelQ2.getCompLevel(), 1, 1);
        ClubSlot uelQ2Tie4 = new ClubSlot(new ClubSlot("Levski", 312f), new ClubSlot("Braga", 48f),
                uelQ2.getCompLevel(), 0, 0);
        ClubSlot uelQ2Tie5 = new ClubSlot(new ClubSlot("Anderlecht", 72f), new ClubSlot("Haecken", 211f),
                uelQ2.getCompLevel(), 1, 0);
        ClubSlot uelQ2Tie6 = new ClubSlot(new ClubSlot("Besiktas", 113f), new ClubSlot("Shakhtar", 41f),
                uelQ2.getCompLevel(), 2, 4);
        ClubSlot uelQ2Tie7 = new ClubSlot(new ClubSlot("Celje", 120f), new ClubSlot("Larnaca", 199f),
                uelQ2.getCompLevel(), 1, 1);
        ClubSlot uelQ2Tie8 = new ClubSlot(new ClubSlot("Lugano", 90f), new ClubSlot("CFR Cluj", 92f),
                uelQ2.getCompLevel(), 0, 0);

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
        ClubSlot ueclQ2Tie1 = new ClubSlot(new ClubSlot("FC Infonet", 1305.1393f),
                new ClubSlot("Saburtalo", 1044.7577f), ueclQ2.getCompLevel(), 1, 0);
        ClubSlot ueclQ2Tie2 = new ClubSlot(new ClubSlot("Olimpija Ljubljana", 1305.1393f),
                new ClubSlot("Escaldes", 1044.7577f), ueclQ2.getCompLevel(), 4, 2);
        ClubSlot ueclQ2Tie3 = new ClubSlot(new ClubSlot("The New Saints", 1305.1393f),
                new ClubSlot("Differdang", 1044.7577f), ueclQ2.getCompLevel(), 0, 1);
        ClubSlot ueclQ2Tie4 = new ClubSlot(new ClubSlot("Podgorica", 1305.1393f),
                new ClubSlot("Milsami Orhei", 1044.7577f), ueclQ2.getCompLevel(), 0, 0);
        ClubSlot ueclQ2Tie5 = new ClubSlot(new ClubSlot("Zalgiris Vilnius", 1305.1393f),
                new ClubSlot("Linfield", 1044.7577f), ueclQ2.getCompLevel(), 0, 0);
        ClubSlot ueclQ2Tie6 = new ClubSlot(new ClubSlot("Cherno More", 1305.1393f),
                new ClubSlot("Bueyueksehir", 1044.7577f), ueclQ2.getCompLevel(), 0, 1);
        ClubSlot ueclQ2Tie7 = new ClubSlot(new ClubSlot("Dinamo Minsk", 1305.1393f),
                new ClubSlot("Egnatia", 1044.7577f), ueclQ2.getCompLevel(), 0, 2);
        ClubSlot ueclQ2Tie8 = new ClubSlot(new ClubSlot("Dundee United", 1305.1393f),
                new ClubSlot("Una Strassen", 1044.7577f), ueclQ2.getCompLevel(), 1, 0);
        ClubSlot ueclQ2Tie9 = new ClubSlot(new ClubSlot("Larne", 1051.3083f), new ClubSlot("Prishtine", 1076.7115f),
                ueclQ2.getCompLevel(), 0, 0);
        ClubSlot ueclQ2Tie10 = new ClubSlot(new ClubSlot("Kosice", 1370.5618f),
                new ClubSlot("Neman Grodno", 1166.1449f), ueclQ2.getCompLevel(), 2, 3);
        ClubSlot ueclQ2Tie11 = new ClubSlot(new ClubSlot("Vaduz", 1284.2422f), new ClubSlot("Dungannon", 1048.1344f),
                ueclQ2.getCompLevel(), 0, 1);
        ClubSlot ueclQ2Tie12 = new ClubSlot(new ClubSlot("Silkeborg", 1448.4069f), new ClubSlot("Akureyri", 1214.6267f),
                ueclQ2.getCompLevel(), 1, 1);
        ClubSlot ueclQ2Tie13 = new ClubSlot(new ClubSlot("Rosenborg", 1449.0964f), new ClubSlot("Banga", 1125.9369f),
                ueclQ2.getCompLevel(), 5, 0);
        ClubSlot ueclQ2Tie14 = new ClubSlot(new ClubSlot("Atletic Club Escaldes", 951.8603f),
                new ClubSlot("Dinamo Tirana", 1791.4805f), ueclQ2.getCompLevel(), 1, 2);
        ClubSlot ueclQ2Tie15 = new ClubSlot(new ClubSlot("Austria Wien", 1480.8750f),
                new ClubSlot("FC Spaeri", 1171.3005f), ueclQ2.getCompLevel(), 2, 0);
        ClubSlot ueclQ2Tie16 = new ClubSlot(new ClubSlot("Ballkani", 1189.1057f), new ClubSlot("Floriana", 1064.6722f),
                ueclQ2.getCompLevel(), 4, 2);
        ClubSlot ueclQ2Tie17 = new ClubSlot(new ClubSlot("Viking", 1509.7223f), new ClubSlot("Koper", 1345.7413f),
                ueclQ2.getCompLevel(), 7, 0);
        ClubSlot ueclQ2Tie18 = new ClubSlot(new ClubSlot("AEK", 1513.6429f), new ClubSlot("Beer-Sheva", 1506.7981f),
                ueclQ2.getCompLevel(), 1, 0);
        ClubSlot ueclQ2Tie19 = new ClubSlot(new ClubSlot("Pyunik", 1277.1631f), new ClubSlot("Gyoer", 1322.6057f),
                ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie20 = new ClubSlot(new ClubSlot("FK Riga", 1276.0104f), new ClubSlot("Dila Gori", 1173.0673f),
                ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie21 = new ClubSlot(new ClubSlot("Rakow", 1545.3329f), new ClubSlot("Zilina", 1370.5618f),
                ueclQ2.getCompLevel(), 3, 0);
        ClubSlot ueclQ2Tie22 = new ClubSlot(new ClubSlot("Petrocub", 1207.4063f), new ClubSlot("Sabah", 1289.7834f),
                ueclQ2.getCompLevel(), 0, 2);
        ClubSlot ueclQ2Tie23 = new ClubSlot(new ClubSlot("Ararat", 1213.0376f),
                new ClubSlot("Universitatea Cluj", 1388.1025f), ueclQ2.getCompLevel(), 0, 0);
        ClubSlot ueclQ2Tie24 = new ClubSlot(new ClubSlot("Varazdin", 1346.6229f),
                new ClubSlot("Santa Clara", 1484.9388f), ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie25 = new ClubSlot(new ClubSlot("Kauno Zalgiris", 1136.1549f),
                new ClubSlot("Valur", 1234.8075f), ueclQ2.getCompLevel(), 1, 1);
        ClubSlot ueclQ2Tie26 = new ClubSlot(new ClubSlot("Paksi", 1364.2646f), new ClubSlot("Maribor", 1367.2371f),
                ueclQ2.getCompLevel(), 1, 0);
        ClubSlot ueclQ2Tie27 = new ClubSlot(new ClubSlot("Vllaznia", 1123.6422f), new ClubSlot("Vikingur", 1292.0940f),
                ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie28 = new ClubSlot(new ClubSlot("Hammarby", 1526.5978f), new ClubSlot("Charleroi", 1526.2194f),
                ueclQ2.getCompLevel(), 0, 0);
        ClubSlot ueclQ2Tie29 = new ClubSlot(new ClubSlot("Kragujevac", 1524.1234f),
                new ClubSlot("Klaksvik", 1176.4392f), ueclQ2.getCompLevel(), 0, 0);
        ClubSlot ueclQ2Tie30 = new ClubSlot(new ClubSlot("Novi Pazar", 1202.0541f),
                new ClubSlot("Jagiellonia", 1461.7075f), ueclQ2.getCompLevel(), 1, 2);
        ClubSlot ueclQ2Tie31 = new ClubSlot(new ClubSlot("Polissya Zhytomyr", 1321.8274f),
                new ClubSlot("Santa Coloma", 959.7197f), ueclQ2.getCompLevel(), 1, 2);
        ClubSlot ueclQ2Tie32 = new ClubSlot(new ClubSlot("Vardar", 1082.2795f), new ClubSlot("Lausanne", 1407.6941f),
                ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie33 = new ClubSlot(new ClubSlot("HB Torshavn", 1047.9337f),
                new ClubSlot("Brondby", 1554.7571f), ueclQ2.getCompLevel(), 1, 1);
        ClubSlot ueclQ2Tie34 = new ClubSlot(new ClubSlot("Olexandriya", 1383.6274f),
                new ClubSlot("Partizan", 1363.2365f), ueclQ2.getCompLevel(), 0, 2);
        ClubSlot ueclQ2Tie35 = new ClubSlot(new ClubSlot("Hibernians Paola", 1056.8972f),
                new ClubSlot("Trnava", 1367.7874f), ueclQ2.getCompLevel(), 1, 2);
        ClubSlot ueclQ2Tie36 = new ClubSlot(new ClubSlot("St Patricks", 1280.5881f),
                new ClubSlot("Nomme Kalju", 1074.1008f), ueclQ2.getCompLevel(), 1, 0);
        ClubSlot ueclQ2Tie37 = new ClubSlot(new ClubSlot("Paide Linnameeskond", 1088.4426f),
                new ClubSlot("AIK", 1469.2344f), ueclQ2.getCompLevel(), 0, 2);
        ClubSlot ueclQ2Tie38 = new ClubSlot(new ClubSlot("FK Sarajevo", 1216.2632f),
                new ClubSlot("Craiova", 1430.0388f), ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie39 = new ClubSlot(new ClubSlot("Aris Limassol", 1428.7919f),
                new ClubSlot("Puskas Akademia", 1418.7068f), ueclQ2.getCompLevel(), 3, 2);
        ClubSlot ueclQ2Tie40 = new ClubSlot(new ClubSlot("St Josephs", 945.9510f), new ClubSlot("Shamrock", 1302.4304f),
                ueclQ2.getCompLevel(), 0, 4);
        ClubSlot ueclQ2Tie41 = new ClubSlot(new ClubSlot("Ilves Tampere", 1203.2096f),
                new ClubSlot("Alkmaar", 1626.2758f), ueclQ2.getCompLevel(), 4, 3);
        ClubSlot ueclQ2Tie42 = new ClubSlot(new ClubSlot("Zira", 1305.4379f), new ClubSlot("Hajduk", 1436.1804f),
                ueclQ2.getCompLevel(), 1, 1);
        ClubSlot ueclQ2Tie43 = new ClubSlot(new ClubSlot("Arda", 1291.4219f), new ClubSlot("HJK Helsinki", 1240.8477f),
                ueclQ2.getCompLevel(), 0, 0);
        ClubSlot ueclQ2Tie44 = new ClubSlot(new ClubSlot("Aktobe", 1235.8463f),
                new ClubSlot("Sparta Praha", 1568.5265f), ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie45 = new ClubSlot(new ClubSlot("FK Astana", 1355.9132f), new ClubSlot("Zimbru", 1159.1692f),
                ueclQ2.getCompLevel(), 1, 1);
        ClubSlot ueclQ2Tie46 = new ClubSlot(new ClubSlot("Decic", 1109.0890f), new ClubSlot("Rapid Wien", 1440.8290f),
                ueclQ2.getCompLevel(), 0, 2);
        ClubSlot ueclQ2Tie47 = new ClubSlot(new ClubSlot("Torpedo Zhodino", 1116.9840f),
                new ClubSlot("Maccabi Haifa", 1449.2256f), ueclQ2.getCompLevel(), 1, 1);
        ClubSlot ueclQ2Tie48 = new ClubSlot(new ClubSlot("Nakchivan", 1293.8859f), new ClubSlot("Aris", 1442.6531f),
                ueclQ2.getCompLevel(), 2, 1);
        ClubSlot ueclQ2Tie49 = new ClubSlot(new ClubSlot("Omonia", 1419.7746f),
                new ClubSlot("Torpedo Kutaisi", 1182.1104f), ueclQ2.getCompLevel(), 1, 0);
        ClubSlot ueclQ2Tie50 = new ClubSlot(new ClubSlot("Cherno More", 1343.4614f),
                new ClubSlot("Bueyueksehir", 1449.8717f), ueclQ2.getCompLevel(), 0, 1);
        ClubSlot ueclQ2Tie51 = new ClubSlot(new ClubSlot("Sutjeska", 1095.4740f), new ClubSlot("Beitar", 1337.2566f),
                ueclQ2.getCompLevel(), 1, 2);

        ueclQ2.addTies(new ArrayList<>(Arrays.asList(
                ueclQ2Tie1, ueclQ2Tie2, ueclQ2Tie3, ueclQ2Tie4, ueclQ2Tie5, ueclQ2Tie6, ueclQ2Tie7, ueclQ2Tie8,
                ueclQ2Tie9, ueclQ2Tie10, ueclQ2Tie11, ueclQ2Tie12, ueclQ2Tie13, ueclQ2Tie14, ueclQ2Tie15, ueclQ2Tie16,
                ueclQ2Tie17, ueclQ2Tie18, ueclQ2Tie19, ueclQ2Tie20, ueclQ2Tie21, ueclQ2Tie22, ueclQ2Tie23, ueclQ2Tie24,
                ueclQ2Tie25, ueclQ2Tie26, ueclQ2Tie27, ueclQ2Tie28, ueclQ2Tie29, ueclQ2Tie30, ueclQ2Tie31, ueclQ2Tie32,
                ueclQ2Tie33, ueclQ2Tie34, ueclQ2Tie35, ueclQ2Tie36, ueclQ2Tie37, ueclQ2Tie38, ueclQ2Tie39, ueclQ2Tie40,
                ueclQ2Tie41, ueclQ2Tie42, ueclQ2Tie43, ueclQ2Tie44, ueclQ2Tie45, ueclQ2Tie46, ueclQ2Tie47, ueclQ2Tie48,
                ueclQ2Tie49, ueclQ2Tie50, ueclQ2Tie51)));

        // Add ties to UEL Q3
        uelQ3.addTies(new ArrayList<>(Arrays.asList(
                new ClubSlot(uelQ2Tie5, uclQ2Tie1, uelQ3.getCompLevel()),
                new ClubSlot(uclQ2Tie2, uelQ2Tie2, uelQ3.getCompLevel()),
                new ClubSlot(uclQ2Tie3, uelQ2Tie6, uelQ3.getCompLevel()),
                new ClubSlot(uelQ2Tie7, uelQ2Tie1, uelQ3.getCompLevel()),
                new ClubSlot(new ClubSlot("Fredrikstad", 1200.0f), uelQ2Tie3, uelQ3.getCompLevel()),
                new ClubSlot(uelQ2Tie8, uelQ2Tie4, uelQ3.getCompLevel()),
                new ClubSlot(new ClubSlot("PAOK", 1300.0f), new ClubSlot("Wolfsberg", 1250.0f),
                        uelQ3.getCompLevel()))));

        // Add ties to UECL Q3
        ueclQ3.addTies(new ArrayList<>(Arrays.asList(
                new ClubSlot(ueclQ2Tie46, ueclQ2Tie8, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie44, ueclQ2Tie23, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie41, ueclQ2Tie11, ueclQ3.getCompLevel()),
                new ClubSlot(uelQ2Tie8, ueclQ2Tie7, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie29, ueclQ2Tie10, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie20, ueclQ2Tie51, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie27, ueclQ2Tie33, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie42, ueclQ2Tie14, ueclQ3.getCompLevel()),
                new ClubSlot(uelQ2Tie5, uelQ2Tie2, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie32, ueclQ2Tie45, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie9, ueclQ2Tie24, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie39, ueclQ2Tie18, ueclQ3.getCompLevel()),
                new ClubSlot(ueclQ2Tie17, ueclQ2Tie50, ueclQ3.getCompLevel()),
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

        // Initialize external service to fetch club elo ratings
        ClubEloDataLoader.init();

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

        // Print seeded and unseeded clubs in the UECL playoff
        System.out.println("UECL Playoff Seeded Clubs:");
        for (ClubSlot seededClubSlot : ueclPO.getSeeded()) {
            System.out.println(seededClubSlot.toCompactString());
        }
        System.out.println("\nUECL Playoff Unseeded Clubs:");
        for (ClubSlot unseededClubSlot : ueclPO.getUnseeded()) {
            System.out.println(unseededClubSlot.toCompactString());
        }
    }
}