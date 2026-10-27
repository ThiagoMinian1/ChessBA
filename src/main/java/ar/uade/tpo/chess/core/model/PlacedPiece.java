package ar.uade.tpo.chess.core.model;

import java.util.Objects;

/** A piece together with the position it stands on. */
public record PlacedPiece(Position position, Piece piece) {

    public PlacedPiece {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(piece, "piece");
    }
}
