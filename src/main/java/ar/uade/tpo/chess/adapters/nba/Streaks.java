package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.model.Team;

/** Rachas de capturas actuales de ambos bandos, guardadas como un valor inmutable en las extensiones del estado. */
record Streaks(int white, int black) {

    static Streaks fresh() {
        return new Streaks(0, 0);
    }

    int of(Team team) {
        return team == Team.WHITE ? white : black;
    }

    Streaks with(Team team, int streak) {
        return team == Team.WHITE ? new Streaks(streak, black) : new Streaks(white, streak);
    }
}
