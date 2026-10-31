package ar.uade.tpo.chess.core.model;

/** Tipos de movimiento que distinguen las reglas estándar; cada uno modifica el tablero de forma distinta. */
public enum MoveType {
    /** Movimiento normal o captura. */
    NORMAL,
    /** El peón avanza dos casillas desde su posición inicial y habilita la captura al paso. */
    PAWN_DOUBLE_PUSH,
    /** El peón captura al paso a otro que acaba de avanzar dos casillas. */
    EN_PASSANT,
    /** El rey avanza dos casillas hacia la torre de la columna h (enroque corto). */
    CASTLE_KINGSIDE,
    /** El rey avanza dos casillas hacia la torre de la columna a (enroque largo). */
    CASTLE_QUEENSIDE,
    /** El peón llega a la última fila y se convierte en la pieza indicada por {@link PieceType}. */
    PROMOTION
}
