package ar.uade.tpo.chess.core.model;

import java.util.Optional;

/**
 * Coordenadas de una casilla del tablero de 8x8. {@code file} de 0 a 7 corresponde a las columnas a-h y
 * {@code rank} de 0 a 7 a las filas 1-8; así, a1 es (0,0) y h8 es (7,7). Toda {@code Position} está dentro del tablero.
 */
public record Position(int file, int rank) {

    public static final int SIZE = 8;

    public Position {
        if (!inBounds(file, rank)) {
            throw new IllegalArgumentException("Position out of board: file=" + file + ", rank=" + rank);
        }
    }

    public static boolean inBounds(int file, int rank) {
        return file >= 0 && file < SIZE && rank >= 0 && rank < SIZE;
    }

    /** Interpreta notación algebraica como {@code "e4"} (sin distinguir mayúsculas en la columna). */
    public static Position of(String algebraic) {
        if (algebraic == null || algebraic.length() != 2) {
            throw new IllegalArgumentException("Invalid square: " + algebraic);
        }
        char file = Character.toLowerCase(algebraic.charAt(0));
        char rank = algebraic.charAt(1);
        if (file < 'a' || file > 'h' || rank < '1' || rank > '8') {
            throw new IllegalArgumentException("Invalid square: " + algebraic);
        }
        return new Position(file - 'a', rank - '1');
    }

    public static Optional<Position> tryOf(int file, int rank) {
        return inBounds(file, rank) ? Optional.of(new Position(file, rank)) : Optional.empty();
    }

    /** Posición a {@code df} columnas y {@code dr} filas de distancia, o vacío si queda fuera del tablero. */
    public Optional<Position> offset(int df, int dr) {
        return tryOf(file + df, rank + dr);
    }

    /** Indica si es una casilla clara (h1 y a8 son claras; a1 es oscura). */
    public boolean isLightSquare() {
        return (file + rank) % 2 == 1;
    }

    @Override
    public String toString() {
        return "" + (char) ('a' + file) + (char) ('1' + rank);
    }
}
