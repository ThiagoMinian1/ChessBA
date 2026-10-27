package ar.uade.tpo.chess.core.rules;

import ar.uade.tpo.chess.core.model.Piece;
import ar.uade.tpo.chess.core.model.Position;

import java.util.Optional;

/**
 * What applying a move did to the board.
 *
 * @param movingPiece the piece that moved, as it was before moving (a promoting pawn is reported as the pawn)
 * @param captured    the captured piece, if any (for en passant, the pawn that was beside the mover)
 * @param capturedAt  where the captured piece stood (differs from the destination for en passant)
 */
public record MoveOutcome(Piece movingPiece, Optional<Piece> captured, Optional<Position> capturedAt) {

    public boolean isCapture() {
        return captured.isPresent();
    }
}
