Rounds
    roundsOfType.forEach(r -> r.play(clubEloDataLoader));
    Apply all temp ELO changes to the clubs.
    if (doubleLegged) {
        roundsOfType.forEach(r -> r.play(clubEloDataLoader));
        Apply all temp ELO changes to the clubs.
    }



QRound   LeaguePhaseRound   KoRound
    play()


SingleLeggedTie     DoubleLeggedTie
    After each match, update those two clubs' ELOs, and temp store the changed ELOs of all other clubs from their countries.