package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;

import java.util.Optional;

/**
 * Resultado de aplicar un movimiento al tablero.
 *
 * @param movingPiece pieza que se movió antes de hacerlo (si promocionó, sigue siendo el peón)
 * @param captured    pieza capturada, si hubo una (en la captura al paso, el peón que estaba al lado)
 * @param capturedAt  casilla donde estaba la pieza capturada (distinta del destino en la captura al paso)
 */
public record MoveOutcome(Piece movingPiece, Optional<Piece> captured, Optional<Position> capturedAt) {

    public boolean isCapture() {
        return captured.isPresent();
    }
}
