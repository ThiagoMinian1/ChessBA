package ar.uade.tpo.chess.core.model;

/**
 * Los dos bandos de una partida. El núcleo solo conoce {@code WHITE} y {@code BLACK};
 * cualquier otro nombre que se muestre para un bando pertenece a los adaptadores.
 */
public enum Team {
    WHITE,
    BLACK;

    public Team opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /** Fila inicial, desde cero, de las piezas mayores (fila 1 para blancas y 8 para negras). */
    public int homeRank() {
        return this == WHITE ? 0 : 7;
    }

    /** Fila inicial, desde cero, de los peones (fila 2 para blancas y 7 para negras). */
    public int pawnStartRank() {
        return this == WHITE ? 1 : 6;
    }

    /** Fila, desde cero, en la que promocionan los peones de este bando. */
    public int promotionRank() {
        return this == WHITE ? 7 : 0;
    }

    /** Sentido en que avanzan los peones: +1 para blancas y -1 para negras. */
    public int forward() {
        return this == WHITE ? 1 : -1;
    }
}
