package ar.uade.tpo.chess.core.model;

/**
 * The two sides of a chess game. The core only knows about {@code WHITE} and {@code BLACK};
 * any other identity a side may be given when presented lives in the adapters.
 */
public enum Team {
    WHITE,
    BLACK;

    public Team opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /** Zero-based rank where this team's back-rank pieces start (rank 1 for white, rank 8 for black). */
    public int homeRank() {
        return this == WHITE ? 0 : 7;
    }

    /** Zero-based rank where this team's pawns start (rank 2 for white, rank 7 for black). */
    public int pawnStartRank() {
        return this == WHITE ? 1 : 6;
    }

    /** Zero-based rank where this team's pawns promote. */
    public int promotionRank() {
        return this == WHITE ? 7 : 0;
    }

    /** Rank direction in which this team's pawns advance: +1 for white, -1 for black. */
    public int forward() {
        return this == WHITE ? 1 : -1;
    }
}
