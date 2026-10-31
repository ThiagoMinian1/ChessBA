package ar.uade.tpo.chess.core.model;

import java.util.Objects;

/** Una pieza junto con la posición que ocupa. */
public record PlacedPiece(Position position, Piece piece) {

    public PlacedPiece {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(piece, "piece");
    }
}
