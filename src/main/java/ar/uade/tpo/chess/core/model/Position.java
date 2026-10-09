package ar.uade.tpo.chess.core.model;

/**
 * An immutable coordinate on the board. (0, 0) is the bottom-left square
 * from White's perspective; rows and columns both run 0..7.
 */
public record Position(int row, int column) {

    public Position {
        if (row < 0 || row > 7 || column < 0 || column > 7) {
            throw new IllegalArgumentException(
                    "Position out of range: row=%d, column=%d".formatted(row, column));
        }
    }

    public static boolean isInRange(int row, int column) {
        return row >= 0 && row <= 7 && column >= 0 && column <= 7;
    }

    public Position translated(int rowDelta, int columnDelta) {
        return new Position(row + rowDelta, column + columnDelta);
    }
}
