package ar.uade.tpo.chess.core.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of one cell of the board: where it is ({@link Position}) and what, if anything, stands on it.
 */
public record Square(Position position, Piece piece) {

    public Square {
        Objects.requireNonNull(position, "position");
    }

    public boolean isEmpty() {
        return piece == null;
    }

    public Optional<Piece> occupant() {
        return Optional.ofNullable(piece);
    }
}
