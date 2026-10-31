package ar.uade.tpo.chess.core.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Vista de solo lectura de una casilla: su posición ({@link Position}) y la pieza que haya en ella, si existe.
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
