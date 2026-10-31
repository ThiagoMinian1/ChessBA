package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Board;
import ar.uade.tpo.chess.core.model.Position;

import java.util.Optional;

/**
 * Datos que puede consultar una {@link MoveRule}: el tablero y el único dato adicional que necesita para
 * generar movimientos, la casilla de captura al paso. Al mantenerlo acotado, las reglas se prueban sin crear
 * una partida completa.
 */
public interface MoveContext {

    Board board();

    /** Casilla que saltó el peón en la jugada anterior, si avanzó dos casillas. */
    Optional<Position> enPassantTarget();

    static MoveContext of(Board board, Position enPassantTarget) {
        return new BasicMoveContext(board, enPassantTarget);
    }

    static MoveContext of(Board board) {
        return new BasicMoveContext(board, null);
    }
}
