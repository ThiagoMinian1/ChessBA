package ar.uade.tpo.chess.core.model;

import java.util.Optional;

/** Los seis tipos de pieza del ajedrez. */
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

    /** Letra algebraica estándar (K, Q, R, B, N, P), usada en diagramas y entradas, no en temas visuales. */
    public char letter() {
        return letter;
    }

    /** Al llegar a la última fila, un peón puede convertirse en dama, torre, alfil o caballo, pero no en rey ni peón. */
    public boolean isPromotionTarget() {
        return this == QUEEN || this == ROOK || this == BISHOP || this == KNIGHT;
    }

    /** Busca por letra algebraica, sin distinguir mayúsculas y minúsculas. */
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
