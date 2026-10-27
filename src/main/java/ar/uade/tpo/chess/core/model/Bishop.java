package ar.uade.tpo.chess.core.model;

public final class Bishop extends Piece {

    public Bishop(Team team) {
        this(team, false);
    }

    public Bishop(Team team, boolean moved) {
        super(team, moved);
    }

    @Override
    public PieceType type() {
        return PieceType.BISHOP;
    }

    @Override
    public Piece withMoved(boolean moved) {
        return new Bishop(team(), moved);
    }

    @Override
    public int materialValue() {
        return 3;
    }
}
