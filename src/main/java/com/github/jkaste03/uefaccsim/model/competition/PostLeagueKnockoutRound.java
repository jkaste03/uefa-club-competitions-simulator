package com.github.jkaste03.uefaccsim.model.competition;

import com.github.jkaste03.uefaccsim.enums.RoundType;
import com.github.jkaste03.uefaccsim.enums.Tournament;
import com.github.jkaste03.uefaccsim.model.rule.PoliticalTieRestrictions;

public class PostLeagueKnockoutRound extends KnockoutRound {

    public PostLeagueKnockoutRound(Tournament tournament, RoundType roundType) {
        super(tournament, roundType);
    }

    public PostLeagueKnockoutRound(Tournament tournament, RoundType roundType, boolean isSingleLegged) {
        super(tournament, roundType, isSingleLegged);
    }

    @Override
    protected void seed() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'seed'");
    }

    @Override
    protected void draw() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'draw'");
    }

    /**
     * Registers all clubs into their appropriate slots for the upcoming rounds in
     * the competition structure.
     * <p>
     * Every tie is added to the next primary round via
     * {@code nextPrimaryRnd.addClubSlot(...)}.
     * </p>
     */
    public void regForNextRounds() {
        // Add ties to the next primary round and the next secondary round if applicable
        ties.forEach(tie -> {
            // Add tie to the next primary round
            this.nextPrimaryRnd.addClubSlot(new ClubSlot(tie));
        });
    }

    /**
     * Determines whether pairing the two specified club slots is prohibited in this
     * round.
     * <p>
     * A tie is illegal if:
     * <ul>
     * <li>A political restriction applies (PoliticalTieRestrictions.isProhibited
     * returns true).</li>
     * </ul>
     *
     * @param clubSlotA the first club slot
     * @param clubSlotB the second club slot
     * @return true if the pairing is not allowed; false otherwise
     */
    @Override
    public boolean isIllegalTie(ClubSlot clubSlotA, ClubSlot clubSlotB) {
        return PoliticalTieRestrictions.isProhibited(clubSlotA, clubSlotB);
    }
}
