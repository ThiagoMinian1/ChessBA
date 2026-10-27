package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.model.Team;

/** Both teams' points, stored as one immutable value in the game state's extensions. */
record Scores(int white, int black) {

    static Scores fresh() {
        return new Scores(0, 0);
    }

    int of(Team team) {
        return team == Team.WHITE ? white : black;
    }

    Scores plus(Team team, int points) {
        return team == Team.WHITE ? new Scores(white + points, black) : new Scores(white, black + points);
    }
}
