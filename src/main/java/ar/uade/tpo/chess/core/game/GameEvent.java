package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;

import java.util.Optional;

/**
 * Things that happened while an action ran. Actions queue them in the {@link GameState}; the {@link Game}
 * delivers them to its observers only once the action has been committed, so observers never hear about an
 * action that was rejected half-way.
 */
public sealed interface GameEvent {

    record MovePlayed(Team team, Move move, PieceType piece) implements GameEvent {
    }

    record PieceCaptured(Team capturer, Piece captured, Position at) implements GameEvent {
    }

    record PiecePromoted(Team team, Position at, PieceType promotedTo) implements GameEvent {
    }

    record CheckGiven(Team checkedTeam) implements GameEvent {
    }

    record GameEnded(GameStatus status, Optional<Team> winner) implements GameEvent {
    }

    record Notice(String message) implements GameEvent {
    }
}
