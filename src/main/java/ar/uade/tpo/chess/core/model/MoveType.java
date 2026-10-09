package ar.uade.tpo.chess.core.model;

/**
 * Kept as its own enum (rather than a boolean) so that later, optional
 * move kinds (castling, en passant, promotion) can be added here without
 * changing {@link Move}'s shape.
 */
public enum MoveType {
    NORMAL,
    CAPTURE
}
