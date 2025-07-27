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
        Tie uclQ2Tie1 = new Tie(new Club("Brann", 189f), new Club("Salzburg", 44f), uclQ2.getCompLevel(), 1, 4);
        Tie uclQ2Tie2 = new Tie(new Club("Viktoria Plzen", 56), new Club("Servette", 140), uclQ2.getCompLevel(), 0, 1);
        Tie uclQ2Tie3 = new Tie(new Club("Rangers", 25f), new Club("Panathinaikos", 111f), uclQ2.getCompLevel(), 2, 0);

        uclQ2.addTies(new ArrayList<>(Arrays.asList(
            uclQ2Tie1,
            uclQ2Tie2,
            uclQ2Tie3)));

        // UEL Q2 ties
        Tie uelQ2Tie1 = new Tie(new Club("Banik Ostrava", 168f), new Club("Legia", 70), uelQ2.getCompLevel(), 2, 2);
        Tie uelQ2Tie2 = new Tie(new Club("Sheriff Tiraspol", 88), new Club("Utrecht", 125.1f), uelQ2.getCompLevel(), 1, 3);
        Tie uelQ2Tie3 = new Tie(new Club("Midtjylland", 67), new Club("Hibernian", 207), uelQ2.getCompLevel(), 1, 1);
        Tie uelQ2Tie4 = new Tie(new Club("Levski", 312), new Club("Braga", 48), uelQ2.getCompLevel(), 0, 0);
        Tie uelQ2Tie5 = new Tie(new Club("Anderlecht", 72), new Club("Haecken", 211), uelQ2.getCompLevel(), 1, 0);
        Tie uelQ2Tie6 = new Tie(new Club("Besiktas", 113), new Club("Shakhtar", 41), uelQ2.getCompLevel(), 2, 4);
        Tie uelQ2Tie7 = new Tie(new Club("Celje", 120), new Club("Larnaca", 199), uelQ2.getCompLevel(), 1, 1);
        Tie uelQ2Tie8 = new Tie(new Club("Lugano", 90), new Club("CFR Cluj", 92), uelQ2.getCompLevel(), 0, 0);

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
        Tie ueclQ2Tie1 = new Tie(new Club("FC Infonet", 1305.1393f), new Club("Saburtalo", 1044.7577f), ueclQ2.getCompLevel(), 1, 0);
        Tie ueclQ2Tie2 = new Tie(new Club("Olimpija Ljubljana", 1305.1393f), new Club("Escaldes", 1044.7577f), ueclQ2.getCompLevel(), 4, 2);
        Tie ueclQ2Tie3 = new Tie(new Club("The New Saints", 1305.1393f), new Club("Differdang", 1044.7577f), ueclQ2.getCompLevel(), 0, 1);
        Tie ueclQ2Tie4 = new Tie(new Club("Podgorica", 1305.1393f), new Club("Milsami Orhei", 1044.7577f), ueclQ2.getCompLevel(), 0, 0);
        Tie ueclQ2Tie5 = new Tie(new Club("Zalgiris Vilnius", 1305.1393f), new Club("Linfield", 1044.7577f), ueclQ2.getCompLevel(), 0, 0);
        Tie ueclQ2Tie6 = new Tie(new Club("Cherno More", 1305.1393f), new Club("Bueyueksehir", 1044.7577f), ueclQ2.getCompLevel(), 0, 1);
        Tie ueclQ2Tie7 = new Tie(new Club("Dinamo Minsk", 1305.1393f), new Club("Egnatia", 1044.7577f), ueclQ2.getCompLevel(), 0, 2);
        Tie ueclQ2Tie8 = new Tie(new Club("Dundee United", 1305.1393f), new Club("Una Strassen", 1044.7577f), ueclQ2.getCompLevel(), 1, 0);
        Tie ueclQ2Tie9 = new Tie(new Club("Larne", 1051.3083f), new Club("Prishtine", 1076.7115f), ueclQ2.getCompLevel(), 0, 0);
        Tie ueclQ2Tie10 = new Tie(new Club("Kosice", 1370.5618f), new Club("Neman Grodno", 1166.1449f), ueclQ2.getCompLevel(), 2, 3);
        Tie ueclQ2Tie11 = new Tie(new Club("Vaduz", 1284.2422f), new Club("Dungannon", 1048.1344f), ueclQ2.getCompLevel(), 0, 1);
        Tie ueclQ2Tie12 = new Tie(new Club("Silkeborg", 1448.4069f), new Club("Akureyri", 1214.6267f), ueclQ2.getCompLevel(), 1, 1);
        Tie ueclQ2Tie13 = new Tie(new Club("Rosenborg", 1449.0964f), new Club("Banga", 1125.9369f), ueclQ2.getCompLevel(), 5, 0);
        Tie ueclQ2Tie14 = new Tie(new Club("Atletic Club Escaldes", 951.8603f), new Club("Dinamo Tirana", 1791.4805f), ueclQ2.getCompLevel(), 1, 2);
        Tie ueclQ2Tie15 = new Tie(new Club("Austria Wien", 1480.8750f), new Club("FC Spaeri", 1171.3005f), ueclQ2.getCompLevel(), 2, 0);
        Tie ueclQ2Tie16 = new Tie(new Club("Ballkani", 1189.1057f), new Club("Floriana", 1064.6722f), ueclQ2.getCompLevel(), 4, 2);
        Tie ueclQ2Tie17 = new Tie(new Club("Viking", 1509.7223f), new Club("Koper", 1345.7413f), ueclQ2.getCompLevel(), 7, 0);
        Tie ueclQ2Tie18 = new Tie(new Club("AEK", 1513.6429f), new Club("Beer-Sheva", 1506.7981f), ueclQ2.getCompLevel(), 1, 0);
        Tie ueclQ2Tie19 = new Tie(new Club("Pyunik", 1277.1631f), new Club("Gyoer", 1322.6057f), ueclQ2.getCompLevel(), 2, 1);
        Tie ueclQ2Tie20 = new Tie(new Club("FK Riga", 1276.0104f), new Club("Dila Gori", 1173.0673f), ueclQ2.getCompLevel(), 2, 1);
        Tie ueclQ2Tie21 = new Tie(new Club("Rakow", 1545.3329f), new Club("Zilina", 1370.5618f), ueclQ2.getCompLevel(), 3, 0);
        Tie ueclQ2Tie22 = new Tie(new Club("Petrocub", 1207.4063f), new Club("Sabah", 1289.7834f), ueclQ2.getCompLevel(), 0, 2);
        Tie ueclQ2Tie23 = new Tie(new Club("Ararat", 1213.0376f), new Club("Universitatea Cluj", 1388.1025f), ueclQ2.getCompLevel(), 0, 0);
        Tie ueclQ2Tie24 = new Tie(new Club("Varazdin", 1346.6229f), new Club("Santa Clara", 1484.9388f), ueclQ2.getCompLevel(), 2, 1);
        Tie ueclQ2Tie25 = new Tie(new Club("Kauno Zalgiris", 1136.1549f), new Club("Valur", 1234.8075f), ueclQ2.getCompLevel(), 1, 1);
        Tie ueclQ2Tie26 = new Tie(new Club("Paksi", 1364.2646f), new Club("Maribor", 1367.2371f), ueclQ2.getCompLevel(), 1, 0);
        Tie ueclQ2Tie27 = new Tie(new Club("Vllaznia", 1123.6422f), new Club("Vikingur", 1292.0940f), ueclQ2.getCompLevel(), 2, 1);
        Tie ueclQ2Tie28 = new Tie(new Club("Hammarby", 1526.5978f), new Club("Charleroi", 1526.2194f), ueclQ2.getCompLevel(), 0, 0);
        Tie ueclQ2Tie29 = new Tie(new Club("Kragujevac", 1524.1234f), new Club("Klaksvik", 1176.4392f), ueclQ2.getCompLevel(), 0, 0);
        Tie ueclQ2Tie30 = new Tie(new Club("Novi Pazar", 1202.0541f), new Club("Jagiellonia", 1461.7075f), ueclQ2.getCompLevel(), 1, 2);
        Tie ueclQ2Tie31 = new Tie(new Club("Polissya Zhytomyr", 1321.8274f), new Club("Santa Coloma", 959.7197f), ueclQ2.getCompLevel(), 1, 2);
        Tie ueclQ2Tie32 = new Tie(new Club("Vardar", 1082.2795f), new Club("Lausanne", 1407.6941f), ueclQ2.getCompLevel(), 2, 1);
        Tie ueclQ2Tie33 = new Tie(new Club("HB Torshavn", 1047.9337f), new Club("Brondby", 1554.7571f), ueclQ2.getCompLevel(), 1, 1);
        Tie ueclQ2Tie34 = new Tie(new Club("Olexandriya", 1383.6274f), new Club("Partizan", 1363.2365f), ueclQ2.getCompLevel(), 0, 2);
        Tie ueclQ2Tie35 = new Tie(new Club("Hibernians Paola", 1056.8972f), new Club("Trnava", 1367.7874f), ueclQ2.getCompLevel(), 1, 2);
        Tie ueclQ2Tie36 = new Tie(new Club("St Patricks", 1280.5881f), new Club("Nomme Kalju", 1074.1008f), ueclQ2.getCompLevel(), 1, 0);
        Tie ueclQ2Tie37 = new Tie(new Club("Paide Linnameeskond", 1088.4426f), new Club("AIK", 1469.2344f), ueclQ2.getCompLevel(), 0, 2);
        Tie ueclQ2Tie38 = new Tie(new Club("FK Sarajevo", 1216.2632f), new Club("Craiova", 1430.0388f), ueclQ2.getCompLevel(), 2, 1);
        Tie ueclQ2Tie39 = new Tie(new Club("Aris Limassol", 1428.7919f), new Club("Puskas Akademia", 1418.7068f), ueclQ2.getCompLevel(), 3, 2);
        Tie ueclQ2Tie40 = new Tie(new Club("St Josephs", 945.9510f), new Club("Shamrock", 1302.4304f), ueclQ2.getCompLevel(), 0, 4);
        Tie ueclQ2Tie41 = new Tie(new Club("Ilves Tampere", 1203.2096f), new Club("Alkmaar", 1626.2758f), ueclQ2.getCompLevel(), 4, 3);
        Tie ueclQ2Tie42 = new Tie(new Club("Zira", 1305.4379f), new Club("Hajduk", 1436.1804f), ueclQ2.getCompLevel(), 1, 1);
        Tie ueclQ2Tie43 = new Tie(new Club("Arda", 1291.4219f), new Club("HJK Helsinki", 1240.8477f), ueclQ2.getCompLevel(), 0, 0);
        Tie ueclQ2Tie44 = new Tie(new Club("Aktobe", 1235.8463f), new Club("Sparta Praha", 1568.5265f), ueclQ2.getCompLevel(), 2, 1);
        Tie ueclQ2Tie45 = new Tie(new Club("FK Astana", 1355.9132f), new Club("Zimbru", 1159.1692f), ueclQ2.getCompLevel(), 1, 1);
        Tie ueclQ2Tie46 = new Tie(new Club("Decic", 1109.0890f), new Club("Rapid Wien", 1440.8290f), ueclQ2.getCompLevel(), 0, 2);
        Tie ueclQ2Tie47 = new Tie(new Club("Torpedo Zhodino", 1116.9840f), new Club("Maccabi Haifa", 1449.2256f), ueclQ2.getCompLevel(), 1, 1);
        Tie ueclQ2Tie48 = new Tie(new Club("Nakchivan", 1293.8859f), new Club("Aris", 1442.6531f), ueclQ2.getCompLevel(), 2, 1);
        Tie ueclQ2Tie49 = new Tie(new Club("Omonia", 1419.7746f), new Club("Torpedo Kutaisi", 1182.1104f), ueclQ2.getCompLevel(), 1, 0);
        Tie ueclQ2Tie50 = new Tie(new Club("Cherno More", 1343.4614f), new Club("Bueyueksehir", 1449.8717f), ueclQ2.getCompLevel(), 0, 1);
        Tie ueclQ2Tie51 = new Tie(new Club("Sutjeska", 1095.4740f), new Club("Beitar", 1337.2566f), ueclQ2.getCompLevel(), 1, 2);

        ueclQ2.addTies(new ArrayList<>(Arrays.asList(
            ueclQ2Tie1, ueclQ2Tie2, ueclQ2Tie3, ueclQ2Tie4, ueclQ2Tie5, ueclQ2Tie6, ueclQ2Tie7, ueclQ2Tie8,
            ueclQ2Tie9, ueclQ2Tie10, ueclQ2Tie11, ueclQ2Tie12, ueclQ2Tie13, ueclQ2Tie14, ueclQ2Tie15, ueclQ2Tie16,
            ueclQ2Tie17, ueclQ2Tie18, ueclQ2Tie19, ueclQ2Tie20, ueclQ2Tie21, ueclQ2Tie22, ueclQ2Tie23, ueclQ2Tie24,
            ueclQ2Tie25, ueclQ2Tie26, ueclQ2Tie27, ueclQ2Tie28, ueclQ2Tie29, ueclQ2Tie30, ueclQ2Tie31, ueclQ2Tie32,
            ueclQ2Tie33, ueclQ2Tie34, ueclQ2Tie35, ueclQ2Tie36, ueclQ2Tie37, ueclQ2Tie38, ueclQ2Tie39, ueclQ2Tie40,
            ueclQ2Tie41, ueclQ2Tie42, ueclQ2Tie43, ueclQ2Tie44, ueclQ2Tie45, ueclQ2Tie46, ueclQ2Tie47, ueclQ2Tie48,
            ueclQ2Tie49, ueclQ2Tie50, ueclQ2Tie51)));

        uelQ3.addTies(new ArrayList<>(Arrays.asList(
                )));

        // Initialize external service to fetch club elo ratings
        ClubEloDataLoader.init();

        uclQ2.play();
        uelQ2.play();
        ueclQ2.play();

        for (Tie tie : ueclQ2.getTies()) {
            System.out.println(tie.getClubSlot1());
            System.out.println(tie.getClubSlot2());
        }
    }
}