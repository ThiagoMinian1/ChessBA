package ar.uade.tpo.chess.core.model;

import java.util.Optional;

/**
 * Coordinates of a cell on the 8x8 board. {@code file} 0..7 maps to a..h and {@code rank} 0..7 maps to 1..8,
 * so a1 is (0,0) and h8 is (7,7). A {@code Position} is always inside the board.
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

    /** Parses algebraic notation such as {@code "e4"} (case-insensitive file letter). */
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

    /** The position {@code df} files and {@code dr} ranks away, or empty if that falls off the board. */
    public Optional<Position> offset(int df, int dr) {
        return tryOf(file + df, rank + dr);
    }

    /** True for light squares (h1 and a8 are light; a1 is dark). */
    public boolean isLightSquare() {
        return (file + rank) % 2 == 1;
    }

    @Override
    public String toString() {
        return "" + (char) ('a' + file) + (char) ('1' + rank);
    }
}
