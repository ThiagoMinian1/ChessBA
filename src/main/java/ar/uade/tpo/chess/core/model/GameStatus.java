package ar.uade.tpo.chess.core.model;

/** Estado de la partida. Cualquier valor distinto de {@code ONGOING} indica que terminó. */
public enum GameStatus {
    ONGOING,
    CHECKMATE,
    STALEMATE,
    DRAW_THREEFOLD_REPETITION,
    DRAW_FIFTY_MOVE_RULE,
    DRAW_INSUFFICIENT_MATERIAL,
    DRAW_BY_AGREEMENT,
    RESIGNATION;

    public boolean isOver() {
        return this != ONGOING;
    }

    public boolean isDraw() {
        return this == STALEMATE
                || this == DRAW_THREEFOLD_REPETITION
                || this == DRAW_FIFTY_MOVE_RULE
                || this == DRAW_INSUFFICIENT_MATERIAL
                || this == DRAW_BY_AGREEMENT;
    }
}
