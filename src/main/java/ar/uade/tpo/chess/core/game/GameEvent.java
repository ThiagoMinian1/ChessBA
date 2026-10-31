package ar.uade.tpo.chess.core.game;

import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;

import java.util.Optional;

/**
 * Sucesos ocurridos durante una acción. Las acciones los guardan en {@link GameState} y {@link Game} se los
 * comunica a los observadores cuando la acción ya se confirmó. Así, no reciben avisos de acciones rechazadas.
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
