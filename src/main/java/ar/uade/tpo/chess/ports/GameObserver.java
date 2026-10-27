package ar.uade.tpo.chess.ports;

import ar.uade.tpo.chess.core.model.GameStatus;
import ar.uade.tpo.chess.core.model.Move;
import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.PieceType;
import ar.uade.tpo.chess.core.model.Position;
import ar.uade.tpo.chess.core.model.Team;

import java.util.Optional;

/**
 * Outbound port (Observer): lets anything outside the core hear about what happens in a game without polling.
 * Every method has an empty default, so an observer only overrides what it cares about.
 * Events are delivered after an action has been fully applied and committed.
 */
public interface GameObserver {

    /** A piece moved. {@code piece} is the kind of piece that moved (a pawn, even when it promoted). */
    default void onMovePlayed(Team team, Move move, PieceType piece) {
    }

    /** {@code capturer} took {@code captured} on {@code at} (for en passant, where the pawn stood). */
    default void onPieceCaptured(Team capturer, Piece captured, Position at) {
    }

    default void onPiecePromoted(Team team, Position at, PieceType promotedTo) {
    }

    /** {@code checkedTeam}'s king is in check and the game continues (checkmate is reported by onGameEnded). */
    default void onCheck(Team checkedTeam) {
    }

    default void onGameEnded(GameStatus status, Optional<Team> winner) {
    }

    /** Free-text message from an action that is not standard chess (for example a themed mechanic). */
    default void onNotice(String message) {
    }
}
