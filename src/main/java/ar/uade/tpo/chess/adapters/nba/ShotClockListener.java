package ar.uade.tpo.chess.adapters.nba;

import ar.uade.tpo.chess.core.model.Team;

/** Observer of the shot clock: told when a team is running low and when a violation skips its turn. */
public interface ShotClockListener {

    /** {@code team} has {@code movesRemaining} own moves left before its clock expires. */
    default void onShotClockWarning(Team team, int movesRemaining) {
    }

    /** {@code team}'s clock expired and its turn was skipped. */
    default void onShotClockViolation(Team team) {
    }
}
