package ar.uade.tpo.chess.core.model;

/**
 * The two sides of a chess game. Kept as plain chess vocabulary on purpose:
 * any theme (NBA franchises, anything else) is a presentation concern that
 * belongs in an adapter, never in the core.
 */
public enum Team {
    WHITE,
    BLACK;

    public Team opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}
