package com.github.jkaste03.uefaccsim.model.competition;

import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;

public class RoundOf16 extends PostLeagueKnockoutRound {

    public RoundOf16(Tournament tournament) {
        super(tournament, RoundType.ROUND_OF_16);
    }

    public RoundOf16(Tournament tournament, boolean isSingleLegged) {
        super(tournament, RoundType.ROUND_OF_16, isSingleLegged);
    }

    @Override
    public void draw() {
        throw new UnsupportedOperationException("Round of 16 draw is not implemented yet");
    }
}
