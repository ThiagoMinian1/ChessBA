package ar.uade.tpo.chess.core.model;

/** The kinds of move the standard rules distinguish, because each one changes the board differently. */
public enum MoveType {
    /** A regular move or capture. */
    NORMAL,
    /** A pawn advancing two squares from its starting rank (opens the en passant window). */
    PAWN_DOUBLE_PUSH,
    /** A pawn capturing a pawn that has just double-pushed past it. */
    EN_PASSANT,
    /** King moves two squares towards the h-file rook (short castling). */
    CASTLE_KINGSIDE,
    /** King moves two squares towards the a-file rook (long castling). */
    CASTLE_QUEENSIDE,
    /** A pawn reaching the last rank, carrying the chosen {@link PieceType}. */
    PROMOTION
}
