package ar.uade.tpo.chess.core.model;

/**
 * A single move that was actually played, kept in a game's history and
 * handed to observers. {@code movedPiece} and {@code capturedPiece} are
 * snapshots taken before the move was applied.
 */
public record Move(
        Position from,
        Position to,
        Piece movedPiece,
        Piece capturedPiece,
        MoveType type
) {
    public boolean isCapture() {
        return type == MoveType.CAPTURE;
    }
}
