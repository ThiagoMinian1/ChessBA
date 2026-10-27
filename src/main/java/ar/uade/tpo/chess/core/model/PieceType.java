package ar.uade.tpo.chess.core.model;

import java.util.Optional;

/** The six kinds of chess piece. */
public enum PieceType {
    KING('K'),
    QUEEN('Q'),
    ROOK('R'),
    BISHOP('B'),
    KNIGHT('N'),
    PAWN('P');

    private final char letter;

    PieceType(char letter) {
        this.letter = letter;
    }

    /** Standard algebraic letter (K, Q, R, B, N, P). Used for diagrams and input, not for theming. */
    public char letter() {
        return letter;
    }

    /** A pawn that reaches the last rank may become a queen, rook, bishop or knight - never a king or a pawn. */
    public boolean isPromotionTarget() {
        return this == QUEEN || this == ROOK || this == BISHOP || this == KNIGHT;
    }

    /** Case-insensitive lookup by algebraic letter. */
    public static Optional<PieceType> fromLetter(char letter) {
        char upper = Character.toUpperCase(letter);
        for (PieceType type : values()) {
            if (type.letter == upper) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
