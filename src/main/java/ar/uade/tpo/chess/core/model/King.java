package ar.uade.tpo.chess.core.model;

public final class King extends Piece {

    public King(Team team) {
        this(team, false);
    }

    public King(Team team, boolean moved) {
        super(team, moved);
    }

    @Override
    public PieceType type() {
        return PieceType.KING;
    }

    @Override
    public Piece withMoved(boolean moved) {
        return new King(team(), moved);
    }

    /** The king is never exchanged, so it carries no material value. */
    @Override
    public int materialValue() {
        return 0;
    }
}
