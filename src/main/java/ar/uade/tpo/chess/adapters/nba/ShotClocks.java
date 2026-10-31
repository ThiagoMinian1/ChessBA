package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.model.Team;

/** Relojes de posesión de ambos bandos, guardados como un valor inmutable en las extensiones del estado. */
record ShotClocks(ShotClockState white, ShotClockState black) {

    static ShotClocks fresh() {
        return new ShotClocks(ShotClockState.fresh(), ShotClockState.fresh());
    }

    ShotClockState of(Team team) {
        return team == Team.WHITE ? white : black;
    }

    ShotClocks with(Team team, ShotClockState state) {
        return team == Team.WHITE ? new ShotClocks(state, black) : new ShotClocks(white, state);
    }
}
