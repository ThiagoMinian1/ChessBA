package ar.uade.tpo.chess.core.model;

import java.util.Objects;

/**
 * A chess piece. Pieces are immutable: "moving" a piece replaces it with {@link #withMoved(boolean)}, which is
 * what lets the game take cheap snapshots (Memento) for undo/redo.
 *
 * <p>How a piece moves is not decided here: movement is a Strategy ({@code MoveRule}) looked up by
 * {@link #type()}, which keeps the model free of rule logic and avoids a cycle with the rules package.
 */
public abstract class Piece {

    private final Team team;
    private final boolean moved;

    protected Piece(Team team, boolean moved) {
        this.team = Objects.requireNonNull(team, "team");
        this.moved = moved;
    }

    public abstract PieceType type();

    /** A copy of this piece with the given "has moved" flag (matters for castling rights). */
    public abstract Piece withMoved(boolean moved);

    /** Conventional material value (pawn 1, minor 3, rook 5, queen 9; the king has no exchange value). */
    public abstract int materialValue();

    public Team team() {
        return team;
    }

    public boolean hasMoved() {
        return moved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Piece other = (Piece) o;
        return team == other.team && moved == other.moved;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), team, moved);
    }

    /** Diagram letter: uppercase for white, lowercase for black. */
    public char symbol() {
        char letter = type().letter();
        return team == Team.WHITE ? letter : Character.toLowerCase(letter);
    }

    @Override
    public String toString() {
        return team + " " + type();
    }
}
