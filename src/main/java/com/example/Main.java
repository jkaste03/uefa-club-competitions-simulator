package com.example;

import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        Round uclQ2 = new Round(1, 2);
        Round uelQ2 = new Round(2, 2);
        Round ueclQ2 = new Round(3, 2);

        Round uclQ3 = new Round(1, 3);
        Round uelQ3 = new Round(2, 3);
        Round ueclQ3 = new Round(3, 3);

        // Example clubs
        uclQ2.addTies(new ArrayList<>(Arrays.asList(
                new Tie(new Club("Brann", 189f), new Club("Salzburg", 44f), uclQ2.getCompLevel(), 1, 4),
                new Tie(new Club("Viktoria Plzen", 56), new Club("Servette", 140), uclQ2.getCompLevel(), 0, 1),
                new Tie(new Club("Rangers", 25f), new Club("Panathinaikos", 111f), uclQ2.getCompLevel(), 2, 0))));

        uelQ2.addTies(new ArrayList<>(Arrays.asList(
                new Tie(new Club("Banik Ostrava", 168f), new Club("Legia", 70), uelQ2.getCompLevel(), 2, 2),
                new Tie(new Club("Sheriff Tiraspol", 88), new Club("Utrecht", 125.1f), uelQ2.getCompLevel(), 1, 3),
                new Tie(new Club("Midtjylland", 67), new Club("Hibernian", 207), uelQ2.getCompLevel(), 1, 1),
                new Tie(new Club("Levski", 312), new Club("Braga", 48), uelQ2.getCompLevel(), 0, 0),
                new Tie(new Club("Anderlecht", 72), new Club("Haecken", 211), uelQ2.getCompLevel(), 1, 0),
                new Tie(new Club("Besiktas", 113), new Club("Shakhtar", 41), uelQ2.getCompLevel(), 2, 4),
                new Tie(new Club("Celje", 120), new Club("Larnaca", 199), uelQ2.getCompLevel(), 1, 1),
                new Tie(new Club("Lugano", 90), new Club("CFR Cluj", 92), uelQ2.getCompLevel(), 0, 0))));

        // Conference League QR2 – Main Path
        ueclQ2.addTies(new ArrayList<>(Arrays.asList(
                new Tie(new Club("Dundee United", 1305.1393f), new Club("Una Strassen", 1044.7577f), uclQ2.getCompLevel(), 1, 0),
                new Tie(new Club("Larne", 1051.3083f), new Club("Prishtine", 1076.7115f), uclQ2.getCompLevel(), 0, 0),
                new Tie(new Club("Kosice", 1370.5618f), new Club("Neman Grodno", 1166.1449f), uclQ2.getCompLevel(), 2, 3),
                new Tie(new Club("Vaduz", 1284.2422f), new Club("Dungannon", 1048.1344f), uclQ2.getCompLevel(), 0, 1),
                new Tie(new Club("Silkeborg", 1448.4069f), new Club("Akureyri", 1214.6267f), uclQ2.getCompLevel(), 1, 1),
                new Tie(new Club("Rosenborg", 1449.0964f), new Club("Banga", 1125.9369f), uclQ2.getCompLevel(), 5, 0),
                new Tie(new Club("Atletic Club Escaldes", 951.8603f), new Club("Dinamo City", 1791.4805f), uclQ2.getCompLevel(), 1, 2),
                new Tie(new Club("Austria Wien", 1480.8750f), new Club("FC Spaeri", 1171.3005f), uclQ2.getCompLevel(), 2, 0),
                new Tie(new Club("Ballkani", 1189.1057f), new Club("Floriana", 1064.6722f), uclQ2.getCompLevel(), 4, 2),
                new Tie(new Club("Viking", 1509.7223f), new Club("Koper", 1345.7413f), uclQ2.getCompLevel(), 7, 0),
                new Tie(new Club("AEK", 1513.6429f), new Club("Beer-Sheva", 1506.7981f), uclQ2.getCompLevel(), 1, 0),
                new Tie(new Club("Pyunik", 1277.1631f), new Club("Gyoer", 1322.6057f), uclQ2.getCompLevel(), 2, 1),
                new Tie(new Club("FK Riga", 1276.0104f), new Club("Dila Gori", 1173.0673f), uclQ2.getCompLevel(), 2, 1),
                new Tie(new Club("Rakow", 1545.3329f), new Club("Zilina", 1370.5618f), uclQ2.getCompLevel(), 3, 0),
                new Tie(new Club("Petrocub", 1207.4063f), new Club("Sabah", 1289.7834f), uclQ2.getCompLevel(), 0, 2),
                new Tie(new Club("Ararat", 1213.0376f), new Club("Universitatea Cluj", 1388.1025f), uclQ2.getCompLevel(), 0, 0),
                new Tie(new Club("Varazdin", 1346.6229f), new Club("Santa Clara", 1484.9388f), uclQ2.getCompLevel(), 2, 1),
                new Tie(new Club("Kauno Zalgiris", 1136.1549f), new Club("Valur", 1234.8075f), uclQ2.getCompLevel(), 1, 1),
                new Tie(new Club("Paksi", 1364.2646f), new Club("Maribor", 1367.2371f), uclQ2.getCompLevel(), 1, 0),
                new Tie(new Club("Vllaznia", 1123.6422f), new Club("Vikingur", 1292.0940f), uclQ2.getCompLevel(), 2, 1),
                new Tie(new Club("Hammarby", 1526.5978f), new Club("Charleroi", 1526.2194f), uclQ2.getCompLevel(), 0, 0),
                new Tie(new Club("Nis", 1524.1234f), new Club("Klaksvik", 1176.4392f), uclQ2.getCompLevel(), 0, 0),
                new Tie(new Club("Novi Pazar", 1202.0541f), new Club("Jagiellonia", 1461.7075f), uclQ2.getCompLevel(), 1, 2),
                new Tie(new Club("Polissya Zhytomyr", 1321.8274f), new Club("Santa Coloma", 959.7197f), uclQ2.getCompLevel(), 1, 2),
                new Tie(new Club("Vardar", 1082.2795f), new Club("Lausanne", 1407.6941f), uclQ2.getCompLevel(), 2, 1),
                new Tie(new Club("HB Torshavn", 1047.9337f), new Club("Brondby", 1554.7571f), uclQ2.getCompLevel(), 1, 1),
                new Tie(new Club("Olexandriya", 1383.6274f), new Club("Partizan", 1363.2365f), uclQ2.getCompLevel(), 0, 2),
                new Tie(new Club("Hibernians Paola", 1056.8972f), new Club("Trnava", 1367.7874f), uclQ2.getCompLevel(), 1, 2),
                new Tie(new Club("St Patricks", 1280.5881f), new Club("Nomme Kalju", 1074.1008f), uclQ2.getCompLevel(), 1, 0),
                new Tie(new Club("Paide Linnameeskond", 1088.4426f), new Club("AIK", 1469.2344f), uclQ2.getCompLevel(), 0, 2),
                new Tie(new Club("FK Sarajevo", 1216.2632f), new Club("Craiova", 1430.0388f), uclQ2.getCompLevel(), 2, 1),
                new Tie(new Club("Aris Limassol", 1428.7919f), new Club("Puskas Akademia", 1418.7068f), uclQ2.getCompLevel(), 3, 2),
                new Tie(new Club("St Josephs", 945.9510f), new Club("Shamrock", 1302.4304f), uclQ2.getCompLevel(), 0, 4),
                new Tie(new Club("Ilves Tampere", 1203.2096f), new Club("Alkmaar", 1626.2758f), uclQ2.getCompLevel(), 4, 3),
                new Tie(new Club("Zira", 1305.4379f), new Club("Hajduk", 1436.1804f), uclQ2.getCompLevel(), 1, 1),
                new Tie(new Club("Arda", 1291.4219f), new Club("HJK Helsinki", 1240.8477f), uclQ2.getCompLevel(), 0, 0),
                new Tie(new Club("Aktobe", 1235.8463f), new Club("Sparta Praha", 1568.5265f), uclQ2.getCompLevel(), 2, 1),
                new Tie(new Club("FK Astana", 1355.9132f), new Club("Zimbru", 1159.1692f), uclQ2.getCompLevel(), 1, 1),
                new Tie(new Club("Decic", 1109.0890f), new Club("Rapid Wien", 1440.8290f), uclQ2.getCompLevel(), 0, 2),
                new Tie(new Club("Torpedo Zhodino", 1116.9840f), new Club("Maccabi Haifa", 1449.2256f), uclQ2.getCompLevel(), 1, 1),
                new Tie(new Club("Nakchivan", 1293.8859f), new Club("Aris", 1442.6531f), uclQ2.getCompLevel(), 2, 1),
                new Tie(new Club("Omonia", 1419.7746f), new Club("Torpedo Kutaisi", 1182.1104f), uclQ2.getCompLevel(), 1, 0),
                new Tie(new Club("Cherno More", 1343.4614f), new Club("Bueyueksehir", 1449.8717f), uclQ2.getCompLevel(), 0, 1),
                new Tie(new Club("Sutjeska", 1095.4740f), new Club("Beitar", 1337.2566f), uclQ2.getCompLevel(), 1, 2))));

        // Initialize external service to fetch club elo ratings
        ClubEloDataLoader.init();

        for (Tie tie : ueclQ2.getTies()) {
            tie.play();
        }
    }
}