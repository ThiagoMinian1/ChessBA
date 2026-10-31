package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.model.Team;

/** Observador del reloj de posesión; recibe avisos de poco tiempo y de turnos saltados por una infracción. */
public interface ShotClockListener {

    /** A {@code team} le quedan {@code movesRemaining} jugadas propias antes de que venza el reloj. */
    default void onShotClockWarning(Team team, int movesRemaining) {
    }

    /** Venció el reloj de {@code team} y se saltó su turno. */
    default void onShotClockViolation(Team team) {
    }
}
