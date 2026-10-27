package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.model.Team;

/** Which teams have already used their one trade, stored as an immutable value in the game state's extensions. */
record TradeLedger(boolean whiteUsed, boolean blackUsed) {

    static TradeLedger fresh() {
        return new TradeLedger(false, false);
    }

    boolean hasUsed(Team team) {
        return team == Team.WHITE ? whiteUsed : blackUsed;
    }

    TradeLedger markUsed(Team team) {
        return team == Team.WHITE ? new TradeLedger(true, blackUsed) : new TradeLedger(whiteUsed, true);
    }
}
